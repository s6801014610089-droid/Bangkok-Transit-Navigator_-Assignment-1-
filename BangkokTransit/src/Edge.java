public class Edge {
    private Vertex a;
    private Vertex b;
    private String type;
    private String line;

    public Edge(Vertex a, Vertex b, String type, String line) {
        this.a = a;
        this.b = b;
        this.type = type;
        this.line = line;
    }

    public Vertex getA() { return a; }
    public Vertex getB() { return b; }
    public String getType() { return type; }
    public String getLine() { return line; }


    public Vertex getOther(Vertex v) {
        if (v == a) {
            return b;
        }
        return a;
    }
}
