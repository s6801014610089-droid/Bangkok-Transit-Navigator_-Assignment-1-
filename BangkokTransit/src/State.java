import java.util.Objects;

public class State {
    private final Vertex station;
    private final String line;

    public State(Vertex station, String line) {
        this.station = station;
        this.line = line;
    }

    public Vertex getStation() { return station; }
    public String getLine() { return line; }

    // Two states are the same if they have the same station code AND the same line.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State other = (State) o;
        return station.getCode().equals(other.station.getCode())
                && line.equals(other.line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(station.getCode(), line);
    }

    @Override
    public String toString() {
        return station.getCode() + "@" + line;
    }
}
