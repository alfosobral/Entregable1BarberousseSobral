package Objects;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Board {
    private final Cell[][] grid;
    private final int rows, cols, total;

    private final AtomicInteger coinCells = new AtomicInteger(0);
    private final AtomicInteger healItems = new AtomicInteger(0);
    private final AtomicInteger traps = new AtomicInteger(0);

    private volatile int targetCoinCells;
    private volatile int targetTraps;
    private volatile int targetHeals;

    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.total = rows * cols;
        this.grid = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell(r, c);
            }
        }
        this.targetCoinCells = Math.max(1, (int)Math.floor(total * 0.10));
        this.targetTraps = Math.max(1, (int)Math.floor(total * 0.10));
        this.targetHeals = 0;
    }

    public int rows() { return rows; }
    public int cols() { return cols; }
    public int total() { return total; }

    public void setTargetHeals(int x) { this.targetHeals = Math.max(0, x); }
    public int getTargetHeals() { return targetHeals; }
    public int getTargetCoinCells() { return targetCoinCells; }
    public int getTargetTraps() { return targetTraps; }

    public Pos randomFreeCell() {
        for (int tries = 0; tries < total * 3; tries++) {
            int r = ThreadLocalRandom.current().nextInt(rows);
            int c = ThreadLocalRandom.current().nextInt(cols);
            Cell cell = grid[r][c];
            if (cell.lock().tryLock()) {
                try {
                    if (!cell.hasPlayer() && cell.content() == Booster.NONE) {
                        return new Pos(r, c);
                    }
                } finally {
                    cell.lock().unlock();
                }
            }
        }
        return null;
    }

    public boolean tryPlaceCoin(int amount) {
        if (coinCells.get() >= targetCoinCells) return false;
        Pos p = randomFreeCell();
        if (p == null) return false;
        Cell cell = grid[p.r()][p.c()];
        cell.lock().lock();
        try {
            if (!cell.hasPlayer() && cell.content() == Booster.NONE) {
                cell.setCoin(amount);
                coinCells.incrementAndGet();
                return true;
            }
        } finally {
            cell.lock().unlock();
        }
        return false;
    }

    public boolean tryPlaceHeal() {
        if (healItems.get() >= targetHeals) return false;
        Pos p = randomFreeCell();
        if (p == null) return false;
        Cell cell = grid[p.r()][p.c()];
        cell.lock().lock();
        try {
            if (!cell.hasPlayer() && cell.content() == Booster.NONE) {
                cell.setHeal();
                healItems.incrementAndGet();
                return true;
            }
        } finally {
            cell.lock().unlock();
        }
        return false;
    }

    public boolean tryPlaceTrap() {
        if (traps.get() >= targetTraps) return false;
        Pos p = randomFreeCell();
        if (p == null) return false;
        Cell cell = grid[p.r()][p.c()];
        cell.lock().lock();
        try {
            if (!cell.hasPlayer() && cell.content() == Booster.NONE) {
                cell.setPoison();
                traps.incrementAndGet();
                return true;
            }
        } finally {
            cell.lock().unlock();
        }
        return false;
    }

    public Booster movePlayerSafe(int r, int c, int nr, int nc, Player p) {
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) return Booster.NONE;
        
        if (r == nr && c == nc) return Booster.NONE;
        
        Cell from = grid[r][c];
        Cell to = grid[nr][nc];

        Cell first = from.row() * cols + from.col() <= to.row() * cols + to.col() ? from : to;
        Cell second = first == from ? to : from;

        first.lock().lock();
        second.lock().lock();
        try {
            if (to.hasPlayer()) return Booster.NONE;
            
            if (from.player() != p) return Booster.NONE;
            
            from.clearPlayer();
            
            to.setPlayer(p);
            p.setPos(nr, nc);

            Booster b = to.content();
            int coinAmount = to.coinAmount();
            if (b != Booster.NONE) {
                to.clearContent();
                if (b == Booster.COIN) coinCells.decrementAndGet();
                else if (b == Booster.HEAL) healItems.decrementAndGet();
                else if (b == Booster.POISON) traps.decrementAndGet();
            }
            if (b == Booster.COIN && coinAmount > 0) {
                p.addCoins(coinAmount);
            }
            return b;
        } finally {
            second.lock().unlock();
            first.lock().unlock();
        }
    }

    public void placePlayerAtRandom(Player p) {
        while (true) {
            int r = ThreadLocalRandom.current().nextInt(rows);
            int c = ThreadLocalRandom.current().nextInt(cols);
            Cell cell = grid[r][c];
            if (cell.lock().tryLock()) {
                try {
                    if (!cell.hasPlayer()) {
                        cell.setPlayer(p);
                        p.setPos(r, c);
                        return;
                    }
                } finally {
                    cell.lock().unlock();
                }
            }
        }
    }

    public boolean isOccupied(int r, int c) {
        return grid[r][c].hasPlayer();
    }
    
    public void clearAllPlayerPositions(Player p) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = grid[r][c];
                cell.lock().lock();
                try {
                    if (cell.player() == p) {
                        cell.clearPlayer();
                    }
                } finally {
                    cell.lock().unlock();
                }
            }
        }
    }

    public Booster cellContent(int r, int c) {
        return grid[r][c].content();
    }

    public int cellCoinAmount(int r, int c) {
        return grid[r][c].coinAmount();
    }

    public String[][] snapshotWithColors() {
        String[][] s = new String[rows][cols];
        for (int i=0;i<rows;i++) {
            for (int j=0;j<cols;j++) {
                s[i][j] = ".";
            }
        }
        for (int i=0;i<rows;i++) {
            for (int j=0;j<cols;j++) {
                Cell cell = grid[i][j];
                cell.lock().lock();
                try {
                    if (cell.hasPlayer()) {
                        Player p = cell.player();
                        String initials = p.getInitials();
                        String color = p.getColorCode();
                        String reset = Player.getResetColor();
                        s[i][j] = color + initials + reset;
                    } else {
                        Booster b = cell.content();
                        if (b == Booster.HEAL) s[i][j] = "+";
                        else if (b == Booster.COIN) s[i][j] = "$";
                    }
                } finally {
                    cell.lock().unlock();
                }
            }
        }
        return s;
    }
    
}
