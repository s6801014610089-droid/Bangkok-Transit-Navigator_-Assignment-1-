import java.util.*;

public class StateTest {
    public static void main(String[] args) {
        Vertex cen = new Vertex("CEN", "Siam");
        Set<State> set = new HashSet<>();
        set.add(new State(cen, "BTS Sukhumvit Line"));
        set.add(new State(cen, "BTS Sukhumvit Line"));
        set.add(new State(cen, "BTS Silom Line"));
        System.out.println("distinct states = " + set.size());
    }
}
