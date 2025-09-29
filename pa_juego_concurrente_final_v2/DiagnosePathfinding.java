import Objects.*;
import java.util.*;

public class DiagnosePathfinding {
    public static void main(String[] args) {
        System.out.println("=== DIAGNÓSTICO DE PATHFINDING ===");
        
        // Crear un tablero pequeño para probar
        Board board = new Board(6, 6);
        PathPlanner planner = new PathPlanner();
        
        // Agregar algunos boosters usando los métodos correctos
        board.tryPlaceCoin(5);
        board.tryPlaceHeal();
        board.tryPlaceTrap();
        
        // Probar pathfinding desde diferentes posiciones
        int[][] testPositions = {{0,0}, {2,2}, {5,5}, {0,5}};
        
        for (int[] pos : testPositions) {
            int r = pos[0], c = pos[1];
            System.out.println("\n--- Probando desde posición (" + r + "," + c + ") ---");
            
            // Probar con diferentes números de pasos
            for (int steps = 1; steps <= 6; steps++) {
                List<Pos> path = planner.plan(r, c, board, steps);
                System.out.println("Pasos=" + steps + " -> Ruta encontrada: " + 
                    (path.isEmpty() ? "NINGUNA" : path.size() + " posiciones"));
                
                if (!path.isEmpty()) {
                    System.out.print("  Ruta: ");
                    for (Pos p : path) {
                        System.out.print("(" + p.r() + "," + p.c() + ") ");
                    }
                    System.out.println();
                }
            }
        }
        
        // Probar escenario con jugadores ocupando casillas
        System.out.println("\n=== PROBANDO CON CASILLAS OCUPADAS ===");
        
        // Simular jugadores en el tablero
        Player p1 = new Player("Ana", 1, board, 3, 100, 200);
        Player p2 = new Player("Bob", 2, board, 3, 100, 200);
        
        // Colocar jugadores usando el método correcto del board
        board.placePlayerAtRandom(p1);
        board.placePlayerAtRandom(p2);
        
        System.out.println("Jugadores colocados aleatoriamente:");
        System.out.println("Ana en: (" + p1.row() + "," + p1.col() + ")");
        System.out.println("Bob en: (" + p2.row() + "," + p2.col() + ")");
        
        System.out.println("Tablero ocupado:");
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                if (board.isOccupied(i, j)) {
                    System.out.print("X ");
                } else {
                    System.out.print(". ");
                }
            }
            System.out.println();
        }
        
        // Probar pathfinding desde diferentes posiciones con obstáculos
        System.out.println("\n=== PATHFINDING CON OBSTÁCULOS ===");
        for (int[] pos : testPositions) {
            int r = pos[0], c = pos[1];
            System.out.println("\nPathfinding desde (" + r + "," + c + "):");
            List<Pos> path = planner.plan(r, c, board, 3);
            if (path.isEmpty()) {
                System.out.println("  ❌ NO SE ENCONTRÓ RUTA - Posible problema!");
                diagnosticPlan(r, c, board, 3);
            } else {
                System.out.println("  ✅ Ruta encontrada: " + path.size() + " pasos");
            }
        }
    }
    
    static void diagnosticPlan(int r, int c, Board board, int steps) {
        System.out.println("    Análisis detallado:");
        System.out.println("    Posición inicial: (" + r + "," + c + ")");
        System.out.println("    Pasos disponibles: " + steps);
        
        int[][] DIRS = { {-1,0},{0,1},{1,0},{0,-1} };
        
        // Verificar si la posición inicial está ocupada
        if (board.isOccupied(r, c)) {
            System.out.println("    ⚠️ POSICIÓN INICIAL OCUPADA!");
            return;
        }
        
        // Verificar casillas adyacentes
        System.out.println("    Casillas adyacentes:");
        int freeAdjacent = 0;
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < board.rows() && nc >= 0 && nc < board.cols()) {
                boolean occupied = board.isOccupied(nr, nc);
                var content = board.cellContent(nr, nc);
                System.out.println("      (" + nr + "," + nc + ") -> Ocupada: " + occupied + 
                    ", Contenido: " + content);
                if (!occupied) freeAdjacent++;
            } else {
                System.out.println("      (" + nr + "," + nc + ") -> FUERA DE LÍMITES");
            }
        }
        
        if (freeAdjacent == 0) {
            System.out.println("    ❌ TODAS LAS CASILLAS ADYACENTES ESTÁN OCUPADAS!");
        } else {
            System.out.println("    ✅ " + freeAdjacent + " casillas adyacentes libres");
        }
    }
}