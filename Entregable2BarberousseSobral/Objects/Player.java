import Entregable2BarberousseSobral.Interfaces.PlayerInterface;

public class Player implements PlayerInterface {
    private final String name;
    private int lifes;
    private int coins;

    public Player(String name) {
        this.name = name;
        this.lifes = 2;
        this.coins = 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLifes() {
        return lifes;
    }

    @Override
    public int updateLife(int delta) {
        if (lifes > 0) {
            lifes += delta;
        }
        return lifes;
    }

    @Override
    public int getCoins() {
        return coins;
    }

    @Override
    public void addCoins(int delta) {
        coins += delta;
    }




}