import java.util.Random;

public class CoinsRobot extends Thread {
    private final int maxCoins;
    private final int Ymin, Ymax;
    private final int[] coins;

    public CoinsRobot(int maxCoins, int Ymin, int Ymax, int[] coins) {
        this.maxCoins = maxCoins;
        this.Ymin = Ymin;
        this.Ymax = Ymax;
        this.coins = coins;
    }

    public int getCoin() {
        Random rand = new Random();
        int randomIndex = rand.nextInt(coins.length);
        return coins[randomIndex];
    }

    @Override
    public void run() {
        
    }


}
