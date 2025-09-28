import Objects.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Main {
    
    private static final int N = 10;

    private static final int NUM_PLAYERS = 4;
    private static final int INITIAL_LIVES = 3;

    private static final long Z_MIN = 120;
    private static final long Z_MAX = 300;

    private static final int COIN_ROBOTS = 1;
    private static final int HEAL_ROBOTS   = 1;
    private static final int POISON_ROBOTS = 1;

    private static final double MAX_HEALS = 0.10;
    private static final long X_MIN = 150;
    private static final long X_MAX = 350;

    private static final double MAX_COINS = 0.10;
    private static final long Y_MIN = 150;
    private static final long Y_MAX = 350;

    private static final double MAX_POISON = 0.10;
    private static final long W_MIN = 150;
    private static final long W_MAX = 350;

     private static final long DURATION_MS = TimeUnit.SECONDS.toMillis(30);

     public static void main(String[] args) throws InterruptedException {
        
        Board board = new Board(N, N);

        Random rng = new Random();
        AtomicBoolean running = new AtomicBoolean(true);

        Consumer<Player> onDeath = (p) -> {
            System.out.println("[DEAD] " + Thread.currentThread().getName() + " notificó muerte de: " + p);
        };

        PathPlanner planner = new PathPlanner();
        
        List<Player> players = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < NUM_PLAYERS; i++) {
            String name = "P" + (i + 1);
            int ir = i / N;
            int ic = i % N;

            Player p = new Player(
                name,
                board,
                ir,
                ic,
                INITIAL_LIVES, 
                X_MIN,
                X_MAX,
                planner, 
                onDeath
            );

            players.add(p);
            threads.add(new Thread(p, "PLAYER-" + name));
        }

        for (int i = 0; i < COIN_ROBOTS; i++)
            threads.add(new Thread(new Robot(board, Booster.COIN, MAX_COINS, Y_MIN, Y_MAX), "R-MON-" + i));
        for (int i = 0; i < HEAL_ROBOTS; i++)
            threads.add(new Thread(new Robot(board, Booster.HEAL,   MAX_HEALS, Z_MIN, Z_MAX), "R-VID-" + i));
        for (int i = 0; i < POISON_ROBOTS; i++)
            threads.add(new Thread(new Robot(board, Booster.POISON, MAX_POISON, W_MIN, W_MAX), "R-TRAP-" + i));


        Thread render = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.print("\u001b[H\u001b[2J"); System.out.flush();
                    System.out.println(asciiBoard(board));
                    Thread.sleep(200);
                }
            } catch (InterruptedException ignored) {}
        }, "RENDER");

        render.setDaemon(true);
        render.start();

        threads.forEach(Thread::start);

        Thread.sleep(DURATION_MS);

        threads.forEach(Thread::interrupt);
        for (Thread t : threads) t.join(5000);
        System.out.println("\n=== FIN DE LA PARTIDA ===\n" + asciiBoard(board));

    }

    private static String asciiBoard(Board b) {
    int R = b.getRows(), C = b.getCols(); // cambia a b.getRows()/b.getCols() si los tenés
    StringBuilder sb = new StringBuilder(R * (2*C + 1));
    for (int r = 0; r < R; r++) {
      for (int c = 0; c < C; c++) {
        // reemplazá por tu acceso real a celda:
        Cell cell = b.getCell(r, c);
        char ch = '.';
        // Pseudocódigo: ajusta a tus getters reales y lectura “no bloqueante”
        boolean hasPlayers = cell.hasPlayer();
        Booster boost = cell.getContentUnsafe();
        
        if (hasPlayers && boost != null) ch = '*';
        else if (hasPlayers) ch = 'J';
        else if (boost != null) ch = letter(boost);
        sb.append(ch).append(' ');
      }
      sb.append('\n');
      System.out.println();
    }
    return sb.toString();
  }

  private static char letter(Booster b) {
    switch (b) {
      case COIN: return 'C';
      case HEAL:   return 'H';
      case POISON: return 'P';
      default:     return 'B';
    }
  }

}