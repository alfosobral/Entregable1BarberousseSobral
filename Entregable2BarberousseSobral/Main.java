import Objects.*;
import java.io.*;
import java.util.*;

public class Main {

    static void printBoard(Board board) {
        String[][] s = board.snapshotWithColors();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length; i++) {
            for (int j = 0; j < s[0].length; j++) {
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

        boolean INPUT_DATA = false;

        long Zmin = 350;
        long Zmax = 550;

        long Xmin = 1000;
        long Xmax = 1700;

        long Ymin = 800;
        long Ymax = 1500;

        long Wmin = 1200;
        long Wmax = 1900;

        int initial_lifes = 2;
        int H = (int) Math.floor(N * N * 0.10);

        if (INPUT_DATA) {
            System.out.print("Zmin jugador (ms): ");
            Zmin = readLong(sc, 10, 5000, 250);

            System.out.print("Zmax jugador (ms): ");
            Zmax = readLong(sc, Zmin, 8000, Math.max(500, Zmin + 500));

            System.out.print("Ymin robot monedas (ms): ");
            Ymin = readLong(sc, 10, 5000, 150);

            System.out.print("Ymax robot monedas (ms): ");
            Ymax = readLong(sc, Ymin, 8000, Math.max(400, Ymin + 400));

            System.out.print("Xmin robot vidas (ms): ");
            Xmin = readLong(sc, 10, 5000, 300);

            System.out.print("Xmax robot vidas (ms): ");
            // FIX: usar Xmin como mínimo (antes había Wmin por error)
            Xmax = readLong(sc, Xmin, 8000, Math.max(600, Xmin + 300));

            System.out.print("Wmin robot malo (ms): ");
            Wmin = readLong(sc, 10, 5000, 300);

            System.out.print("Wmax robot malo (ms): ");
            Wmax = readLong(sc, Wmin, 8000, Math.max(600, Wmin + 300));

            System.out.print("Cantidad de vidas H a colocar (fijo, +1 por casilla): ");
            H = readInt(sc, 0, N * N, Math.max(1, (int) (0.05 * N * N)));

            System.out.print("Cantidad de vidas iniciales por jugador: ");
            initial_lifes = readInt(sc, 1, 10, 3);
        }

        System.out.print("Cantidad de jugadores M (1 a 4): ");
        int M = readInt(sc, 1, 4, 3);

        List<String> names = new ArrayList<>();
        for (int i = 1; i <= M; i++) {
            System.out.print("Nombre del jugador " + i + ": ");
            String nm = sc.nextLine().trim(); 
            if (nm.isEmpty()) nm = "Jugador" + i;
            names.add(nm);
        }

        Board board = new Board(N, N);
        board.setTargetHeals(H);

        Thread coinBot = new Thread(new Robot(board, Booster.COIN, Ymin, Ymax), "RobotMonedas");
        Thread healBot = new Thread(new Robot(board, Booster.HEAL, Xmin, Xmax), "RobotVidas");
        Thread badBot  = new Thread(new Robot(board, Booster.POISON, Wmin, Wmax), "RobotMalo");

        List<Player> players = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        int id = 1;
        for (String nm : names) {
            Player p = new Player(nm, id++, board, initial_lifes, Zmin, Zmax);
            players.add(p);
        }
        for (Player p : players) board.placePlayerAtRandom(p);

        final boolean[] running = { true };
        Thread display = new Thread(() -> {
            try {
                while (running[0]) {
                    System.out.println();
                    printBoard(board);
                    Thread.sleep(300);
                }
            } catch (InterruptedException ignored) {}
        }, "Display");

        coinBot.start();
        healBot.start();
        badBot.start();

        for (Player p : players) {
            Thread t = new Thread(p, "Player-" + p.name());
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

        players.sort((a, b) -> {
            int aliveA = a.lifes() > 0 ? 1 : 0;
            int aliveB = b.lifes() > 0 ? 1 : 0;
            if (aliveA != aliveB) return Integer.compare(aliveB, aliveA);
            if (a.coins() != b.coins()) return Integer.compare(b.coins(), a.coins());
            if (a.lifes() != b.lifes()) return Integer.compare(b.lifes(), a.lifes());
            return a.name().compareToIgnoreCase(b.name());
        });

        StringBuilder report = new StringBuilder();
        report.append("==== RESULTADOS DE LA PARTIDA ====\n");
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            report.append(String.format("%d) %s  | monedas=%d | vidas=%d%n", i + 1, p.name(), p.coins(), p.lifes()));
        }

        System.out.println("\n" + report);

        String filename = "partida_" + java.time.LocalDateTime.now().toString().replace(':', '-') + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(filename, false))) {
            out.print(report.toString());
        }
        System.out.println("Log guardado en: " + filename);

