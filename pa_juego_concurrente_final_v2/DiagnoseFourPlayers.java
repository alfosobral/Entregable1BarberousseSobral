import Objects.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DiagnoseFourPlayers {
    public static void main(String[] args) throws Exception {
        System.out.println("=== DIAGNÓSTICO CON 4 JUGADORES ===");
        
        // Usar exactamente la misma configuración que tu juego
        Scanner sc = new Scanner("6\n6\n4\n200\n400\n6000\n4");
        
        int rows = sc.nextInt();
        int cols = sc.nextInt();
        int playerCnt = sc.nextInt();
        int zmin = sc.nextInt();
        int zmax = sc.nextInt();
        int duration = sc.nextInt();
        int initialLives = sc.nextInt();
        
        Board board = new Board(rows, cols);
        
        // Crear los 4 jugadores con los mismos nombres
        String[] names = {"Ana", "Bob", "Carlos", "Diana"};
        Player[] players = new Player[playerCnt];
        Thread[] threads = new Thread[playerCnt];
        AtomicInteger[] movementCounters = new AtomicInteger[playerCnt];
        int[][] lastPositions = new int[playerCnt][2];
        
        for (int i = 0; i < playerCnt; i++) {
            players[i] = new Player(names[i], i + 1, board, initialLives, zmin, zmax);
            board.placePlayerAtRandom(players[i]);
            movementCounters[i] = new AtomicInteger(0);
            lastPositions[i][0] = players[i].row();
            lastPositions[i][1] = players[i].col();
            
            System.out.println(names[i] + " colocado en (" + players[i].row() + "," + players[i].col() + ")");
        }
        
        // Crear robots para colocar items
        Robot coinRobot = new Robot(board, Booster.COIN, 100, 500);
        Robot healRobot = new Robot(board, Booster.HEAL, 1000, 2000);
        Robot trapRobot = new Robot(board, Booster.POISON, 800, 1500);
        
        Thread coinThread = new Thread(coinRobot);
        Thread healThread = new Thread(healRobot);
        Thread trapThread = new Thread(trapRobot);
        
        // Iniciar robots
        coinThread.start();
        healThread.start();
        trapThread.start();
        
        // Iniciar jugadores
        for (int i = 0; i < playerCnt; i++) {
            threads[i] = new Thread(players[i]);
            threads[i].start();
        }
        
        // Monitor detallado
        long startTime = System.currentTimeMillis();
        int iteration = 0;
        
        while (System.currentTimeMillis() - startTime < duration) {
            Thread.sleep(200); // Cada 200ms
            
            // Verificar movimientos
            boolean anyMovement = false;
            for (int i = 0; i < playerCnt; i++) {
                Player p = players[i];
                int[] lastPos = lastPositions[i];
                
                if (p.row() != lastPos[0] || p.col() != lastPos[1]) {
                    movementCounters[i].incrementAndGet();
                    lastPos[0] = p.row();
                    lastPos[1] = p.col();
                    anyMovement = true;
                }
            }
            
            // Mostrar estado cada segundo
            if (iteration % 5 == 0) {
                System.out.println("\n--- ITERACIÓN " + iteration + " ---");
                for (int i = 0; i < playerCnt; i++) {
                    Player p = players[i];
                    System.out.println(p.name() + ": (" + p.row() + "," + p.col() + 
                        ") - Movimientos: " + movementCounters[i].get() + 
                        " - Monedas: " + p.coins() + " - Vidas: " + p.lifes());
                    
                    // Verificar pathfinding
                    PathPlanner planner = new PathPlanner();
                    List<Pos> path = planner.plan(p.row(), p.col(), board, 3);
                    System.out.println("  " + p.name() + " pathfinding: " + 
                        (path.isEmpty() ? "❌ SIN RUTA" : "✅ OK (" + path.size() + " pasos)"));
                }
                
                // Mostrar tablero
                printBoard(board, players);
                
                // Verificar jugadores "quietos"
                List<String> stuckPlayers = new ArrayList<>();
                for (int i = 0; i < playerCnt; i++) {
                    if (movementCounters[i].get() < iteration / 10) { // Menos de 1 movimiento por segundo
                        stuckPlayers.add(players[i].name());
                    }
                }
                
                if (!stuckPlayers.isEmpty()) {
                    System.out.println("⚠️ JUGADORES APARENTEMENTE QUIETOS: " + stuckPlayers);
                    
                    // Diagnóstico detallado para jugadores quietos
                    for (String name : stuckPlayers) {
                        for (int i = 0; i < playerCnt; i++) {
                            if (players[i].name().equals(name)) {
                                diagnosticStuckPlayer(players[i], board);
                                break;
                            }
                        }
                    }
                }
            }
            
            iteration++;
        }
        
        // Detener todo
        for (int i = 0; i < playerCnt; i++) {
            players[i].stopGracefully();
        }
        coinThread.interrupt();
        healThread.interrupt();
        trapThread.interrupt();
        
        System.out.println("\n=== RESUMEN FINAL ===");
        for (int i = 0; i < playerCnt; i++) {
            Player p = players[i];
            System.out.println(p.name() + ": " + movementCounters[i].get() + 
                " movimientos, " + p.coins() + " monedas, " + p.lifes() + " vidas");
        }
        
        // Identificar jugadores problemáticos
        int minMovements = Arrays.stream(movementCounters).mapToInt(AtomicInteger::get).min().orElse(0);
        int maxMovements = Arrays.stream(movementCounters).mapToInt(AtomicInteger::get).max().orElse(0);
        
        if (maxMovements - minMovements > 10) {
            System.out.println("\n❌ PROBLEMA DETECTADO: Gran diferencia en movimientos");
            System.out.println("Diferencia: " + (maxMovements - minMovements) + " movimientos");
            
            for (int i = 0; i < playerCnt; i++) {
                if (movementCounters[i].get() == minMovements) {
                    System.out.println("Jugador con menos movimientos: " + players[i].name());
                }
            }
        } else {
            System.out.println("✅ Todos los jugadores se movieron de manera similar");
        }
    }
    
    static void printBoard(Board board, Player[] players) {
        System.out.println("Tablero actual:");
        for (int i = 0; i < board.rows(); i++) {
            for (int j = 0; j < board.cols(); j++) {
                boolean foundPlayer = false;
                for (Player p : players) {
                    if (p.row() == i && p.col() == j) {
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
    
    static void diagnosticStuckPlayer(Player p, Board board) {
        System.out.println("  🔍 Diagnóstico para " + p.name() + ":");
        System.out.println("    Posición: (" + p.row() + "," + p.col() + ")");
        System.out.println("    Vidas: " + p.lifes());
        System.out.println("    Monedas: " + p.coins());
        
        if (p.lifes() <= 0) {
            System.out.println("    ❌ JUGADOR MUERTO - Por eso no se mueve!");
            return;
        }
        
        if (p.row() == -1 || p.col() == -1) {
            System.out.println("    ❌ POSICIÓN INVÁLIDA - Jugador removido del tablero!");
            return;
        }
        
        // Verificar casillas adyacentes
        int[][] DIRS = {{-1,0},{0,1},{1,0},{0,-1}};
        int freeAdjacent = 0;
        
        for (int[] d : DIRS) {
            int nr = p.row() + d[0], nc = p.col() + d[1];
            if (nr >= 0 && nr < board.rows() && nc >= 0 && nc < board.cols()) {
                if (!board.isOccupied(nr, nc)) {
                    freeAdjacent++;
                }
            }
        }
        
        if (freeAdjacent == 0) {
            System.out.println("    ❌ RODEADO - Todas las casillas adyacentes ocupadas!");
        } else {
            System.out.println("    ✅ " + freeAdjacent + " casillas libres - Debería poder moverse");
        }
    }
}