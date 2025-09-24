import java.util.function.Consumer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayDeque;

public class Player implements Runnable {
    private final String name;
    private final Board board;

    private final AtomicInteger lifes = new AtomicInteger();
    private final AtomicInteger coins = new AtomicInteger();
    private final long Tmin, Tmax;
    private volatile int r, c;

    private final PathPlanner planner;

    private final Consumer<Player> onDeath;

    private volatile boolean running = true;

    public Player(String n, Board b, int ir, int ic, int il, long tmin, long tmax, PathPlanner p, Consumer<Player> onDeath) {
        this.name = n;
        this.board = b;
        this.lifes.set(il);;
        this.Tmin = tmin;
        this.Tmax = tmax;
        this.r = ir;
        this.c = ic;
        this.planner = p;
        this.onDeath = onDeath;
    }

    @Override
    public void run() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                long waitMs = rnd.nextLong(Tmin, Tmax);
                Thread.sleep(waitMs);

                move();

                while(running && !Thread.currentThread().isInterrupted() && !path.isEmpty()) {
                    Pos next = path.pollFirst();
                    if (next == null) break;

                    if (!board.inRange(next.r(), next.c())) continue;

                    Booster b = board.movePlayerSafe(r, c, next.r(), next.c()); 
                    r = next.r();
                    c = next.c();

                    applyBoost(b);

                    if (lifes.get() <= 0) {
                        deathMessage();
                        return;
                    }


                    }

            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    protected void move() {
        int dice = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 7); // 1..6
        List<Pos> path = (planner != null) ? planner.plan(r, c, board, dice) : java.util.List.of();

        if (path == null || path.size() != dice) {
            throw new IllegalStateException("El planner debe devolver exactamente " + dice + " pasos.");
        }

        ArrayDeque<Pos> cleanPath = new ArrayDeque<>(dice);
        
        for (Pos p : path) {
            if (!board.inRange(p.r(), p.c())) {
                throw new IllegalStateException("Planner devolvió coordenada fuera de rango: " + p);
            }
            cleanPath.add(p);
        }

        rutaPendiente.clear();       // 🔁 reemplaza la ruta del turno anterior
        rutaPendiente.addAll(limpia);
    }

    private void applyBoost(Booster b) {
        switch (b) {
            case COIN -> coins.incrementAndGet();
            case HEAL   -> lifes.incrementAndGet();
            case POISON -> lifes.decrementAndGet();
            default     -> {}
        }
    }

    private void deathMessage() {
        running = false;
        if (onDeath != null) {
            try { onDeath.accept(this); } catch (Exception ignored) {}
        }
    }

    public String name() { return name; }
    public int lifes()     { return lifes.get(); }
    public int coins()   { return coins.get(); }
    public int row()      { return r; }
    public int col()       { return c; }

    public void stopGracefully() { running = false; }
}