import Objects.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    static void printBoard(Board board) {
        String[][] s = board.snapshotWithColors();
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<s.length;i++) {
            for (int j=0;j<s[0].length;j++) {
                sb.append(s[i][j]).append(' ');
            }
            sb.append('\n');
        }
        System.out.print(sb.toString());
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        System.out.print("Tamaño del tablero N (sugerido 10): ");
        int N = readInt(sc, 5, 50, 10);

        System.out.print("Duración de la partida T en segundos (sugerido 60): ");
        int T = readInt(sc, 10, 600, 60);

        System.out.print("Cantidad de jugadores M (1 a 4): ");
        int M = readInt(sc, 1, 4, 3);

        //System.out.print("Zmin jugador (ms): ");
        // long Zmin = readLong(sc, 10, 5000, 250);
        long Zmin = 350; // Más tiempo para planificación equitativa

        //System.out.print("Zmax jugador (ms): ");
        // long Zmax = readLong(sc, Zmin, 8000, Math.max(500, Zmin+500));
        long Zmax = 550; // Rango más amplio para variabilidad

        // System.out.print("Ymin robot monedas (ms): ");
        // long Ymin = readLong(sc, 10, 5000, 150);
        long Ymin = 200; // Más lento para dar oportunidades

        // System.out.print("Ymax robot monedas (ms): ");
        // long Ymax = readLong(sc, Ymin, 8000, Math.max(400, Ymin+400));
        long Ymax = 400; // Más variabilidad

        // System.out.print("Wmin robot malo (ms): ");
        // long Wmin = readLong(sc, 10, 5000, 300);
        long Wmin = 800;

        // System.out.print("Wmax robot malo (ms): ");
        // long Wmax = readLong(sc, Wmin, 8000, Math.max(600, Wmin+300));
        long Wmax = 1500;

        // System.out.print("Cantidad de vidas H a colocar (fijo, +1 por casilla): ");
        // int H = readInt(sc, 0, N*N, Math.max(1, (int)(0.05*N*N)));
        int H = 8;

        List<String> names = new ArrayList<>();
        for (int i=1;i<=M;i++) {
            System.out.print("Nombre del jugador " + i + ": ");
            String nm = sc.next().trim();
            if (nm.isEmpty()) nm = "Jugador" + i;
            names.add(nm);
        }
        sc.close();

        Board board = new Board(N, N);
        board.setTargetHeals(H);

        // Robots: colocan ítems UNA VEZ hasta llegar al objetivo
        Thread coinBot = new Thread(new Robot(board, Booster.COIN, Ymin, Ymax), "RobotMonedas");
        Thread healBot = new Thread(new Robot(board, Booster.HEAL, 200, 400), "RobotVidas");
        Thread badBot  = new Thread(new Robot(board, Booster.POISON, Wmin, Wmax), "RobotMalo");

        // Jugadores
        List<Player> players = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        int id=1;
        for (String nm : names) {
            Player p = new Player(nm, id++, board, 3 /* vidas iniciales */, Zmin, Zmax);
            players.add(p);
        }
        for (Player p : players) board.placePlayerAtRandom(p);

        // Display thread
        final boolean[] running = { true };
        Thread display = new Thread(() -> {
            try {
                while (running[0]) {
                    System.out.println("\nTABLERO:");
                    printBoard(board);
                    Thread.sleep(300);
                }
            } catch (InterruptedException ignored) {}
        }, "Display");

        // Start robots first to seed items
        coinBot.start();
        healBot.start();
        badBot.start();

        // Start players
        for (Player p : players) {
            Thread t = new Thread(p, "Player-"+p.name());
            threads.add(t);
            t.start();
        }
        display.start();

        System.out.println("\n>>> Comienza la partida con " + M + " jugadores! (duración " + T + "s)");

        long deadline = System.currentTimeMillis() + T * 1000L;
        while (System.currentTimeMillis() < deadline) {
            int alive = 0;
            for (Player p : players) if (p.lifes() > 0) alive++;
            if (alive <= 1) break;
            Thread.sleep(200);
        }

        for (Player p : players) p.stopGracefully();
        coinBot.interrupt(); healBot.interrupt(); badBot.interrupt();
        running[0] = false; display.interrupt();

        for (Thread t : threads) t.join(2000);

        players.sort((a,b) -> {
            int aliveA = a.lifes() > 0 ? 1 : 0;
            int aliveB = b.lifes() > 0 ? 1 : 0;
            if (aliveA != aliveB) return Integer.compare(aliveB, aliveA);
            if (a.coins() != b.coins()) return Integer.compare(b.coins(), a.coins());
            if (a.lifes() != b.lifes()) return Integer.compare(b.lifes(), a.lifes());
            return a.name().compareToIgnoreCase(b.name());
        });

        StringBuilder report = new StringBuilder();
        report.append("==== RESULTADOS DE LA PARTIDA ====\n");
        for (int i=0;i<players.size();i++) {
            Player p = players.get(i);
            report.append(String.format("%d) %s  | monedas=%d | vidas=%d%n", i+1, p.name(), p.coins(), p.lifes()));
        }

        System.out.println("\n" + report);

        String filename = "partida_" + java.time.LocalDateTime.now().toString().replace(':','-') + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(filename, false))) {
            out.print(report.toString());
        }
        System.out.println("Log guardado en: " + filename);
    }

    static int readInt(Scanner sc, int min, int max, int dflt) {
        try {
            int v = Integer.parseInt(sc.nextLine().trim());
            if (v < min || v > max) return dflt;
            return v;
        } catch (Exception e) {
            return dflt;
        }
    }

    static long readLong(Scanner sc, long min, long max, long dflt) {
        try {
            long v = Long.parseLong(sc.nextLine().trim());
            if (v < min || v > max) return dflt;
            return v;
        } catch (Exception e) {
            return dflt;
        }
    }
}
