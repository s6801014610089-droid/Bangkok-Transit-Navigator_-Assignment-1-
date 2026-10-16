import java.util.*;

public class Planner {

    // Bonus: assumed service hours of every line (the data has no real timetable).
    public static final int FIRST_TRAIN = 5 * 60;    // 05:00
    public static final int LAST_TRAIN = 24 * 60;    // 24:00

    // arriveBy = false: timeMinutes is the departure time ("Depart at").
    // arriveBy = true:  timeMinutes is the wanted arrival time ("Arrive by").
    // cmp chooses the goal: Dijkstra.FASTEST or Dijkstra.FEWEST_INTERCHANGES.
    public static Itinerary plan(StateGraph sg, String fromCode, String toCode,
                                 boolean arriveBy, int timeMinutes,
                                 Comparator<Dijkstra.Entry> cmp) {
        return plan(sg, fromCode, toCode, arriveBy, timeMinutes, cmp, Collections.<String>emptySet());
    }

    // Same, with a set of closed station codes (bonus: station closure).
    public static Itinerary plan(StateGraph sg, String fromCode, String toCode,
                                 boolean arriveBy, int timeMinutes,
                                 Comparator<Dijkstra.Entry> cmp, Set<String> closedCodes) {
        if (sg.statesOf(fromCode) == null) {
            throw new IllegalArgumentException("Unknown origin station: " + fromCode);
        }
        if (sg.statesOf(toCode) == null) {
            throw new IllegalArgumentException("Unknown destination station: " + toCode);
        }
        if (fromCode.equals(toCode)) {
            throw new IllegalArgumentException("Origin and destination are the same station");
        }

        if (closedCodes.contains(fromCode)) {
            throw new IllegalArgumentException("The origin station is closed");
        }
        if (closedCodes.contains(toCode)) {
            throw new IllegalArgumentException("The destination station is closed");
        }

        Dijkstra.Result r = Dijkstra.find(sg, fromCode, toCode, cmp, closedCodes);
        if (r == null) {
            throw new IllegalArgumentException(closedCodes.isEmpty()
                    ? "No route found" : "No route found with the closed stations");
        }

        if (sg.hasHeadway()) {
            return planWithHeadway(sg, r, arriveBy, timeMinutes);
        }

        // The graph has no timetable, so the trip always takes r.minutes.
        // Arrive by = the same trip, just started r.minutes earlier.
        // Bonus (first / last train): all lines run from FIRST_TRAIN to LAST_TRAIN.
        int depart;
        String note = null;
        if (arriveBy) {
            if (timeMinutes < FIRST_TRAIN) {
                throw new IllegalArgumentException("No service at " + TimeUtil.format(timeMinutes)
                        + ". Trains run " + TimeUtil.format(FIRST_TRAIN) + " - 24:00");
            }
            depart = timeMinutes - r.minutes;
            if (depart < FIRST_TRAIN) {
                throw new IllegalArgumentException("Too early: the first train is at "
                        + TimeUtil.format(FIRST_TRAIN) + ", so you cannot arrive by "
                        + TimeUtil.format(timeMinutes) + " (earliest arrival "
                        + TimeUtil.format(FIRST_TRAIN + r.minutes) + ")");
            }
        } else {
            depart = timeMinutes;
            if (depart < FIRST_TRAIN) {
                note = "Note: the service has not started yet (trains run "
                        + TimeUtil.format(FIRST_TRAIN) + " - 24:00). The first train leaves at "
                        + TimeUtil.format(FIRST_TRAIN) + ", so your trip starts then instead of "
                        + TimeUtil.format(timeMinutes) + ".";
                depart = FIRST_TRAIN;
            }
            if (depart + r.minutes > LAST_TRAIN) {
                throw new IllegalArgumentException("Too late: the last train is at 24:00, "
                        + "and this trip would arrive at " + TimeUtil.format(depart + r.minutes)
                        + " (next day)");
            }
        }
        int arrive = depart + r.minutes;

        List<String> lines = describe(sg, r.path, depart);
        if (note != null) {
            lines.add(0, note);
        }
        return new Itinerary(r.path, lines, depart, arrive, r.minutes, r.interchanges);
    }

