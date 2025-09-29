package Objects;

import java.util.*;

public class PathPlanner {
    private static final int[][] DIRS = { {-1,0},{0,1},{1,0},{0,-1} };

    // Simple greedy: BFS up to dice steps, prefer cells with content value and unoccupied
    public List<Pos> plan(int r, int c, Board board, int steps) {
        // Validar coordenadas de entrada
        if (r < 0 || r >= board.rows() || c < 0 || c >= board.cols()) {
            return Collections.emptyList();
        }
        
        record Node(int r,int c,int d,List<Pos> path,int score) {}

        Queue<Node> q = new ArrayDeque<>();
        q.add(new Node(r,c,0,new ArrayList<>(List.of(new Pos(r,c))), 0));

        boolean[][][] vis = new boolean[board.rows()][board.cols()][steps+1];
        vis[r][c][0]=true;

        Node best = null;

        while (!q.isEmpty()) {
            Node cur = q.poll();
            if (cur.d == steps) {
                if (best == null || cur.score > best.score) best = cur;
                continue;
            }
            for (int[] d: DIRS) {
                int nr = cur.r + d[0], nc = cur.c + d[1];
                if (nr<0 || nr>=board.rows() || nc<0 || nc>=board.cols()) continue;
                if (vis[nr][nc][cur.d+1]) continue;
                // Avoid paths that step into occupied cells (can't move into them)
                if (board.isOccupied(nr,nc)) continue;
                
                // Los venenos son invisibles para el pathfinding - no los considera
                var cont = board.cellContent(nr,nc);
                if (cont == Booster.POISON) {
                    // Tratar las casillas de veneno como casillas normales (sin penalización ni bonus)
                    cont = Booster.NONE;
                }
                
                int add = 0;
                if (cont == Booster.COIN) add += Math.min(10, Math.max(1, board.cellCoinAmount(nr,nc)));
                else if (cont == Booster.HEAL) add += 3;
                
                // Sistema de dispersión para evitar clustering
                int playerCount = countPlayersInArea(board, nr, nc, 2);
                
                // Penalizar áreas con muchos jugadores
                if (playerCount > 1) {
                    add -= playerCount * 6; // Penalización más fuerte por clustering
                } else if (playerCount == 0) {
                    add += 12; // Mayor bonus por área libre
                }
                
                // Bonus muy reducido por exploración para evitar dominancia
                int explorationBonus = (Math.abs(nr - r) + Math.abs(nc - c)) / 3;
                add += explorationBonus;
                
                // Mayor factor aleatorio para más variabilidad
                add += (int)(Math.random() * 12);
                
                // Penalización moderada por distancia extrema
                int distance = Math.abs(nr - r) + Math.abs(nc - c);
                if (distance > 4) {
                    add -= distance / 2; // Penalización más suave
                }

                List<Pos> npath = new ArrayList<>(cur.path);
                npath.add(new Pos(nr,nc));
                Node nxt = new Node(nr,nc,cur.d+1,npath, cur.score + add);
                vis[nr][nc][cur.d+1]=true;
                q.add(nxt);
            }
        }

        if (best == null) return Collections.emptyList();
        return best.path;
    }
    
    // Método para contar jugadores en un área específica
    private int countPlayersInArea(Board board, int centerRow, int centerCol, int radius) {
        int count = 0;
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                int r = centerRow + dr;
                int c = centerCol + dc;
                if (r >= 0 && r < board.rows() && c >= 0 && c < board.cols()) {
                    if (board.isOccupied(r, c)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
