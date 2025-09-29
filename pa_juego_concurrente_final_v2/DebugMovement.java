import Objects.*;
import java.util.*;

public class DebugMovement {
    
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
        System.out.println("=== DEBUG MOVIMIENTO ===\n");
        
        Board board = new Board(5, 5);
        
        // Crear 3 jugadores
        Player p1 = new Player("Ana", 1, board, 2, 250, 500);
        Player p2 = new Player("Bob", 2, board, 2, 250, 500);
        Player p3 = new Player("Carlos", 3, board, 2, 250, 500);
        
        // Colocar jugadores
        board.placePlayerAtRandom(p1);
        board.placePlayerAtRandom(p2);
        board.placePlayerAtRandom(p3);
        
        System.out.println("Posiciones iniciales:");
        System.out.println("Ana: (" + p1.row() + ", " + p1.col() + ")");
        System.out.println("Bob: (" + p2.row() + ", " + p2.col() + ")");
        System.out.println("Carlos: (" + p3.row() + ", " + p3.col() + ")");
        
        printBoard(board);
        
        // Agregar algunos boosters
        board.tryPlaceCoin(10);
        board.tryPlaceHeal();
        
        System.out.println("\nCon boosters:");
        printBoard(board);
        
        // Probar el pathfinding para cada jugador
        PathPlanner planner = new PathPlanner();
        
        for (Player p : Arrays.asList(p1, p2, p3)) {
            System.out.println("\n--- PathPlanning para " + p.name() + " desde (" + p.row() + ", " + p.col() + ") ---");
            var path = planner.plan(p.row(), p.col(), board, 3);
            if (path.isEmpty()) {
                System.out.println("¡NO SE ENCONTRÓ RUTA!");
            } else {
                System.out.println("Ruta encontrada con " + path.size() + " pasos:");
                for (int i = 0; i < path.size(); i++) {
                    var pos = path.get(i);
                    System.out.println("  Paso " + i + ": (" + pos.r() + ", " + pos.c() + ")");
                }
            }
        }
        
        // Probar movimientos manuales
        System.out.println("\n--- PROBANDO MOVIMIENTOS MANUALES ---");
        
        for (Player p : Arrays.asList(p1, p2, p3)) {
            System.out.println("\nIntentando mover " + p.name() + " desde (" + p.row() + ", " + p.col() + ")");
            
            // Intentar mover hacia la derecha
            int newCol = p.col() + 1;
            if (newCol < board.cols()) {
                Booster result = board.movePlayerSafe(p.row(), p.col(), p.row(), newCol, p);
                System.out.println("Resultado del movimiento: " + result);
                System.out.println("Nueva posición: (" + p.row() + ", " + p.col() + ")");
            } else {
                System.out.println("No se puede mover hacia la derecha (fuera de límites)");
            }
            
            printBoard(board);
        }
        
        System.out.println("\n=== FIN DEBUG ===");
    }
}