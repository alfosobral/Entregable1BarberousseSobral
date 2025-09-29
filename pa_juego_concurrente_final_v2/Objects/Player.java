package Objects;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Player implements Runnable {
    private final String name;
    private final Board board;
    private final long zmin, zmax;
    private volatile boolean running = true;

    private final Random rnd = new Random();
    private final PathPlanner planner = new PathPlanner();

    private volatile int r=-1,c=-1;
    private volatile int lives;
    private volatile int coins;

    private final int idDigit;

    public Player(String name, int idDigit, Board board, int initialLives, long zmin, long zmax) {
        this.name = name;
        this.idDigit = idDigit;
        this.board = board;
        this.lives = initialLives;
        this.zmin = zmin;
        this.zmax = zmax;
    }

    public String name() { return name; }
    public int idDigit() { return idDigit; }
    public int lifes() { return lives; }
    public int coins() { return coins; }
    public int row() { return r; }
    public int col() { return c; }
    
    public String getInitials() {
        String[] words = name.trim().split("\\s+");
        if (words.length >= 2) {
            return (words[0].charAt(0) + "" + words[1].charAt(0)).toUpperCase();
        } else if (words[0].length() >= 2) {
            return words[0].substring(0, 2).toUpperCase();
        } else {
            return (words[0] + " ").toUpperCase();
        }
    }
    
    public String getColorCode() {
        String[] colors = {
            "\u001B[31m", // Rojo
            "\u001B[32m", // Verde  
            "\u001B[33m", // Amarillo
            "\u001B[34m", // Azul
            "\u001B[35m", // Magenta
            "\u001B[36m", // Cyan
        };
        return colors[(idDigit - 1) % colors.length];
    }
    
    public static String getResetColor() {
        return "\u001B[0m";
    }

    public void setPos(int r, int c) { this.r=r; this.c=c; }
    public void addCoins(int amt) { this.coins += Math.max(0, amt); }
    public void addLife() { this.lives++; }
    public void loseLife() { this.lives--; }

    @Override
    public void run() {
        try {
            while (running && lives > 0) {
                int dice = 1 + rnd.nextInt(6);

                var path = planner.plan(r, c, board, dice);
                for (int i = 1; running && lives > 0 && i < path.size(); i++) {
                    var step = path.get(i);
                    Booster b = board.movePlayerSafe(r, c, step.r(), step.c(), this);
                    r = step.r(); c = step.c();

                    if (b == Booster.HEAL) {
                        addLife();
                        System.out.printf("+++ %s recogió una vida (vidas=%d) %n", name, lives);
                    } else if (b == Booster.POISON) {
                        loseLife();
                        System.out.printf("--- %s pisó una trampa (vidas=%d) %n", name, lives);
                        if (lives <= 0) break;
                    } else if (b == Booster.COIN) {
                        System.out.printf("$$$ %s juntó monedas (total=%d) %n", name, coins);
                    }

                    Thread.sleep(40);
                }

                if (lives <= 0) break;

                long nap = ThreadLocalRandom.current().nextLong(zmin, zmax + 1);
                Thread.sleep(nap);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running = false;
            clearFromBoard();
            if (lives <= 0) {
                System.out.printf("XXX %s murió%n", name);
            }
        }
    }

    public void stopGracefully() { 
        running = false; 
    }

    public void clearFromBoard() {
        board.clearAllPlayerPositions(this);
        if (!running) {
            r = -1;
            c = -1;
        }
    }
}
