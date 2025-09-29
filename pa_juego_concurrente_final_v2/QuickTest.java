import Objects.*;
import java.util.*;

public class QuickTest {
    
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
        System.out.println("=== PRUEBA DE CAMBIOS ===");
        
        Board board = new Board(5, 5);
        board.setTargetHeals(2);
        
        // Crear jugadores con nombres variados para probar las iniciales
        Player p1 = new Player("Ana Maria", 1, board, 2, 250, 500);
        Player p2 = new Player("Bob", 2, board, 2, 250, 500);
        Player p3 = new Player("Carlos David", 3, board, 2, 250, 500);
        Player p4 = new Player("X", 4, board, 2, 250, 500);
        
        // Mostrar las iniciales de cada jugador
        System.out.println("Iniciales de los jugadores:");
        System.out.println("- " + p1.name() + " -> " + p1.getInitials());
        System.out.println("- " + p2.name() + " -> " + p2.getInitials());
        System.out.println("- " + p3.name() + " -> " + p3.getInitials());
        System.out.println("- " + p4.name() + " -> " + p4.getInitials());
        
        // Colocar jugadores en el tablero
        board.placePlayerAtRandom(p1);
        board.placePlayerAtRandom(p2);
        board.placePlayerAtRandom(p3);
        board.placePlayerAtRandom(p4);
        
        // Agregar algunos boosters
        board.tryPlaceCoin(10);
        board.tryPlaceCoin(20);
        board.tryPlaceHeal();
        board.tryPlaceHeal();
        board.tryPlaceTrap(); // Veneno (no debería aparecer)
        board.tryPlaceTrap(); // Más veneno (no debería aparecer)
        
        System.out.println("\nTablero inicial con jugadores y boosters:");
        System.out.println("Leyenda: $ = monedas, + = vidas, iniciales coloreadas = jugadores");
        System.out.println("Nota: Los venenos están presentes pero no son visibles\n");
        
        printBoard(board);
        
        System.out.println("\n=== FIN DE LA PRUEBA ===");
    }
}