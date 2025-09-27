import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    private static final int MAX_ACTIVE_PLAYERS = 4;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== BIENVENIDO AL JUEGO CONCURRENTE ===");
        System.out.println("Configuración inicial del juego\n");
        
        // ===== CONFIGURACIÓN DEL TABLERO =====
        System.out.print("Ingrese el tamaño del tablero (NxN): ");
        int boardSize = validatePositiveInput(scanner, "Tamaño del tablero");
        
        // ===== CONFIGURACIÓN DE JUGADORES =====
        System.out.print("Ingrese la cantidad total de jugadores: ");
        int totalPlayers = validatePositiveInput(scanner, "Cantidad de jugadores");
        
        if (totalPlayers > MAX_ACTIVE_PLAYERS) {
            System.out.println("⚠️  Se permiten máximo " + MAX_ACTIVE_PLAYERS + " jugadores activos simultáneamente.");
            System.out.println("   Los jugadores restantes (" + (totalPlayers - MAX_ACTIVE_PLAYERS) + ") quedarán en cola de espera.");
        }
        
        // ===== CONFIGURACIÓN DE TIEMPOS DE PARTIDA =====
        System.out.print("Ingrese el tiempo total de la partida (segundos): ");
        int gameTimeSeconds = validatePositiveInput(scanner, "Tiempo de partida");
        
        // ===== CONFIGURACIÓN DE TIEMPOS DE JUGADORES =====
        System.out.println("\n--- Configuración de tiempos de jugadores ---");
        System.out.print("Tiempo mínimo entre turnos de jugadores (ms): ");
        int playerTurnMin = validatePositiveInput(scanner, "Tiempo mínimo jugadores");
        
        System.out.print("Tiempo máximo entre turnos de jugadores (ms): ");
        int playerTurnMax = validateMinMaxInput(scanner, playerTurnMin, "Tiempo máximo jugadores");
        
        // ===== CONFIGURACIÓN DE ROBOTS DE VIDA =====
        System.out.println("\n--- Configuración de robots de vida (buenos) ---");
        System.out.print("Cantidad máxima de vidas que puede poner el robot de vida: ");
        int maxLifeBonuses = validatePositiveInput(scanner, "Cantidad máxima de vidas");
        
        System.out.print("Tiempo mínimo entre acciones del robot de vida (ms): ");
        int lifeRobotMin = validatePositiveInput(scanner, "Tiempo mínimo robot vida");
        
        System.out.print("Tiempo máximo entre acciones del robot de vida (ms): ");
        int lifeRobotMax = validateMinMaxInput(scanner, lifeRobotMin, "Tiempo máximo robot vida");
        
        // ===== CONFIGURACIÓN DE ROBOTS DE MONEDAS =====
        System.out.println("\n--- Configuración de robots de monedas (buenos) ---");
        System.out.print("Tiempo mínimo entre acciones del robot de monedas (ms): ");
        int coinRobotMin = validatePositiveInput(scanner, "Tiempo mínimo robot monedas");
        
        System.out.print("Tiempo máximo entre acciones del robot de monedas (ms): ");
        int coinRobotMax = validateMinMaxInput(scanner, coinRobotMin, "Tiempo máximo robot monedas");
        
        // ===== CONFIGURACIÓN DE ROBOTS MALVADOS =====
        System.out.println("\n--- Configuración de robots malvados ---");
        System.out.print("Tiempo mínimo entre acciones del robot malvado (ms): ");
        int evilRobotMin = validatePositiveInput(scanner, "Tiempo mínimo robot malvado");
        
        System.out.print("Tiempo máximo entre acciones del robot malvado (ms): ");
        int evilRobotMax = validateMinMaxInput(scanner, evilRobotMin, "Tiempo máximo robot malvado");
        
        scanner.close();
        
        // ===== MOSTRAR RESUMEN DE CONFIGURACIÓN =====
        showGameConfiguration(boardSize, totalPlayers, gameTimeSeconds, 
                            playerTurnMin, playerTurnMax, maxLifeBonuses,
                            lifeRobotMin, lifeRobotMax, coinRobotMin, coinRobotMax,
                            evilRobotMin, evilRobotMax);
        
        System.out.println("✅ Configuración completada exitosamente.");
        System.out.println("💡 El juego está listo para iniciarse con estos parámetros.");
        
        // ===== AQUÍ PUEDES AGREGAR LA LÓGICA DE INICIO CUANDO LA NECESITES =====
        // startGame(boardSize, totalPlayers, gameTimeSeconds,
        //          playerTurnMin, playerTurnMax, maxLifeBonuses,
        //          lifeRobotMin, lifeRobotMax, coinRobotMin, coinRobotMax,
        //          evilRobotMin, evilRobotMax);
    }
    
    /**
     * Valida que el input sea un número positivo
     */
    private static int validatePositiveInput(Scanner scanner, String fieldName) {
        int value;
        while (true) {
            try {
                value = scanner.nextInt();
                if (value > 0) {
                    break;
                } else {
                    System.out.print("❌ " + fieldName + " debe ser mayor a 0. Intente nuevamente: ");
                }
            } catch (InputMismatchException e) {
                System.out.print("❌ Ingrese un número válido para " + fieldName + ": ");
                scanner.next(); // Limpiar input inválido
            }
        }
        return value;
    }
    
    /**
     * Valida que el máximo sea mayor o igual al mínimo
     */
    private static int validateMinMaxInput(Scanner scanner, int minValue, String fieldName) {
        int value;
        while (true) {
            try {
                value = scanner.nextInt();
                if (value >= minValue) {
                    break;
                } else {
                    System.out.print("❌ " + fieldName + " debe ser mayor o igual a " + minValue + ". Intente nuevamente: ");
                }
            } catch (InputMismatchException e) {
                System.out.print("❌ Ingrese un número válido para " + fieldName + ": ");
                scanner.next(); // Limpiar input inválido
            }
        }
        return value;
    }
    
    /**
     * Muestra un resumen de la configuración antes de iniciar el juego
     */
    private static void showGameConfiguration(int boardSize, int totalPlayers, int gameTimeSeconds,
                                            int playerTurnMin, int playerTurnMax, int maxLifeBonuses,
                                            int lifeRobotMin, int lifeRobotMax, int coinRobotMin, int coinRobotMax,
                                            int evilRobotMin, int evilRobotMax) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("               CONFIGURACIÓN DEL JUEGO");
        System.out.println("=".repeat(60));
        System.out.println("📋 Tablero: " + boardSize + "x" + boardSize + " casillas");
        System.out.println("👥 Jugadores totales: " + totalPlayers + " (máximo " + MAX_ACTIVE_PLAYERS + " activos)");
        System.out.println("⏰ Tiempo de partida: " + gameTimeSeconds + " segundos");
        System.out.println("🎮 Turnos jugadores: " + playerTurnMin + "-" + playerTurnMax + " ms");
        System.out.println("❤️  Robot vida: " + maxLifeBonuses + " vidas máx, " + lifeRobotMin + "-" + lifeRobotMax + " ms");
        System.out.println("💰 Robot monedas: " + coinRobotMin + "-" + coinRobotMax + " ms");
        System.out.println("💀 Robot malvado: " + evilRobotMin + "-" + evilRobotMax + " ms");
        System.out.println("=".repeat(60));
        System.out.println("🚀 Iniciando juego en 3 segundos...\n");
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
    
    
