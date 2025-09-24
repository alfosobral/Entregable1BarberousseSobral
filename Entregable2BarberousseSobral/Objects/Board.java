import java.util.concurrent.atomic.LongAdder;

public class Board {
    private final Cell[][] grid;
    private final int rows, cols, total;

    private final LongAdder coins = new LongAdder();
    private final LongAdder healing = new LongAdder();
    private final LongAdder poison = new LongAdder();
    

    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.total = rows * cols;
        this.grid = new Cell[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                this.grid[r][c] = new Cell(r, c);
            }
        }
    }

    public int getTotal() {
        return total;
    }

    public boolean inRange(int r, int c) { 
        return r>=0 && r<rows && c>=0 && c<cols; 
    }

    public int id(int r, int c) { 
        return r * cols + c; 
    }

    public Pos getPosFromId(int id) {
    if (id < 0 || id >= total) {
        throw new IllegalArgumentException(
            "id fuera de rango: " + id + " (válido: 0.." + (total - 1) + ")"
        );
    }
    int r = id / cols;
    int c = id % cols;
    return new Pos(r, c);
}

    public Cell getCell(int x, int y) {
        return grid[x][y];
    }
    
   public long count(Booster b) {
    return switch(b) {
        case COIN -> coins.sum();
        case HEAL -> healing.sum();
        case POISON -> poison.sum();
        case NONE -> total - (coins.sum() + healing.sum() + poison.sum());
    };
   }

   public void inc(Booster b) {
    switch(b) {
        case COIN -> coins.increment();
        case HEAL -> healing.increment();
        case POISON -> poison.increment();
        default -> {}
    };
   }

   public void dec(Booster b) {
    switch(b) {
        case COIN -> coins.decrement();
        case HEAL -> healing.decrement();
        case POISON -> poison.decrement();
        default -> {}
    };
   }

   public Booster consumeAt(int r, int c) {
    Cell x = grid[r][c];
    x.lock().lock();
    try {
        Booster boost = x.getContentUnsafe();
        if (boost != Booster.NONE) {
            x.setContentUnsafe(Booster.NONE);
            dec(boost);
        }
        return boost;
    } finally {
        x.lock().unlock();
    }
   }

   public Booster movePlayerSafe(int r1, int c1, int r2, int c2) {
    Cell a = grid[r1][c1], b = grid[r2][c2];
    Cell first = (id(a.row(),a.col()) <= id(b.row(),b.col())) ? a : b;
    Cell second = (first == a) ? b : a;

    first.lock().lock();
    second.lock().lock();

    try {
        Booster consumed = b.getContentUnsafe();
        if (consumed != Booster.NONE) {
            b.setContentUnsafe(Booster.NONE);
            dec(consumed);
        }
        return consumed;
    } finally {
        second.lock().unlock();
        first.lock().unlock();
    }

   }

   public boolean fillIfEmpty(int r, int c, Booster b) {
    if (b == Booster.NONE) {
        return false;
    }

    Cell x = grid[r][c];
    if (x.lock().tryLock()) {
        try {
            if (x.getContentUnsafe() == Booster.NONE) {
                x.setContentUnsafe(b);
                inc(b);
                return true;
            }
            return false;
        } finally {
            x.lock().unlock();
        }
    }
    return false;
   }

   public int[] randomCoord() {
        int r = java.util.concurrent.ThreadLocalRandom.current().nextInt(rows);
        int c = java.util.concurrent.ThreadLocalRandom.current().nextInt(cols);
        return new int[]{r,c};
    }
}