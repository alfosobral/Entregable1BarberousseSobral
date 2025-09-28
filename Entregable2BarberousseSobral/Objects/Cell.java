package Objects;

import java.util.concurrent.locks.ReentrantLock;

public class Cell {
	private final int row, col;
	private final ReentrantLock lock = new ReentrantLock();
	private volatile Booster content = Booster.NONE;
	private volatile Player player = null;

	public Cell(int row, int col) {
		this.row = row;
		this.col = col;
	}

	public int row() { return row; }
	public int col() {return col; }
    public ReentrantLock lock() { return lock; }

	public Booster getContentUnsafe() {
		return content;
	}

	public void setContentUnsafe(Booster content) {
		this.content = content;
	}

	public boolean hasPlayer() {
		return !(this.player == null);
	}

	public void setPlayer(Player player) {
		this.player = player;
	}
}
