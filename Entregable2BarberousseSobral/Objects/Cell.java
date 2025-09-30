package Objects;

import java.util.concurrent.locks.ReentrantLock;

public class Cell {
    private final int row, col;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile Booster content = Booster.NONE;
    private volatile int coinAmount = 0;
    private volatile Player player = null;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int row() { return row; }
    public int col() { return col; }
    public ReentrantLock lock() { return lock; }

    public Booster content() { return content; }
    public int coinAmount() { return coinAmount; }

    public boolean hasPlayer() { return player != null; }
    public Player player() { return player; }

    public void setPlayer(Player p) { this.player = p; }
    public void clearPlayer() { this.player = null; }

    public void setCoin(int amount) {
        this.content = Booster.COIN;
        this.coinAmount = amount;
    }

    public void setHeal() {
        this.content = Booster.HEAL;
        this.coinAmount = 0;
    }

    public void setPoison() {
        this.content = Booster.POISON;
        this.coinAmount = 0;
    }

    public void clearContent() {
        this.content = Booster.NONE;
        this.coinAmount = 0;
    }
}
