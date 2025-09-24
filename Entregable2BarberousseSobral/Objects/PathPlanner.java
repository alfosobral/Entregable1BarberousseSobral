import java.util.*;

public class PathPlanner implements PathPlannerInterface {
    
    private static final int[][] DIRECTIONS = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
    
    /**
     * Implementación del método de la interfaz PathPlannerInterface
     * Retorna una lista de posiciones en lugar de IDs
     */
    @Override
    public List<Pos> plan(int r, int c, Board tablero, int dice) {
        int[] pathIds = planBestPath(r, c, tablero, dice);
        List<Pos> positions = new ArrayList<>();
        
        for (int id : pathIds) {
            positions.add(tablero.getPosFromId(id));
        }
        
        return positions;
    }
    
    /**
     * Encuentra la mejor ruta para el jugador basada en su posición actual,
     * el estado del tablero y el número de dados (pasos disponibles)
     * 
     * @param currentRow Fila actual del jugador
     * @param currentCol Columna actual del jugador
     * @param board Tablero con todos los boosters
     * @param diceValue Valor del dado (número de pasos que puede dar)
     * @return Array con los IDs de las casillas que visitará
     */
    public int[] planBestPath(int currentRow, int currentCol, Board board, int diceValue) {
        if (diceValue <= 0) {
            return new int[0]; 
        }
        
        // Estructura para mantener el estado de búsqueda
        class PathState {
            int row, col, stepsLeft, totalScore;
            List<int[]> path; // Lista de posiciones [row, col]
            
            PathState(int row, int col, int stepsLeft, int totalScore, List<int[]> path) {
                this.row = row;
                this.col = col;
                this.stepsLeft = stepsLeft;
                this.totalScore = totalScore;
                this.path = new ArrayList<>(path);
            }
        }
        
        // Cola de prioridad para explorar mejores rutas primero
        PriorityQueue<PathState> queue = new PriorityQueue<>((a, b) -> b.totalScore - a.totalScore);
        
        // Estado inicial
        List<int[]> initialPath = new ArrayList<>();
        initialPath.add(new int[]{currentRow, currentCol});
        queue.add(new PathState(currentRow, currentCol, diceValue, 0, initialPath));
        
        PathState bestPath = null;
        int bestScore = Integer.MIN_VALUE;
        
        // Exploración de todas las rutas posibles
        while (!queue.isEmpty()) {
            PathState current = queue.poll();
            // Si se agotaron los pasos, evaluar esta ruta
            if (current.stepsLeft == 0) {
                if (current.totalScore > bestScore) {
                    bestScore = current.totalScore;
                    bestPath = current;
                }
                continue;
            }
            
            // Explorar todas las direcciones posibles
            for (int[] dir : DIRECTIONS) {
                int newRow = current.row + dir[0];
                int newCol = current.col + dir[1];
                // Verificar que la nueva posición esté dentro del tablero
                if (isValidPosition(newRow, newCol, board)) {
                    int cellScore = calculateCellScore(board.getCell(newRow, newCol));
                    // Crear nueva ruta
                    List<int[]> newPath = new ArrayList<>(current.path);
                    newPath.add(new int[]{newRow, newCol});
                    // Agregar nuevo estado a la cola
                    queue.add(new PathState(
                        newRow, newCol, 
                        current.stepsLeft - 1, 
                        current.totalScore + cellScore,
                        newPath
                    ));
                }
            }
        }
        
        // Si no se encontró ninguna ruta (caso extremo), moverse aleatoriamente
        if (bestPath == null) {
            return planRandomPath(currentRow, currentCol, board, diceValue);
        }
        
        // Convertir la mejor ruta a array de IDs de casillas
        return convertPathToValues(bestPath.path, board);
    }
    