    // ---------- Bonus: headway ----------
    // Dijkstra chose the route using the EXPECTED wait (headway / 2). Here the trip is replayed
    // against a simple timetable: on every line a train leaves at FIRST_TRAIN + k * headway.
    // Because the real wait now depends on the clock, "arrive by" is no longer just the
    // mirror image of "depart at".

    private static class Trip {
        List<String> lines = new ArrayList<>();
        int arrive;
    }

    private static Itinerary planWithHeadway(StateGraph sg, Dijkstra.Result r,
                                             boolean arriveBy, int timeMinutes) {
        int depart;
        Trip trip;
        String note = null;
        if (arriveBy) {
            if (timeMinutes < FIRST_TRAIN) {
                throw new IllegalArgumentException("No service at " + TimeUtil.format(timeMinutes)
                        + ". Trains run " + TimeUtil.format(FIRST_TRAIN) + " - 24:00");
            }
            // Latest start that still arrives in time: try later starts first, go back minute by minute.
            depart = timeMinutes;
            trip = simulate(sg, r.path, depart);
            while (trip.arrive > timeMinutes && depart > 0) {
                depart--;
                trip = simulate(sg, r.path, depart);
            }
            if (depart < FIRST_TRAIN) {
                throw new IllegalArgumentException("Too early: the first train is at "
                        + TimeUtil.format(FIRST_TRAIN) + ", so you cannot arrive by "
                        + TimeUtil.format(timeMinutes));
            }
        } else {
            depart = timeMinutes;
            if (depart < FIRST_TRAIN) {
                note = "Note: the service has not started yet (trains run "
                        + TimeUtil.format(FIRST_TRAIN) + " - 24:00). The first train leaves at "
                        + TimeUtil.format(FIRST_TRAIN) + ", so your trip starts then instead of "
                        + TimeUtil.format(timeMinutes) + ".";
                depart = FIRST_TRAIN;
            }
            trip = simulate(sg, r.path, depart);
            if (trip.arrive > LAST_TRAIN) {
                throw new IllegalArgumentException("Too late: the last train is at 24:00, "
                        + "and this trip would arrive at " + TimeUtil.format(trip.arrive)
                        + " (next day)");
            }
        }
        List<String> lines = trip.lines;
        if (note != null) {
            lines.add(0, note);
        }
        return new Itinerary(r.path, lines, depart, trip.arrive, trip.arrive - depart, r.interchanges);
    }

    // The first train of a line at or after time t (trains leave every headway minutes from FIRST_TRAIN).
    private static int nextTrain(String line, int t) {
        int h = StateGraph.headwayOf(line);
        if (t <= FIRST_TRAIN) {
            return FIRST_TRAIN;
        }
        int k = (t - FIRST_TRAIN + h - 1) / h;
        return FIRST_TRAIN + k * h;
    }

    // Wait for the next train of s's line (if needed) and board it. Returns the boarding time.
    private static int boardWithWait(Trip trip, State s, int t) {
        int board = nextTrain(s.getLine(), t);
        if (board > t) {
            trip.lines.add(TimeUtil.format(t) + " Wait " + (board - t) + " min for " + s.getLine()
                    + " (a train every " + StateGraph.headwayOf(s.getLine()) + " min)");
        }
        trip.lines.add(TimeUtil.format(board) + " Board " + s.getLine() + " at " + name(s));
        return board;
    }

