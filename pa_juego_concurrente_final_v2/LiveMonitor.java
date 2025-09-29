import Objects.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LiveMonitor {
    public static void main(String[] args) throws Exception {
        System.out.println("=== MONITOR EN TIEMPO REAL ===");
        System.out.println("Configuración: Tablero 6x6, 4 jugadores, 8 segundos");
        
        Board board = new Board(6, 6);
        board.setTargetHeals(5);
        
        // Crear jugadores con los mismos parámetros que tu Main.java
        String[] names = {"Ana", "Bob", "Carlos", "Diana"};
        Player[] players = new Player[4];
        Thread[] playerThreads = new Thread[4];
        
        // Contadores de movimiento
        Map<String, AtomicInteger> movementCounters = new ConcurrentHashMap<>();
        Map<String, int[]> lastPositions = new ConcurrentHashMap<>();
        
        for (int i = 0; i < 4; i++) {
            players[i] = new Player(names[i], i + 1, board, 2, 100, 200);
            board.placePlayerAtRandom(players[i]);
            movementCounters.put(names[i], new AtomicInteger(0));
            lastPositions.put(names[i], new int[]{players[i].row(), players[i].col()});
            
            System.out.println(names[i] + " colocado en (" + players[i].row() + "," + players[i].col() + ")");
        }
        
        // Robots
        Thread coinBot = new Thread(new Robot(board, Booster.COIN, 100, 200));
        Thread healBot = new Thread(new Robot(board, Booster.HEAL, 200, 400));
        Thread trapBot = new Thread(new Robot(board, Booster.POISON, 100, 200));
        
        coinBot.start();
        healBot.start();
        trapBot.start();
        
        // Iniciar jugadores
        for (int i = 0; i < 4; i++) {
            playerThreads[i] = new Thread(players[i]);
            playerThreads[i].start();
        }
        
        // Monitor de movimiento cada 100ms
        long startTime = System.currentTimeMillis();
        int iteration = 0;
        
        System.out.println("\n=== COMENZANDO MONITOREO ===");
        
        while (System.currentTimeMillis() - startTime < 8000) { // 8 segundos
            Thread.sleep(100);
            
            // Verificar movimientos
            for (int i = 0; i < 4; i++) {
                Player p = players[i];
                String name = p.name();
                int[] lastPos = lastPositions.get(name);
                
                if (p.row() != lastPos[0] || p.col() != lastPos[1]) {
                    movementCounters.get(name).incrementAndGet();
                    lastPos[0] = p.row();
                    lastPos[1] = p.col();
                    System.out.println("🚶 " + name + " se movió a (" + p.row() + "," + p.col() + 
                        ") - Movimiento #" + movementCounters.get(name).get());
                }
            }
            
            // Mostrar estado cada segundo
            if (iteration % 10 == 0) {
                System.out.println("\n--- SEGUNDO " + (iteration/10) + " ---");
                
                // Estado de cada jugador
                for (int i = 0; i < 4; i++) {
                    Player p = players[i];
                    String name = p.name();
                    System.out.println(name + ": Pos(" + p.row() + "," + p.col() + 
                        ") Movimientos:" + movementCounters.get(name).get() + 
                        " Monedas:" + p.coins() + " Vidas:" + p.lifes());
                }
                
                // Tablero visual
                System.out.println("Tablero:");
                printBoardWithPlayers(board, players);
                
                // Detectar jugadores inactivos
                List<String> inactivePlayers = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    String name = players[i].name();
                    if (movementCounters.get(name).get() < (iteration/10) * 0.5) { // Menos de 0.5 movimientos por segundo
                        inactivePlayers.add(name);
                    }
                }
                
                if (!inactivePlayers.isEmpty()) {
                    System.out.println("⚠️ JUGADORES POTENCIALMENTE INACTIVOS: " + inactivePlayers);
                    
                    for (String name : inactivePlayers) {
                        for (int i = 0; i < 4; i++) {
                            if (players[i].name().equals(name)) {
                                diagnosePlayer(players[i], board);
                                break;
                            }
                        }
                    }
                }
            }
            
            iteration++;
        }
        
        // Detener todo
        for (int i = 0; i < 4; i++) {
            players[i].stopGracefully();
        }
        coinBot.interrupt();
        healBot.interrupt();
        trapBot.interrupt();
        
        System.out.println("\n=== RESUMEN FINAL ===");
        for (int i = 0; i < 4; i++) {
            Player p = players[i];
            String name = p.name();
            System.out.println(name + ": " + movementCounters.get(name).get() + 
                " movimientos totales, " + p.coins() + " monedas, " + p.lifes() + " vidas");
        }
        
        // Análisis final
        int totalMovements = movementCounters.values().stream().mapToInt(AtomicInteger::get).sum();
        int minMovements = movementCounters.values().stream().mapToInt(AtomicInteger::get).min().orElse(0);
        int maxMovements = movementCounters.values().stream().mapToInt(AtomicInteger::get).max().orElse(0);
        
        System.out.println("\nAnálisis:");
        System.out.println("Movimientos totales: " + totalMovements);
        System.out.println("Movimientos mínimos: " + minMovements);
        System.out.println("Movimientos máximos: " + maxMovements);
        System.out.println("Diferencia: " + (maxMovements - minMovements));
        
        if (maxMovements - minMovements > 5) {
            System.out.println("❌ DESBALANCE DETECTADO: Algunos jugadores se movieron significativamente menos");
        } else {
            System.out.println("✅ MOVIMIENTO EQUILIBRADO: Todos los jugadores se movieron de manera similar");
        }
    }
    
    static void printBoardWithPlayers(Board board, Player[] players) {
        for (int i = 0; i < board.rows(); i++) {
            for (int j = 0; j < board.cols(); j++) {
                boolean foundPlayer = false;
                for (Player p : players) {
                    if (p.row() == i && p.col() == j && p.lifes() > 0) {
                        System.out.print(p.getInitials() + " ");
                        foundPlayer = true;
                        break;
                    }
                }
                if (!foundPlayer) {
                    Booster content = board.cellContent(i, j);
                    switch (content) {
                        case COIN -> System.out.print("$ ");
                        case HEAL -> System.out.print("+ ");
                        case POISON -> System.out.print("X ");
                        default -> System.out.print(". ");
                    }
                }
            }
            System.out.println();
        }
    }
    
    static void diagnosePlayer(Player p, Board board) {
        System.out.println("  🔍 " + p.name() + " diagnóstico:");
        if (p.lifes() <= 0) {
            System.out.println("    ❌ JUGADOR MUERTO");
            return;
        }
        if (p.row() == -1 || p.col() == -1) {
            System.out.println("    ❌ POSICIÓN INVÁLIDA");
            return;
        }
        
        // Verificar si puede moverse
        PathPlanner planner = new PathPlanner();
        List<Pos> path = planner.plan(p.row(), p.col(), board, 3);
        if (path.isEmpty()) {
            System.out.println("    ❌ NO PUEDE PLANIFICAR RUTA");
        } else {
            System.out.println("    ✅ Puede planificar ruta de " + path.size() + " pasos");
        }
        
        // Verificar casillas adyacentes
        int[][] dirs = {{-1,0},{0,1},{1,0},{0,-1}};
        int freeAdjacent = 0;
        for (int[] d : dirs) {
            int nr = p.row() + d[0], nc = p.col() + d[1];
            if (nr >= 0 && nr < board.rows() && nc >= 0 && nc < board.cols()) {
                if (!board.isOccupied(nr, nc)) {
                    freeAdjacent++;
                }
            }
        }
        System.out.println("    Casillas libres adyacentes: " + freeAdjacent);
    }
}