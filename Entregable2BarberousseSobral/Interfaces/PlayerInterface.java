package Interfaces;

public interface PlayerInterface {
    String getName();
    int getLifes();
    int updateLife(int delta);
    int getCoins();
    void addCoins(int delta);
    int throwDice();
}