    /**
     * Plan de respaldo: movimiento aleatorio cuando no hay rutas óptimas
     */
    private int[] planRandomPath(int currentRow, int currentCol, Board board, int diceValue) {
        List<int[]> randomPath = new ArrayList<>();
        randomPath.add(new int[]{currentRow, currentCol});
        Random random = new Random();
        int row = currentRow, col = currentCol;
        
        for (int step = 0; step < diceValue; step++) {
            // Obtener direcciones válidas
            List<int[]> validMoves = new ArrayList<>();
            for (int[] dir : DIRECTIONS) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (isValidPosition(newRow, newCol, board)) {
                    validMoves.add(new int[]{newRow, newCol});
                }
            }
            
            // Si hay movimientos válidos, elegir uno al azar
            if (!validMoves.isEmpty()) {
                int[] nextMove = validMoves.get(random.nextInt(validMoves.size()));
                row = nextMove[0];
                col = nextMove[1];
                randomPath.add(new int[]{row, col});
            } else {
                // Si no hay movimientos válidos, quedarse en el mismo lugar
                randomPath.add(new int[]{row, col});
            }
        }
        return convertPathToValues(randomPath, board);
    }
    
    /**
     * Convierte una lista de posiciones en un array de IDs de las casillas
     * Usando board.id() para obtener el ID numérico de cada posición
     */
    private int[] convertPathToValues(List<int[]> path, Board board) {
        int[] values = new int[path.size()];
        for (int i = 0; i < path.size(); i++) {
            int[] pos = path.get(i);
            values[i] = board.id(pos[0], pos[1]); // Usar ID en lugar de puntuación
        }
        return values;
    }
    
    /**
     * Verifica si una posición es válida dentro del tablero
     */
    private boolean isValidPosition(int row, int col, Board board) {
        return board.inRange(row, col); // Usar método del Board
    }
    
    /**
     * Calcula la puntuación de una celda basada en su contenido
     */
    private int calculateCellScore(Cell cell) {
        Booster content = cell.getContentUnsafe();
        
        return switch(content) {
            case HEAL -> 15;      // Alta prioridad para curación
            case COIN -> 10;      // Buena prioridad para monedas
            case POISON -> -25;   // Evitar veneno fuertemente
            case NONE -> 1;       // Movimiento neutral tiene valor mínimo
        };
    }
    
    /**
     * Obtiene la siguiente posición recomendada (primer paso de la mejor ruta)
     * @return Array [row, col] con la siguiente posición
     */
    public int[] getNextPosition(int currentRow, int currentCol, Board board, int diceValue) {
        // Encuentra la mejor ruta completa
        class PathState {
            int row, col, stepsLeft, totalScore;
            List<int[]> path;
            
            PathState(int row, int col, int stepsLeft, int totalScore, List<int[]> path) {
                this.row = row;
                this.col = col;
                this.stepsLeft = stepsLeft;
                this.totalScore = totalScore;
                this.path = new ArrayList<>(path);
            }
        }
        
        PriorityQueue<PathState> queue = new PriorityQueue<>((a, b) -> b.totalScore - a.totalScore);
        List<int[]> initialPath = new ArrayList<>();
        initialPath.add(new int[]{currentRow, currentCol});
        queue.add(new PathState(currentRow, currentCol, diceValue, 0, initialPath));
        
        PathState bestPath = null;
        int bestScore = Integer.MIN_VALUE;
        
        while (!queue.isEmpty()) {
            PathState current = queue.poll();
            
            if (current.stepsLeft == 0) {
                if (current.totalScore > bestScore) {
                    bestScore = current.totalScore;
                    bestPath = current;
                }
                continue;
            }
            
            for (int[] dir : DIRECTIONS) {
                int newRow = current.row + dir[0];
                int newCol = current.col + dir[1];   
                
                if (isValidPosition(newRow, newCol, board)) {
                    int cellScore = calculateCellScore(board.getCell(newRow, newCol));
                    List<int[]> newPath = new ArrayList<>(current.path);
                    newPath.add(new int[]{newRow, newCol});
                    
                    queue.add(new PathState(newRow, newCol, current.stepsLeft - 1, 
                                          current.totalScore + cellScore, newPath));
                }
            }
        }
        
        // Devolver la siguiente posición de la mejor ruta encontrada
        if (bestPath != null && bestPath.path.size() >= 2) {
            return bestPath.path.get(1); // Primera posición después de la actual
        }
        
        // Fallback: quedarse en el mismo lugar
        return new int[]{currentRow, currentCol};
    }
    
    /**
     * Convierte un array de IDs de vuelta a posiciones usando board.getPosFromId()
     * Útil para debugging o visualización
     */
    public Pos[] convertIdsToPositions(int[] pathIds, Board board) {
        Pos[] positions = new Pos[pathIds.length];
        for (int i = 0; i < pathIds.length; i++) {
            positions[i] = board.getPosFromId(pathIds[i]);
        }
        return positions;
    }
    
    /**
     * Método de utilidad para obtener el ID de una posición específica
     */
    public int getPositionId(int row, int col, Board board) {
        return board.id(row, col);
    }
}