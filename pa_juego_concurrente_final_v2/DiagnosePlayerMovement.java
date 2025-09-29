import Objects.*;
import java.util.*;
import java.util.concurrent.*;

public class DiagnosePlayerMovement {
    
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
        System.out.println("=== DIAGNÓSTICO DE MOVIMIENTO DE JUGADORES ===\n");
        
        Board board = new Board(5, 5);
        board.setTargetHeals(2);
        
        // Crear 4 jugadores para probar el problema
        Player p1 = new Player("Ana", 1, board, 2, 100, 300);
        Player p2 = new Player("Bob", 2, board, 2, 100, 300);
        Player p3 = new Player("Carlos", 3, board, 2, 100, 300);
        Player p4 = new Player("Diana", 4, board, 2, 100, 300);
        
        List<Player> players = Arrays.asList(p1, p2, p3, p4);
        
        // Colocar jugadores
        for (Player p : players) {
            board.placePlayerAtRandom(p);
            System.out.println(p.name() + " colocado en (" + p.row() + ", " + p.col() + ")");
        }
        
        // Agregar algunos boosters
        for (int i = 0; i < 3; i++) {
            board.tryPlaceCoin(10);
            board.tryPlaceHeal();
            board.tryPlaceTrap();
        }
        
        System.out.println("\nEstado inicial:");
        printBoard(board);
        
        // Crear contadores de movimiento para cada jugador
        final Map<String, Integer> movementCounts = new ConcurrentHashMap<>();
        final Map<String, String> lastPositions = new ConcurrentHashMap<>();
        
        for (Player p : players) {
            movementCounts.put(p.name(), 0);
            lastPositions.put(p.name(), p.row() + "," + p.col());
        }
        
        // Crear hilos para los jugadores
        List<Thread> threads = new ArrayList<>();
        final boolean[] running = { true };
        
        // Thread de monitoreo
        Thread monitor = new Thread(() -> {
            try {
                int iteration = 0;
                while (running[0] && iteration < 30) {
                    Thread.sleep(200);
                    
                    System.out.println("\n--- Iteración " + iteration + " ---");
                    
                    // Verificar si los jugadores se han movido
                    for (Player p : players) {
                        String currentPos = p.row() + "," + p.col();
                        String lastPos = lastPositions.get(p.name());
                        
                        if (!currentPos.equals(lastPos)) {
                            movementCounts.put(p.name(), movementCounts.get(p.name()) + 1);
                            lastPositions.put(p.name(), currentPos);
                        }
                        
                        System.out.println(p.name() + ": (" + p.row() + ", " + p.col() + ") - Movimientos: " + movementCounts.get(p.name()) + " - Monedas: " + p.coins() + " - Vidas: " + p.lifes());
                    }
                    
                    // Probar pathfinding para cada jugador
                    PathPlanner planner = new PathPlanner();
                    for (Player p : players) {
                        var path = planner.plan(p.row(), p.col(), board, 3);
                        System.out.println("  " + p.name() + " pathfinding: " + (path.size() > 1 ? "OK (" + path.size() + " pasos)" : "SIN RUTA"));
                    }
                    
                    printBoard(board);
                    iteration++;
                }
            } catch (InterruptedException ignored) {}
        }, "Monitor");
        
        // Start players
        for (Player p : players) {
            Thread t = new Thread(p, "Player-"+p.name());
            threads.add(t);
            t.start();
        }
        monitor.start();
        
        // Correr por 6 segundos
        Thread.sleep(6000);
        
        // Detener jugadores
        for (Player p : players) {
            p.stopGracefully();
        }
        running[0] = false;
        
        for (Thread t : threads) {
            t.join(1000);
        }
        monitor.join(1000);
        
        System.out.println("\n=== RESUMEN FINAL ===");
        for (Player p : players) {
            int movements = movementCounts.get(p.name());
            System.out.println(p.name() + ": " + movements + " movimientos, " + p.coins() + " monedas, " + p.lifes() + " vidas");
            
            if (movements == 0) {
                System.out.println("  ⚠️  " + p.name() + " NO SE MOVIÓ EN ABSOLUTO!");
            } else if (movements < 3) {
                System.out.println("  ⚠️  " + p.name() + " se movió muy poco");
            }
        }
        
        System.out.println("\n=== ESTADO FINAL DEL TABLERO ===");
        printBoard(board);
    }
}