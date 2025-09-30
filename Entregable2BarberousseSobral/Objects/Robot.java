package Objects;

import java.util.concurrent.ThreadLocalRandom;

public class Robot implements Runnable {
    private final Board board;
    private final Booster type;
    private final long tmin, tmax;

    public Robot(Board board, Booster type, long tmin, long tmax) {
        this.board = board;
        this.type = type;
        this.tmin = tmin;
        this.tmax = tmax;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                boolean placed = false;
                if (type == Booster.COIN) {
                    int amount = new int[]{1,2,5,10}[ThreadLocalRandom.current().nextInt(4)];
                    placed = board.tryPlaceCoin(amount);
                } else if (type == Booster.HEAL) {
                    placed = board.tryPlaceHeal();
                } else if (type == Booster.POISON) {
                    placed = board.tryPlaceTrap();
                }

                if (!placed) {
                    Thread.sleep(200);
                    boolean done = switch (type) {
                        case COIN -> false;
                        case HEAL -> true;
                        case POISON -> true;
                        default -> true;
                    };
                    if (done) return;
                } else {
                    Thread.sleep(ThreadLocalRandom.current().nextLong(tmin, tmax + 1));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
