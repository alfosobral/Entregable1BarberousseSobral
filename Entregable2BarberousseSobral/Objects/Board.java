public class Board {
    private Cell[][] cells;
    private int size;

    public Board(int size) {
        this.size = size;
        cells = new Cell[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                cells[i][j] = new Cell();
            }
        }
    }

    public Cell getCell(int x, int y) {
        return cells[x][y];
    }

    public int getSize() {
        return size;
    }

    public void addLifeToCell(int x, int y) {
        cells[x][y].setHealing(true);
    }

    public void addCoinsToCell(int x, int y, int amount) {
        cells[x][y].setCoins(amount);
    }
}