    // Walk along the path from the time the rider starts, with real waits.
    private static Trip simulate(StateGraph sg, List<State> p, int depart) {
        Trip trip = new Trip();
        int t = boardWithWait(trip, p.get(0), depart);
        for (int j = 0; j + 1 < p.size(); j++) {
            State a = p.get(j);
            State b = p.get(j + 1);
            Move m = findMove(sg, a, b);
            boolean last = (j + 1 == p.size() - 1);
            if (!m.isTransfer) {
                t += StateGraph.RIDE_MIN;
                if (last) {
                    trip.lines.add(TimeUtil.format(t) + " Arrive " + name(b));
                }
            } else {
                boolean samePlace = a.getStation().getCode().equals(b.getStation().getCode());
                if (samePlace) {
                    trip.lines.add(TimeUtil.format(t) + " Arrive " + name(a) + " -- cross platform");
                    t += StateGraph.SIAM_MIN;
                } else {
                    trip.lines.add(TimeUtil.format(t) + " Arrive " + name(a)
                            + " -- walk to " + name(b) + " (" + b.getStation().getCode() + ")");
                    t += StateGraph.TRANSFER_MIN;
                }
                t = boardWithWait(trip, b, t);
                if (last) {
                    trip.lines.add(TimeUtil.format(t) + " Arrive " + name(b));
                }
            }
        }
        trip.arrive = t;
        return trip;
    }

    // Same as plan(), but the user picked an item from the dropdown (a station label).
    public static Itinerary planByName(StationIndex idx, StateGraph sg,
                                       String fromName, String toName,
                                       boolean arriveBy, int timeMinutes,
                                       Comparator<Dijkstra.Entry> cmp) {
        return planByName(idx, sg, fromName, toName, arriveBy, timeMinutes, cmp,
                Collections.<String>emptyList());
    }

    // closedNames are dropdown labels of stations closed for maintenance.
    public static Itinerary planByName(StationIndex idx, StateGraph sg,
                                       String fromName, String toName,
                                       boolean arriveBy, int timeMinutes,
                                       Comparator<Dijkstra.Entry> cmp, List<String> closedNames) {
        Set<String> closed = new HashSet<>();
        for (String n : closedNames) {
            List<String> codes = idx.codesOf(n);
            if (codes == null) {
                throw new IllegalArgumentException("Unknown closed station: " + n);
            }
            closed.addAll(codes);
        }
        List<String> from = idx.codesOf(fromName);
        List<String> to = idx.codesOf(toName);
        if (from == null) {
            throw new IllegalArgumentException("Unknown origin station: " + fromName);
        }
        if (to == null) {
            throw new IllegalArgumentException("Unknown destination station: " + toName);
        }
        return plan(sg, from.get(0), to.get(0), arriveBy, timeMinutes, cmp, closed);
    }

    private static List<String> describe(StateGraph sg, List<State> p, int depart) {
        List<String> out = new ArrayList<>();
        int t = depart;
        out.add(TimeUtil.format(t) + " Board " + p.get(0).getLine() + " at " + name(p.get(0)));

        for (int j = 0; j + 1 < p.size(); j++) {
            State a = p.get(j);
            State b = p.get(j + 1);
            Move m = findMove(sg, a, b);
            boolean last = (j + 1 == p.size() - 1);

            if (!m.isTransfer) {
                t += m.minutes;
                if (last) {
                    out.add(TimeUtil.format(t) + " Arrive " + name(b));
                }
            } else {
                boolean samePlace = a.getStation().getCode().equals(b.getStation().getCode());
                if (samePlace) {
                    out.add(TimeUtil.format(t) + " Arrive " + name(a) + " -- cross platform");
                } else {
                    out.add(TimeUtil.format(t) + " Arrive " + name(a)
                            + " -- walk to " + name(b) + " (" + b.getStation().getCode() + ")");
                }
                t += m.minutes;
                out.add(TimeUtil.format(t) + " Board " + b.getLine() + " at " + name(b));
                if (last) {
                    out.add(TimeUtil.format(t) + " Arrive " + name(b));
                }
            }
        }
        return out;
    }

    private static String name(State s) {
        return s.getStation().getName();
    }

    // Find the move that goes from state a to state b.
    private static Move findMove(StateGraph sg, State a, State b) {
        for (Move m : sg.movesFrom(a)) {
            if (m.to.equals(b)) {
                return m;
            }
        }
        throw new IllegalStateException("No move from " + a + " to " + b);
    }
}