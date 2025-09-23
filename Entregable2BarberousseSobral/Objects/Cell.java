import java.util.concurrent.locks.ReentrantLock;

public class Cell {
	private final int row, col;
	private final ReentrantLock lock = new ReentrantLock();
	private Booster content = Booster.NONE;

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
}
