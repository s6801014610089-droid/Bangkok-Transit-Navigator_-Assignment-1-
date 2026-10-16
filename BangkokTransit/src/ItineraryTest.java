import java.io.*;
import java.util.*;

public class ItineraryTest {
    public static void main(String[] args) throws IOException {
        Graph g = GraphLoader.load(ProjectDir.csvPath());
        StateGraph sg = new StateGraph(g);

        System.out.println("--- N1 -> W1, depart 08:15, fastest ---");
        Planner.plan(sg, "N1", "W1", false, TimeUtil.parse("08:15"), Dijkstra.FASTEST).print();

        System.out.println();
        System.out.println("--- N1 -> W1, arrive by 09:00, fastest ---");
        Planner.plan(sg, "N1", "W1", true, TimeUtil.parse("09:00"), Dijkstra.FASTEST).print();

        System.out.println();
        System.out.println("--- BL01 -> N1, depart 08:00, fastest ---");
        Planner.plan(sg, "BL01", "N1", false, TimeUtil.parse("08:00"), Dijkstra.FASTEST).print();

        System.out.println();
        System.out.println("--- BL01 -> N1, depart 08:00, fewest interchanges ---");
        Planner.plan(sg, "BL01", "N1", false, TimeUtil.parse("08:00"), Dijkstra.FEWEST_INTERCHANGES).print();

        System.out.println();
        System.out.println("--- error checks ---");
        try {
            Planner.plan(sg, "N1", "N1", false, 480, Dijkstra.FASTEST);
        } catch (IllegalArgumentException e) {
            System.out.println("same station: " + e.getMessage());
        }
        try {
            Planner.plan(sg, "N1", "XX99", false, 480, Dijkstra.FASTEST);
        } catch (IllegalArgumentException e) {
            System.out.println("unknown station: " + e.getMessage());
        }
        try {
            TimeUtil.parse("8.15");
        } catch (IllegalArgumentException e) {
            System.out.println("bad time: " + e.getMessage());
        }
        try {
            TimeUtil.parse("25:00");
        } catch (IllegalArgumentException e) {
            System.out.println("bad time: " + e.getMessage());
        }
    }
}