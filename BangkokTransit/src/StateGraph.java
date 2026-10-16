import java.util.*;

// A second graph whose vertices are (station, line) pairs.
// Dijkstra will run on this graph, so a line change is always a real edge.
public class StateGraph {
    public static final int RIDE_MIN = 3;      // one stop on the same line
    public static final int TRANSFER_MIN = 6;  // walk between different station codes
    public static final int SIAM_MIN = 5;      // cross platform at the same station

    // Bonus (headway): assumed minutes between trains on each line (the data has no timetable).
    // The expected wait for a train is half of this.
    private static final Map<String, Integer> HEADWAY = new HashMap<>();
    static {
        HEADWAY.put("BTS Sukhumvit Line", 4);
        HEADWAY.put("BTS Silom Line", 4);
        HEADWAY.put("MRT Blue Line", 6);
        HEADWAY.put("MRT Purple Line", 8);
    }
    public static final int DEFAULT_HEADWAY = 6;

    public static int headwayOf(String line) {
        Integer h = HEADWAY.get(line);
        return h == null ? DEFAULT_HEADWAY : h;
    }

    // The CSV has no Line Track row for PP16, so it has no line of its own.
    private static final String PP16_LINE = "MRT Purple Line";

    private Map<State, List<Move>> adj = new HashMap<>();
    private Map<String, List<State>> statesOfStation = new HashMap<>();

    // true = every boarding and every line change also costs the expected wait (headway / 2)
    private final boolean headway;

    public StateGraph(Graph g) {
        this(g, false);
    }

    public StateGraph(Graph g, boolean withHeadway) {
        this.headway = withHeadway;
        // 1) one state for every (station, line) pair
        for (Vertex v : g.getVertices()) {
            Set<String> lines = v.getLines();
            if (lines.isEmpty()) {
                lines = new HashSet<>();
                lines.add(PP16_LINE);
            }
            List<State> list = new ArrayList<>();
            for (String line : lines) {
                State s = new State(v, line);
                adj.put(s, new ArrayList<>());
                list.add(s);
            }
            statesOfStation.put(v.getCode(), list);
        }

        // 2) ride edges and walking-transfer edges, from the CSV edges
        for (Edge e : g.getEdges()) {
            if (e.getType().equals("Line Track")) {
                State a = stateOf(e.getA(), e.getLine());
                State b = stateOf(e.getB(), e.getLine());
                connect(a, b, RIDE_MIN, 0, false);
            } else {
                State a = statesOfStation.get(e.getA().getCode()).get(0);
                State b = statesOfStation.get(e.getB().getCode()).get(0);
                connect(a, b, TRANSFER_MIN, 1, true);
            }
        }

        // 3) cross-platform transfers inside one station (Siam)
        for (Vertex v : g.getCrossPlatform()) {
            List<State> list = statesOfStation.get(v.getCode());
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    connect(list.get(i), list.get(j), SIAM_MIN, 1, true);
                }
            }
        }
    }

    private State stateOf(Vertex v, String line) {
        for (State s : statesOfStation.get(v.getCode())) {
            if (s.getLine().equals(line)) {
                return s;
            }
        }
        return null;
    }

    // Undirected: add a move in both directions.
    private void connect(State a, State b, int minutes, int interchanges, boolean isTransfer) {
        // With headway, a line change also waits for the next train of the line we board.
        int toB = minutes + (headway && isTransfer ? headwayOf(b.getLine()) / 2 : 0);
        int toA = minutes + (headway && isTransfer ? headwayOf(a.getLine()) / 2 : 0);
        adj.get(a).add(new Move(b, toB, interchanges, isTransfer));
        adj.get(b).add(new Move(a, toA, interchanges, isTransfer));
    }

    public boolean hasHeadway() { return headway; }

    // Expected wait when we first board a train of this state's line.
    public int boardWait(State s) {
        return headway ? headwayOf(s.getLine()) / 2 : 0;
    }

    public int numStates() { return adj.size(); }

    public int numMoves() {
        int total = 0;
        for (List<Move> list : adj.values()) {
            total += list.size();
        }
        return total / 2; // each undirected edge is stored twice
    }

    public List<Move> movesFrom(State s) { return adj.get(s); }
    public List<State> statesOf(String stationCode) { return statesOfStation.get(stationCode); }
}