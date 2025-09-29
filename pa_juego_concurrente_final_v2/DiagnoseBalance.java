import Objects.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DiagnoseBalance {
    private static Map<String, AtomicInteger> itemsCollected = new ConcurrentHashMap<>();
    private static Map<String, AtomicInteger> movementCounts = new ConcurrentHashMap<>();
    private static Map<String, AtomicInteger> healItemsFound = new ConcurrentHashMap<>();
    private static Map<String, AtomicInteger> coinItemsFound = new ConcurrentHashMap<>();
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== DIAGNÓSTICO DE BALANCE ===");
        
        Board board = new Board(6, 6);
        board.setTargetHeals(8); // Reducir vidas disponibles
        
        String[] names = {"Ana", "Bob", "Carlos", "Diana"};
        Player[] players = new Player[4];
        Thread[] threads = new Thread[4];
        
        // Inicializar contadores
        for (String name : names) {
            itemsCollected.put(name, new AtomicInteger(0));
            movementCounts.put(name, new AtomicInteger(0));
            healItemsFound.put(name, new AtomicInteger(0));
            coinItemsFound.put(name, new AtomicInteger(0));
        }
        
        // Crear jugadores con tiempos más balanceados
        for (int i = 0; i < 4; i++) {
            players[i] = new Player(names[i], i + 1, board, 3, 200, 400); // Tiempos más normales
            board.placePlayerAtRandom(players[i]);
            System.out.println(names[i] + " colocado en (" + players[i].row() + "," + players[i].col() + ")");
        }
        
        // Robots con colocación más lenta y equilibrada  
        Thread coinBot = new Thread(new Robot(board, Booster.COIN, 300, 600));
        Thread healBot = new Thread(new Robot(board, Booster.HEAL, 800, 1200));
        Thread trapBot = new Thread(new Robot(board, Booster.POISON, 1000, 2000));
        
        coinBot.start();
        healBot.start();
        trapBot.start();
        
        // Monitor detallado de posiciones anteriores
        int[][] lastPositions = new int[4][2];
        for (int i = 0; i < 4; i++) {
            lastPositions[i][0] = players[i].row();
            lastPositions[i][1] = players[i].col();
        }
        
        // Iniciar jugadores
        for (int i = 0; i < 4; i++) {
            threads[i] = new Thread(players[i]);
            threads[i].start();
        }
        
        // Monitor cada 500ms durante 10 segundos
        long startTime = System.currentTimeMillis();
        int iteration = 0;
        
        while (System.currentTimeMillis() - startTime < 10000) {
            Thread.sleep(500);
            
            // Verificar movimientos y recolección de items
            for (int i = 0; i < 4; i++) {
                Player p = players[i];
                String name = p.name();
                
                // Contar movimientos
                if (p.row() != lastPositions[i][0] || p.col() != lastPositions[i][1]) {
                    movementCounts.get(name).incrementAndGet();
                    lastPositions[i][0] = p.row();
                    lastPositions[i][1] = p.col();
                }
                
                // Detectar recolección excesiva
                if (p.coins() > itemsCollected.get(name).get()) {
                    coinItemsFound.get(name).incrementAndGet();
                    itemsCollected.get(name).set(p.coins());
                }
            }
            
            // Mostrar estado cada 2 segundos
            if (iteration % 4 == 0) {
                System.out.println("\n--- SEGUNDO " + (iteration/2) + " ---");
                
                for (int i = 0; i < 4; i++) {
                    Player p = players[i];
                    String name = p.name();
                    System.out.println(name + 
                        " | Pos: (" + p.row() + "," + p.col() + 
                        ") | Movimientos: " + movementCounts.get(name).get() +
                        " | Monedas: " + p.coins() + 
                        " | Vidas: " + p.lifes() +
                        " | Items encontrados: " + coinItemsFound.get(name).get());
                }
                
                // Verificar si hay dominancia excesiva
                checkDominance(players);
                
                // Mostrar tablero
                System.out.println("Tablero:");
                printSimpleBoard(board, players);
            }
            
            iteration++;
        }
        
        // Detener juego
        for (int i = 0; i < 4; i++) {
            players[i].stopGracefully();
        }
        coinBot.interrupt();
        healBot.interrupt();  
        trapBot.interrupt();
        
        // Análisis final
        System.out.println("\n=== ANÁLISIS FINAL ===");
        for (int i = 0; i < 4; i++) {
            Player p = players[i];
            String name = p.name();
            System.out.println(name + ":");
            System.out.println("  Movimientos totales: " + movementCounts.get(name).get());
            System.out.println("  Monedas finales: " + p.coins());
            System.out.println("  Vidas finales: " + p.lifes());
            System.out.println("  Eficiencia (monedas/movimiento): " + 
                (movementCounts.get(name).get() > 0 ? 
                    String.format("%.2f", (double)p.coins() / movementCounts.get(name).get()) : "0"));
        }
        
        analyzeBalance(players);
    }
    
    static void checkDominance(Player[] players) {
        int maxCoins = 0, minCoins = Integer.MAX_VALUE;
        String dominant = "", weakest = "";
        
        for (Player p : players) {
            if (p.lifes() > 0) {
                if (p.coins() > maxCoins) {
                    maxCoins = p.coins();
                    dominant = p.name();
                }
                if (p.coins() < minCoins) {
                    minCoins = p.coins();
                    weakest = p.name();
                }
            }
        }
        
        if (maxCoins > minCoins * 3) { // Si el líder tiene 3x más que el último
            System.out.println("⚠️ DOMINANCIA DETECTADA: " + dominant + " (" + maxCoins + ") vs " + weakest + " (" + minCoins + ")");
        }
    }
    
    static void printSimpleBoard(Board board, Player[] players) {
        for (int i = 0; i < board.rows(); i++) {
            for (int j = 0; j < board.cols(); j++) {
                boolean foundPlayer = false;
                for (Player p : players) {
                    if (p.row() == i && p.col() == j && p.lifes() > 0) {
                        System.out.print(p.name().charAt(0) + " ");
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
    
    static void analyzeBalance(Player[] players) {
        System.out.println("\n=== EVALUACIÓN DE BALANCE ===");
        
        int[] coins = new int[4];
        for (int i = 0; i < 4; i++) {
            coins[i] = players[i].coins();
        }
        Arrays.sort(coins);
        
        int range = coins[3] - coins[0];
        double avg = Arrays.stream(coins).average().orElse(0);
        
        System.out.println("Rango de monedas: " + coins[0] + " - " + coins[3] + " (diferencia: " + range + ")");
        System.out.println("Promedio: " + String.format("%.1f", avg));
        
        if (range > avg * 2) {
            System.out.println("❌ JUEGO DESBALANCEADO: Diferencia excesiva entre jugadores");
            System.out.println("Recomendaciones:");
            System.out.println("- Aumentar tiempo de sleep de jugadores");
            System.out.println("- Reducir velocidad de robots");
            System.out.println("- Distribuir mejor los recursos iniciales");
        } else {
            System.out.println("✅ Balance aceptable");
        }
    }
}