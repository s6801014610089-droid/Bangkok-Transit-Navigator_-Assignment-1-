import java.io.*;
import java.util.*;

public class DijkstraTest {
    public static void main(String[] args) throws IOException {
        Graph g = GraphLoader.load(ProjectDir.csvPath());
        StateGraph sg = new StateGraph(g);

        show(sg, "N1", "W1");
        show(sg, "S12", "BL14");
        show(sg, "BL37", "E23");
    }

    static void show(StateGraph sg, String from, String to) {
        for (int mode = 0; mode < 2; mode++) {
            Comparator<Dijkstra.Entry> c = mode == 0 ? Dijkstra.FASTEST : Dijkstra.FEWEST_INTERCHANGES;
            Dijkstra.Result r = Dijkstra.find(sg, from, to, c);
            System.out.println(from + " -> " + to + (mode == 0 ? "  [fastest]" : "  [fewest interchanges]"));
            if (r == null) {
                System.out.println("   no path");
                continue;
            }
            System.out.println("   " + r.minutes + " min, " + r.interchanges + " interchange(s), " + r.path.size() + " states");
            System.out.println("   " + r.path);
        }
    }
}