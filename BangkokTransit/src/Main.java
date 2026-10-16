import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        Graph g = GraphLoader.load(ProjectDir.csvPath());
        System.out.println("vertices = " + g.numVertices());
        System.out.println("edges    = " + g.numEdges());
        System.out.println("BL01 degree = " + g.getVertex("BL01").getEdges().size());
        Vertex sirin = g.getVertex("BL05A");
        if (sirin != null) {
            System.out.println("Sirindhorn degree = " + sirin.getEdges().size());
        }

        StateGraph sg = new StateGraph(g);
        System.out.println("states = " + sg.numStates());
        System.out.println("moves  = " + sg.numMoves());

        // Look at what you can do from Siam on the Sukhumvit line
        for (State s : sg.statesOf("CEN")) {
            System.out.println("From " + s + ":");
            for (Move m : sg.movesFrom(s)) {
                String kind = m.isTransfer ? "TRANSFER" : "ride";
                System.out.println("   " + kind + " to " + m.to + " (" + m.minutes + " min)");
            }
        }
    }
}