import java.util.*;

public class Dijkstra {

    // One item in the priority queue: "we can reach this state with this cost".
    public static class Entry {
        public final State state;
        public final int minutes;
        public final int interchanges;

        public Entry(State state, int minutes, int interchanges) {
            this.state = state;
            this.minutes = minutes;
            this.interchanges = interchanges;
        }
    }

    // The answer: the states on the path (in order) and the total cost.
    public static class Result {
        public final List<State> path;
        public final int minutes;
        public final int interchanges;

        public Result(List<State> path, int minutes, int interchanges) {
            this.path = path;
            this.minutes = minutes;
            this.interchanges = interchanges;
        }
    }

    // Cost function 1: least time first, interchanges only break ties.
    public static final Comparator<Entry> FASTEST = (a, b) -> {
        if (a.minutes != b.minutes) return Integer.compare(a.minutes, b.minutes);
        return Integer.compare(a.interchanges, b.interchanges);
    };

    // Cost function 2: fewest interchanges first, time only breaks ties.
    public static final Comparator<Entry> FEWEST_INTERCHANGES = (a, b) -> {
        if (a.interchanges != b.interchanges) return Integer.compare(a.interchanges, b.interchanges);
        return Integer.compare(a.minutes, b.minutes);
    };

    // Returns null if there is no path.
    public static Result find(StateGraph sg, String fromCode, String toCode, Comparator<Entry> cmp) {
        return find(sg, fromCode, toCode, cmp, Collections.<String>emptySet());
    }

    // Bonus (station closure): closedCodes are stations that cannot be used.
    // A move into a closed station is simply skipped, so Dijkstra finds the best detour.
    public static Result find(StateGraph sg, String fromCode, String toCode, Comparator<Entry> cmp,
                              Set<String> closedCodes) {
        MinHeap<Entry> heap = new MinHeap<Entry>(cmp);
        Map<State, Entry> best = new HashMap<>();   // best cost known so far (like D[])
        Map<State, State> prev = new HashMap<>();   // where we came from (predecessor)
        Set<State> done = new HashSet<>();          // the "cloud"

        // Start from every (station, line) state of the origin, cost 0.
        for (State s : sg.statesOf(fromCode)) {
            Entry start = new Entry(s, sg.boardWait(s), 0);   // wait for the first train (0 without headway)
            best.put(s, start);
            heap.insert(start);
        }

        while (!heap.isEmpty()) {
            Entry cur = heap.removeMin();
            State u = cur.state;

            // Old, outdated copy of a state we already finished: skip it.
            if (done.contains(u)) {
                continue;
            }
            done.add(u);

            // First time we take out a state of the destination station: finished.
            if (u.getStation().getCode().equals(toCode)) {
                return buildResult(u, cur, prev);
            }

            for (Move m : sg.movesFrom(u)) {
                State v = m.to;
                if (done.contains(v)) {
                    continue;
                }
                if (closedCodes.contains(v.getStation().getCode())) {
                    continue;   // station closed for maintenance
                }
                Entry candidate = new Entry(v, cur.minutes + m.minutes, cur.interchanges + m.interchanges);
                Entry old = best.get(v);
                if (old == null || cmp.compare(candidate, old) < 0) {
                    best.put(v, candidate);
                    prev.put(v, u);
                    heap.insert(candidate);
                }
            }
        }
        return null;
    }

    // Walk backwards with prev, then reverse the list.
    private static Result buildResult(State end, Entry cost, Map<State, State> prev) {
        List<State> path = new ArrayList<>();
        State s = end;
        while (s != null) {
            path.add(s);
            s = prev.get(s);
        }
        Collections.reverse(path);
        return new Result(path, cost.minutes, cost.interchanges);
    }
}