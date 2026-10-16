import java.util.*;

public class Itinerary {
    public final List<State> path;       // every state on the route, in order
    public final List<String> lines;     // the stop-by-stop text, one string per line
    public final int departMinutes;      // minutes since midnight
    public final int arriveMinutes;
    public final int totalMinutes;
    public final int interchanges;

    public Itinerary(List<State> path, List<String> lines, int departMinutes,
                     int arriveMinutes, int totalMinutes, int interchanges) {
        this.path = path;
        this.lines = lines;
        this.departMinutes = departMinutes;
        this.arriveMinutes = arriveMinutes;
        this.totalMinutes = totalMinutes;
        this.interchanges = interchanges;
    }

    public String summary() {
        return "Total: " + totalMinutes + " min, " + interchanges
                + (interchanges == 1 ? " interchange" : " interchanges");
    }

    public void print() {
        for (String s : lines) {
            System.out.println(s);
        }
        System.out.println(summary());
    }
}