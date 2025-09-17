import java.util.concurrent.atomic.AtomicBoolean;

public class Cell {
	private boolean healing;
    private boolean poison;
	private int coins;
	private AtomicBoolean locked;

	public Cell() {
		this.healing = false;
        this.poison = false;
		this.coins = 0;
		this.locked = new AtomicBoolean(false);
	}

	public boolean isHealed() {
		return healing;
	}

    public boolean isPoisoned(){
        return poison;
    }

	public int getCoins() {
		return coins;
	}

	public boolean isLocked() {
		return locked.get();
	}

	public void setHealing(boolean healing) {
		this.healing = healing;
	}

	public void setPoisoned(boolean poison) {
		this.poison = poison;
	}

	public void setCoins(int coins) {
		this.coins = coins;
	}

	public void lock() {
		this.locked.set(true);
	}

	public void unlock() {
		this.locked.set(false);
	}
}
