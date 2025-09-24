public class Robot implements Runnable {
    private final Board board;
    private final Booster type;
    private final double maxFill;
    private final long Tmin, Tmax;

    public Robot(Board b, Booster t, double m, long tmin, long tmax) {
        this.board = b;
        this.type = t;
        this.maxFill = m;
        this.Tmin = tmin;
        this.Tmax = tmax;
    }

    @Override
    public void run() {
        var rnd = java.util.concurrent.ThreadLocalRandom.current();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(rnd.nextLong(Tmin, Tmax));
                int target = (int)Math.floor(board.getTotal() * maxFill);
                int deficit = target - (int)board.count(type);

                if (deficit <= 0) continue;

                int maxAttempts = deficit * 8;
                int filled = 0;

                for (int i = 0; i < maxAttempts && filled < deficit; i++) {
                    int[] rc = board.randomCoord();
                    if (board.fillIfEmpty(rc[0], rc[1], type)) {
                        filled++;
                    }
                }


            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}