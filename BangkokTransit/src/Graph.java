import java.util.*;

public class Graph {
    private Map<String, Vertex> vertices = new HashMap<>();
    private List<Edge> edges = new ArrayList<>();
    // Stations that have a cross-platform self-loop in the CSV (only Siam, CEN).
    private List<Vertex> crossPlatform = new ArrayList<>();

    public int numVertices() { return vertices.size(); }
    public int numEdges() { return edges.size(); }

    public Collection<Vertex> getVertices() { return vertices.values(); }
    public List<Edge> getEdges() { return edges; }
    public List<Vertex> getCrossPlatform() { return crossPlatform; }

    public Vertex getVertex(String code) {
        return vertices.get(code);
    }

    // If this station code already exists, return the existing vertex.
    // Otherwise create a new one.
    public Vertex insertVertex(String code, String name) {
        Vertex v = vertices.get(code);
        if (v == null) {
            v = new Vertex(code, name);
            vertices.put(code, v);
        }
        return v;
    }

    // Undirected graph: add the edge to the lists of BOTH endpoints.
    public Edge insertEdge(Vertex u, Vertex v, String type, String line) {
        Edge e = new Edge(u, v, type, line);
        edges.add(e);
        u.addEdge(e);
        v.addEdge(e);
        return e;
    }

    // ---------- the rest of the Graph ADT (Lecture 18) ----------
    // The graph is undirected, so "outgoing" and "incoming" are the same edges.

    public int degree(Vertex v) { return v.getEdges().size(); }
    public int outDegree(Vertex v) { return degree(v); }
    public int inDegree(Vertex v) { return degree(v); }

    public List<Edge> outgoingEdges(Vertex v) { return v.getEdges(); }
    public List<Edge> incomingEdges(Vertex v) { return v.getEdges(); }

    // The edge joining u and v, or null if there is none.
    public Edge getEdge(Vertex u, Vertex v) {
        for (Edge e : u.getEdges()) {
            if (e.getOther(u) == v) {
                return e;
            }
        }
        return null;
    }

    public Vertex[] endVertices(Edge e) {
        return new Vertex[] { e.getA(), e.getB() };
    }

    // The other end of edge e, seen from v.
    public Vertex opposite(Vertex v, Edge e) {
        return e.getOther(v);
    }

    public void removeEdge(Edge e) {
        e.getA().removeEdge(e);
        e.getB().removeEdge(e);
        edges.remove(e);
    }

    // Remove the vertex and all edges that touch it.
    public void removeVertex(Vertex v) {
        for (Edge e : new ArrayList<>(v.getEdges())) {
            removeEdge(e);
        }
        vertices.remove(v.getCode());
        crossPlatform.remove(v);
    }

    // Remember that this station needs a platform change between its own lines.
    public void markCrossPlatform(Vertex v) {
        crossPlatform.add(v);
    }
}