import java.util.*;

public class Vertex {
    private String code;
    private String name;
    private Set<String> lines = new HashSet<>();
    private List<Edge> incidentEdges = new ArrayList<>();

    public Vertex(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public Set<String> getLines() { return lines; }

    public void addLine(String line) {
        lines.add(line);
    }

    public void addEdge(Edge e) {
        incidentEdges.add(e);
    }

    public void removeEdge(Edge e) {
        incidentEdges.remove(e);
    }

    public List<Edge> getEdges() { return incidentEdges; }
}