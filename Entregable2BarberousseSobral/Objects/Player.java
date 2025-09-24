import java.util.concurrent.atomic.AtomicInteger;

public class Player implements Runnable {
    private final String name;
    private final Board tablero;

    private final AtomicInteger lifes = new AtomicInteger();
    private final AtomicInteger monedas = new AtomicInteger();
    private volatile int r, c;

    private final PathPlanner planner;

    private final Consumer<Player> onDeath;

    private volatile boolean 
}