import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import Objects.*;

public class DiagnoseStuckPlayers {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== DIAGNÓSTICO DE JUGADORES ESTÁTICOS ===");
        
        // Configuración de prueba
        Board board = new Board(6, 6);
        board.setTargetHeals(8);
        
        String[] names = {"Ana", "Bob", "Carlos", "Diana"};
        Player[] players = new Player[4];
        Thread[] threads = new Thread[4];
        
        // Configuración similar al juego real
        long zmin = 350, zmax = 550;
        
        // Crear jugadores
        for (int i = 0; i < 4; i++) {
            players[i] = new Player(names[i], i + 1, board, 3, zmin, zmax);
            threads[i] = new Thread(players[i], names[i]);
        }
        
        // Robots para generar ítems
        Thread coinBot = new Thread(new Robot(board, Booster.COIN, 200, 400), "RobotMonedas");
        Thread healBot = new Thread(new Robot(board, Booster.HEAL, 200, 400), "RobotVidas");
        Thread badBot = new Thread(new Robot(board, Booster.POISON, 800, 1500), "RobotMalo");
        
        // Iniciar todos los hilos
        for (Thread t : threads) t.start();
        coinBot.start();
        healBot.start();
        badBot.start();
        
        // Monitor de actividad de jugadores
        Map<String, Integer> lastCoins = new HashMap<>();
        Map<String, Pos> lastPositions = new HashMap<>();
        Map<String, Integer> stuckCounter = new HashMap<>();
        
        for (int i = 0; i < 4; i++) {
            lastCoins.put(names[i], 0);
            lastPositions.put(names[i], null);
            stuckCounter.put(names[i], 0);
        }
        
        System.out.println("Monitoreando actividad de jugadores...\n");
        
        // Monitorear por 30 segundos
        for (int second = 1; second <= 30; second++) {
            Thread.sleep(1000);
            
            System.out.printf("=== SEGUNDO %d ===\n", second);
            
            boolean anyMovement = false;
            
            for (int i = 0; i < 4; i++) {
                String name = names[i];
                Player p = players[i];
                
                if (p.lifes() <= 0) {
                    System.out.printf("%s: MUERTO (vidas=%d)\n", name, p.lifes());
                    continue;
                }
                
                Pos currentPos = new Pos(p.row(), p.col());
                int currentCoins = p.coins();
                boolean moved = false;
                boolean gainedCoins = false;
                
                // Verificar movimiento
                if (lastPositions.get(name) != null && !lastPositions.get(name).equals(currentPos)) {
                    moved = true;
                    anyMovement = true;
                }
                
                // Verificar ganancia de monedas
                if (currentCoins > lastCoins.get(name)) {
                    gainedCoins = true;
                    anyMovement = true;
                }
                
                // Actualizar contadores de inactividad
                if (!moved && !gainedCoins) {
                    stuckCounter.put(name, stuckCounter.get(name) + 1);
                } else {
                    stuckCounter.put(name, 0);
                }
                
                // Mostrar estado
                String status = "";
                if (moved) status += "MOVIÓ ";
                if (gainedCoins) status += "MONEDAS ";
                if (status.isEmpty()) status = "QUIETO";
                
                System.out.printf("%s: %s | Pos=%s | Monedas=%d | Vidas=%d | Quieto por %d seg\n", 
                    name, status, currentPos, currentCoins, p.lifes(), stuckCounter.get(name));
                
                // Actualizar estados anteriores
                lastPositions.put(name, currentPos);
                lastCoins.put(name, currentCoins);
            }
            
            if (!anyMovement) {
                System.out.println("⚠️  NINGÚN JUGADOR SE MOVIÓ EN ESTE SEGUNDO!");
            }
            
            // Mostrar tablero cada 5 segundos
            if (second % 5 == 0) {
                System.out.println("\nTABLERO ACTUAL:");
                System.out.println(board.snapshotWithColors());
            }
            
            System.out.println();
        }
        
        // Detener todo
        System.out.println("=== ANÁLISIS FINAL ===");
        for (int i = 0; i < 4; i++) {
            players[i].stopGracefully();
        }
        
        // Esperar a que terminen
        for (Thread t : threads) {
            try { t.join(1000); } catch (InterruptedException e) {}
        }
        
        // Resultados finales
        System.out.println("\n=== RESULTADOS FINALES ===");
        for (int i = 0; i < 4; i++) {
            String name = names[i];
            Player p = players[i];
            System.out.printf("%s: monedas=%d | vidas=%d | quieto_final=%d seg | vivo=%s\n", 
                name, p.coins(), p.lifes(), stuckCounter.get(name), (p.lifes() > 0));
        }
        
        System.out.println("\n=== DIAGNÓSTICO ===");
        for (String name : names) {
            if (stuckCounter.get(name) >= 10) {
                System.out.printf("⚠️  %s estuvo BLOQUEADO por %d segundos consecutivos\n", 
                    name, stuckCounter.get(name));
            }
        }
    }
}