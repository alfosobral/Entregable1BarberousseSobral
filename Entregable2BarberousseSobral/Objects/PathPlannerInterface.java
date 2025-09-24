import java.util.List;

public interface PathPlannerInterface {
    List<Pos> plan(int r, int c, Board tablero, int dice);
}
