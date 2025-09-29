import Objects.*;
import java.util.*;
import java.util.concurrent.*;

public class TestConcurrentMovement {
    
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
        System.out.println("=== TEST MOVIMIENTO CONCURRENTE ===\n");
        
        Board board = new Board(5, 5);
        board.setTargetHeals(1);
        
        // Crear 2 jugadores para simplificar el test
        Player p1 = new Player("Ana", 1, board, 2, 100, 200);
        Player p2 = new Player("Bob", 2, board, 2, 100, 200);
        
        List<Player> players = Arrays.asList(p1, p2);
        
        // Colocar jugadores
        for (Player p : players) {
            board.placePlayerAtRandom(p);
        }
        
        // Agregar algunos boosters
        board.tryPlaceCoin(10);
        board.tryPlaceHeal();
        
        System.out.println("Estado inicial:");
        printBoard(board);
        
        System.out.println("Posiciones iniciales:");
        for (Player p : players) {
            System.out.println(p.name() + ": (" + p.row() + ", " + p.col() + ")");
        }
        
        // Crear hilos para los jugadores
        List<Thread> threads = new ArrayList<>();
        final boolean[] running = { true };
        
        // Display thread
        Thread display = new Thread(() -> {
            try {
                int count = 0;
                while (running[0] && count < 20) {
                    System.out.println("\n--- Iteración " + count + " ---");
                    printBoard(board);
                    
                    // Mostrar posiciones
                    for (Player p : players) {
                        System.out.println(p.name() + ": (" + p.row() + ", " + p.col() + ") - Vidas: " + p.lifes() + " - Monedas: " + p.coins());
                    }
                    
                    Thread.sleep(500);
                    count++;
                }
            } catch (InterruptedException ignored) {}
        }, "Display");
        
        // Start players
        for (Player p : players) {
            Thread t = new Thread(p, "Player-"+p.name());
            threads.add(t);
            t.start();
        }
        display.start();
        
        // Correr por 3 segundos
        Thread.sleep(3000);
        
        // Detener jugadores
        for (Player p : players) {
            p.stopGracefully();
        }
        running[0] = false;
        
        for (Thread t : threads) {
            t.join(1000);
        }
        display.join(1000);
        
        System.out.println("\n=== ESTADO FINAL ===");
        printBoard(board);
        
        for (Player p : players) {
            System.out.println(p.name() + ": Monedas=" + p.coins() + ", Vidas=" + p.lifes());
        }
    }
}