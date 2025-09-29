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

                // If target already reached for this type, we just exit
                if (!placed) {
                    // Check if we hit target and cannot place more
                    Thread.sleep(200);
                    boolean done = switch (type) {
                        case COIN -> false; // may still be under target later if others pick coins (but requirement asks to place set once; keep simple: once full, we stop)
                        case HEAL -> true;  // stop when target reached or no space right now
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
