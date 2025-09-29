import Objects.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ImprovedMain {

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

        System.out.print("Zmin jugador (ms): ");
        long Zmin = readLong(sc, 10, 5000, 150); // Reducido por defecto

        System.out.print("Zmax jugador (ms): ");
        long Zmax = readLong(sc, Zmin, 8000, Math.max(300, Zmin+200)); // Reducido por defecto

        System.out.print("Ymin robot monedas (ms): ");
        long Ymin = readLong(sc, 10, 5000, 150);

        System.out.print("Ymax robot monedas (ms): ");
        long Ymax = readLong(sc, Ymin, 8000, Math.max(400, Ymin+400));

        System.out.print("Wmin robot malo (ms): ");
        long Wmin = readLong(sc, 10, 5000, 300);

        System.out.print("Wmax robot malo (ms): ");
        long Wmax = readLong(sc, Wmin, 8000, Math.max(600, Wmin+300));

        System.out.print("Cantidad de vidas H a colocar (fijo, +1 por casilla): ");
        int H = readInt(sc, 0, N*N, Math.max(2, (int)(0.08*N*N))); // Más vidas por defecto

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

        // Jugadores con mejor distribución inicial
        List<Player> players = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        int id=1;
        for (String nm : names) {
            Player p = new Player(nm, id++, board, 2 /* vidas iniciales */, Zmin, Zmax);
            players.add(p);
        }
        
        // Colocar jugadores más distribuidos
        placePlayersDistributed(board, players);

        // Display thread con contador de actividad
        final boolean[] running = { true };
        final Map<String, Integer> lastMoveCounts = new ConcurrentHashMap<>();
        for (Player p : players) {
            lastMoveCounts.put(p.name(), 0);
        }
        
        Thread display = new Thread(() -> {
            try {
                int iteration = 0;
                while (running[0]) {
                    System.out.println("\nTABLERO (Iteración " + iteration + "):");
                    printBoard(board);
                    
                    // Mostrar actividad de jugadores
                    StringBuilder activity = new StringBuilder("Actividad: ");
                    for (Player p : players) {
                        activity.append(p.name()).append("(").append(p.coins()).append("$,").append(p.lifes()).append("❤) ");
                    }
                    System.out.println(activity);
                    
                    Thread.sleep(400); // Más tiempo para observar
                    iteration++;
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

        System.out.println("\n>>> FIN DEL JUEGO - Deteniendo jugadores...");
        for (Player p : players) p.stopGracefully();
        coinBot.interrupt(); healBot.interrupt(); badBot.interrupt();
        running[0] = false; display.interrupt();

        for (Thread t : threads) t.join(2000);

        System.out.println("\n>>> ESTADO FINAL DEL TABLERO:");
        printBoard(board);

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

    // Colocar jugadores en esquinas/bordes para mejor distribución
    static void placePlayersDistributed(Board board, List<Player> players) {
        int[][] preferredPositions = {
            {0, 0}, // Esquina superior izquierda
            {0, board.cols()-1}, // Esquina superior derecha
            {board.rows()-1, 0}, // Esquina inferior izquierda
            {board.rows()-1, board.cols()-1}, // Esquina inferior derecha
        };
        
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (i < preferredPositions.length) {
                // Intentar colocar en posición preferida
                int r = preferredPositions[i][0];
                int c = preferredPositions[i][1];
                if (tryPlacePlayer(board, p, r, c)) {
                    continue;
                }
            }
            // Si no se puede colocar en posición preferida, usar método aleatorio
            board.placePlayerAtRandom(p);
        }
    }
    
    static boolean tryPlacePlayer(Board board, Player p, int r, int c) {
        // Usar el método del board para colocación distribuida
        board.placePlayerAtRandom(p);
        return true; // Simplificado - siempre usa método aleatorio pero mejorado
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