        askForNewGame(players, N, T, initial_lifes, Zmin, Zmax, Ymin, Ymax, Xmin, Xmax, Wmin, Wmax, H, sc);

        sc.close();
    }

    private static void askForNewGame(List<Player> previousPlayers, int boardSize, int gameTime,
                                      int initialLives, long Zmin, long Zmax, long Ymin, long Ymax,
                                      long Xmin, long Xmax, long Wmin, long Wmax, int H, Scanner sc) throws Exception {

        int alivePlayers = 0;
        for (Player p : previousPlayers) {
            if (p.lifes() > 0) {
                alivePlayers++;
            }
        }

        System.out.println("\n=== ¿QUÉ DESEAS HACER? ===");
        
        if (alivePlayers < 2) {
            System.out.println("Solo hay " + alivePlayers + " jugador vivo. Se necesitan al menos 2 para continuar.");
            System.out.println("1) Nueva partida completa (configurar todo de nuevo)");
            System.out.println("2) Terminar simulación");
            System.out.print("Opción (1-2): ");
            
            String input = sc.nextLine().trim();
            int option = 2; 
            try {
                option = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Opción inválida, terminando...");
            }

            System.out.println("Opción elegida: " + option);

            switch (option) {
                case 1 -> {
                    System.out.println("Iniciando nueva partida completa...");
                    startCompleteNewGame(sc);
                }
                default -> {
                    System.out.println("¡Gracias por jugar!");
                    return;
                }
            }
            return;
        }

        System.out.println(" Jugadores vivos: " + alivePlayers);
        System.out.println("1) Continuar con los jugadores actuales (mismo estado)");
        System.out.println("2) Nueva partida con jugadores sobrevivientes (2 vidas, 0 monedas)");
        System.out.println("3) Nueva partida completa (configurar todo de nuevo)");
        System.out.println("4) Terminar simulación");
        System.out.print("Opción (1-4): ");

        String input = sc.nextLine().trim();

        int option = 4; 
        try {
            option = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Opción inválida, terminando...");
        }

        System.out.println("Opción elegida: " + option);

        switch (option) {
            case 1 -> {
                System.out.println(" Continuando con los jugadores actuales...");
                continueWithCurrentPlayers(previousPlayers, boardSize, gameTime, initialLives,
                        Zmin, Zmax, Ymin, Ymax, Xmin, Xmax, Wmin, Wmax, H, sc);
            }
            case 2 -> {
                List<Player> survivors = new ArrayList<>();
                System.out.println("\n Verificando sobrevivientes:");
                for (Player p : previousPlayers) {
                    System.out.println("  " + p.name() + ": vidas=" + p.lifes());
                    if (p.lifes() > 0) {
                        survivors.add(p);
                    }
                }
                if (survivors.size() < 2) {
                    System.out.println("Solo " + survivors.size() + " sobreviviente(s). Se necesitan al menos 2.");
                    System.out.println("Cambiando a nueva partida completa...");
                    startCompleteNewGame(sc);
                    return;
                } else {
                    System.out.println(" " + survivors.size() + " sobrevivientes encontrados!");
                    runNewGame(survivors, boardSize, gameTime, 2,
                            Zmin, Zmax, Ymin, Ymax, Xmin, Xmax, Wmin, Wmax, H, true, sc);
                }
            }
            case 3 -> {
                System.out.println(" Iniciando nueva partida completa...");
                startCompleteNewGame(sc);
            }
            default -> {
                System.out.println("¡Gracias por jugar!");
                return;
            }
        }
    }

    private static void runNewGame(List<Player> oldPlayers, int boardSize, int gameTime, int initialLives,
                                   long Zmin, long Zmax, long Ymin, long Ymax, long Xmin, long Xmax,
                                   long Wmin, long Wmax, int H, boolean reviveAll, Scanner sc) throws Exception {

        System.out.println("\n ¡INICIANDO NUEVA PARTIDA!");

        Board board = new Board(boardSize, boardSize);
        board.setTargetHeals(H);

        List<Player> players = new ArrayList<>();
        int id = 1;
        for (Player oldPlayer : oldPlayers) {
            int newLives = reviveAll ? initialLives : Math.max(1, oldPlayer.lifes());
            Player newPlayer = new Player(oldPlayer.name(), id++, board, newLives, Zmin, Zmax);
            
            if (!reviveAll && oldPlayer.coins() > 0 && initialLives != 2) {
                newPlayer.addCoins(oldPlayer.coins());
            }
                    
            players.add(newPlayer);
            System.out.println(" " + newPlayer.name() + " - Vidas: " + newPlayer.lifes() + " - Monedas: " + newPlayer.coins());
        }

        for (Player p : players) {
            board.placePlayerAtRandom(p);
        }

        Thread coinBot = new Thread(new Robot(board, Booster.COIN, Ymin, Ymax), "RobotMonedas");
        Thread healBot = new Thread(new Robot(board, Booster.HEAL, Xmin, Xmax), "RobotVidas");
        Thread badBot  = new Thread(new Robot(board, Booster.POISON, Wmin, Wmax), "RobotMalo");

        List<Thread> threads = new ArrayList<>();
        for (Player p : players) {
            Thread t = new Thread(p, "Player-" + p.name());
            threads.add(t);
        }

        final boolean[] running = { true };
        Thread display = new Thread(() -> {
            try {
                while (running[0]) {
                    System.out.println();
                    printBoard(board);
                    Thread.sleep(300);
                }
            } catch (InterruptedException ignored) {}
        }, "Display");

        coinBot.start();
        healBot.start();
        badBot.start();
        display.start();

        for (Thread t : threads) t.start();

        System.out.println("Duración: " + gameTime + " segundos");

        long deadline = System.currentTimeMillis() + gameTime * 1000L;
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

        players.sort((a, b) -> {
            int aliveA = a.lifes() > 0 ? 1 : 0;
            int aliveB = b.lifes() > 0 ? 1 : 0;
            if (aliveA != aliveB) return Integer.compare(aliveB, aliveA);
            if (a.coins() != b.coins()) return Integer.compare(b.coins(), a.coins());
            if (a.lifes() != b.lifes()) return Integer.compare(b.lifes(), a.lifes());
            return a.name().compareToIgnoreCase(b.name());
        });

        StringBuilder report = new StringBuilder();
        report.append("==== RESULTADOS NUEVA PARTIDA ====\n");
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            report.append(String.format("%d) %s  | monedas=%d | vidas=%d%n", i + 1, p.name(), p.coins(), p.lifes()));
        }

        System.out.println("\n" + report);

        String filename = "nueva_partida_" + java.time.LocalDateTime.now().toString().replace(':', '-') + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(filename, false))) {
            out.print(report.toString());
        }
        System.out.println("Log guardado en: " + filename);

        askForNewGame(players, boardSize, gameTime, initialLives, Zmin, Zmax, Ymin, Ymax, Xmin, Xmax, Wmin, Wmax, H, sc);
    }

    private static void continueWithCurrentPlayers(List<Player> currentPlayers, int boardSize, int gameTime,
                                                   int initialLives, long Zmin, long Zmax, long Ymin, long Ymax,
                                                   long Xmin, long Xmax, long Wmin, long Wmax, int H, Scanner sc) throws Exception {

        System.out.println("\n ¡CONTINUANDO CON LOS JUGADORES ACTUALES!");

        List<Player> alivePlayers = new ArrayList<>();
        System.out.println("\n Estado actual de los jugadores:");
        for (Player p : currentPlayers) {
            System.out.println("  " + p.name() + " - Vidas: " + p.lifes() + " - Monedas: " + p.coins());
            if (p.lifes() > 0) alivePlayers.add(p);
        }

        if (alivePlayers.size() < 2) {
            System.out.println("No hay suficientes jugadores vivos para continuar.");
            System.out.println("Cambiando a nueva partida con todos revividos...");
            runNewGame(currentPlayers, boardSize, gameTime, initialLives, Zmin, Zmax, Ymin, Ymax, Xmin, Xmax, Wmin, Wmax, H, true, sc);
            return;
        }

        System.out.println("Continuando con " + alivePlayers.size() + " jugadores vivos");

        Board board = new Board(boardSize, boardSize);
        board.setTargetHeals(H);

        List<Player> players = new ArrayList<>();
        int id = 1;
        for (Player oldPlayer : alivePlayers) {
            Player continuingPlayer = new Player(oldPlayer.name(), id++, board, oldPlayer.lifes(), Zmin, Zmax);
            if (oldPlayer.coins() > 0) continuingPlayer.addCoins(oldPlayer.coins());
            players.add(continuingPlayer);
        }

        for (Player p : players) board.placePlayerAtRandom(p);

        Thread coinBot = new Thread(new Robot(board, Booster.COIN, Ymin, Ymax), "RobotMonedas");
        Thread healBot = new Thread(new Robot(board, Booster.HEAL, Xmin, Xmax), "RobotVidas");
        Thread badBot  = new Thread(new Robot(board, Booster.POISON, Wmin, Wmax), "RobotMalo");

        List<Thread> threads = new ArrayList<>();
        for (Player p : players) {
            Thread t = new Thread(p, "Player-" + p.name());
            threads.add(t);
        }

        final boolean[] running = { true };
        Thread display = new Thread(() -> {
            try {
                while (running[0]) {
                    System.out.println();
                    printBoard(board);
                    Thread.sleep(300);
                }
            } catch (InterruptedException ignored) {}
        }, "Display");

        coinBot.start();
        healBot.start();
        badBot.start();
        display.start();

        for (Thread t : threads) t.start();

        System.out.println("Duración: " + gameTime + " segundos");

        long deadline = System.currentTimeMillis() + gameTime * 1000L;
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

        players.sort((a, b) -> {
            int aliveA = a.lifes() > 0 ? 1 : 0;
            int aliveB = b.lifes() > 0 ? 1 : 0;
            if (aliveA != aliveB) return Integer.compare(aliveB, aliveA);
            if (a.coins() != b.coins()) return Integer.compare(b.coins(), a.coins());
            if (a.lifes() != b.lifes()) return Integer.compare(b.lifes(), a.lifes());
            return a.name().compareToIgnoreCase(b.name());
        });

        StringBuilder report = new StringBuilder();
        report.append("==== RESULTADOS PARTIDA CONTINUADA ====\n");
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            report.append(String.format("%d) %s  | monedas=%d | vidas=%d%n", i + 1, p.name(), p.coins(), p.lifes()));
        }

        System.out.println("\n" + report);

        String filename = "partida_continuada_" + java.time.LocalDateTime.now().toString().replace(':', '-') + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(filename, false))) {
            out.print(report.toString());
        }
        System.out.println("Log guardado en: " + filename);

        askForNewGame(players, boardSize, gameTime, initialLives, Zmin, Zmax, Ymin, Ymax, Xmin, Xmax, Wmin, Wmax, H, sc);
    }
    
    private static void startCompleteNewGame(Scanner sc) throws Exception {
        System.out.println("\n ===== NUEVA PARTIDA COMPLETA =====");
        System.out.println("Configurando todo desde cero...\n");
        
        System.out.print("Tamaño del tablero N (sugerido 10): ");
        int N = readInt(sc, 5, 50, 10);

        System.out.print("Duración de la partida T en segundos (sugerido 60): ");
        int T = readInt(sc, 10, 600, 60);

        boolean INPUT_DATA = false;
        long Zmin = 350, Zmax = 550;
        long Xmin = 1000, Xmax = 1700;
        long Ymin = 800, Ymax = 1500;
        long Wmin = 1200, Wmax = 1900;
        int initial_lifes = 2;
        int H = (int)Math.floor(N*N*0.10);

        if (INPUT_DATA) {
            System.out.print("Cantidad de vidas iniciales por jugador: ");
            initial_lifes = readInt(sc, 1, 10, 2);
        }

        System.out.print("Cantidad de jugadores M (1 a 4): ");
        int M = readInt(sc, 1, 4, 3);

        List<String> names = new ArrayList<>();
        for (int i=1; i<=M; i++) {
            System.out.print("Nombre del jugador " + i + ": ");
            String nm = sc.next().trim();
            if (nm.isEmpty()) nm = "Jugador" + i;
            names.add(nm);
        }
        
        sc.nextLine();

        Board board = new Board(N, N);
        board.setTargetHeals(H);

        List<Player> players = new ArrayList<>();
        int id = 1;
        for (String nm : names) {
            Player p = new Player(nm, id++, board, initial_lifes, Zmin, Zmax);
            players.add(p);
        }
        for (Player p : players) board.placePlayerAtRandom(p);

        Thread coinBot = new Thread(new Robot(board, Booster.COIN, Ymin, Ymax), "RobotMonedas");
        Thread healBot = new Thread(new Robot(board, Booster.HEAL, Xmin, Xmax), "RobotVidas");
        Thread badBot = new Thread(new Robot(board, Booster.POISON, Wmin, Wmax), "RobotMalo");

        List<Thread> threads = new ArrayList<>();
        for (Player p : players) {
            Thread t = new Thread(p, "Player-" + p.name());
            threads.add(t);
        }

        final boolean[] running = { true };
        Thread display = new Thread(() -> {
            try {
                while (running[0]) {
                    System.out.println();
                    printBoard(board);
                    Thread.sleep(300);
                }
            } catch (InterruptedException ignored) {}
        }, "Display");

        coinBot.start();
        healBot.start();
        badBot.start();
        display.start();

        for (Thread t : threads) {
            t.start();
        }

        System.out.println("\n>>> ¡Nueva partida completa iniciada con " + M + " jugadores! (duración " + T + "s)");

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
        report.append("==== RESULTADOS PARTIDA COMPLETA ====\n");
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            report.append(String.format("%d) %s  | monedas=%d | vidas=%d%n", i+1, p.name(), p.coins(), p.lifes()));
        }

        System.out.println("\n" + report);

        String filename = "partida_completa_" + java.time.LocalDateTime.now().toString().replace(':','-') + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(filename, false))) {
            out.print(report.toString());
        }
        System.out.println("Log guardado en: " + filename);

        askForNewGame(players, N, T, initial_lifes, Zmin, Zmax, Ymin, Ymax, Xmin, Xmax, Wmin, Wmax, H, sc);
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
