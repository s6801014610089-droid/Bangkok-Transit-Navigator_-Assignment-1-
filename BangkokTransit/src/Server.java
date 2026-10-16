import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Server {
    static final int PORT = 8080;

    static Graph graph;
    static StateGraph stateGraph;
    static StateGraph stateGraphWait;   // bonus: same network, with waiting time (headway)
    static StationIndex index;
    static Path webDir;

    public static void main(String[] args) throws IOException {
        Path dir = ProjectDir.find();
        graph = GraphLoader.load(dir.resolve("bangkok_network_graph.csv").toString());
        stateGraph = new StateGraph(graph);
        stateGraphWait = new StateGraph(graph, true);
        index = new StationIndex(graph);
        webDir = dir.resolve("web");

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", Server::handlePage);
        server.createContext("/api/stations", Server::handleStations);
        server.createContext("/api/route", Server::handleRoute);
        server.createContext("/api/network", Server::handleNetwork);
        server.start();
        System.out.println("Server running. Open http://localhost:" + PORT + " in your browser.");
    }

    // ---------- pages ----------

    static void handlePage(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path file = webDir.resolve(path.substring(1)).normalize();
        if (!file.startsWith(webDir.normalize()) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain; charset=utf-8", "Not found");
            return;
        }
        String type = path.endsWith(".js") ? "application/javascript; charset=utf-8"
                : path.endsWith(".css") ? "text/css; charset=utf-8"
                  : "text/html; charset=utf-8";
        send(ex, 200, type, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    // ---------- /api/stations ----------

    static void handleStations(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String name : index.names()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"name\":").append(q(name)).append(",\"codes\":[");
            List<String> codes = index.codesOf(name);
            for (int i = 0; i < codes.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(q(codes.get(i)));
            }
            sb.append("]}");
        }
        sb.append("]");
        send(ex, 200, "application/json; charset=utf-8", sb.toString());
    }

    // ---------- /api/network ----------
    // Everything the map needs: stations, edges, and the cross-platform stations.

    static void handleNetwork(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder("{\"stations\":[");
        boolean first = true;
        for (Vertex v : graph.getVertices()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"code\":").append(q(v.getCode()))
                    .append(",\"name\":").append(q(v.getName())).append("}");
        }
        sb.append("],\"edges\":[");
        first = true;
        for (Edge e : graph.getEdges()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"a\":").append(q(e.getA().getCode()))
                    .append(",\"b\":").append(q(e.getB().getCode()))
                    .append(",\"type\":").append(q(e.getType()))
                    .append(",\"line\":").append(q(e.getLine())).append("}");
        }
        sb.append("],\"crossPlatform\":[");
        first = true;
        for (Vertex v : graph.getCrossPlatform()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(q(v.getCode()));
        }
        sb.append("]}");
        send(ex, 200, "application/json; charset=utf-8", sb.toString());
    }

    // ---------- /api/route ----------
    // /api/route?from=Mo+Chit&to=Sala+Daeng&time=08:15&mode=depart|arrive&goal=fastest|fewest

    static void handleRoute(HttpExchange ex) throws IOException {
        Map<String, String> p = parseQuery(ex.getRequestURI().getRawQuery());
        try {
            String from = need(p, "from");
            String to = need(p, "to");
            int time = TimeUtil.parse(need(p, "time"));
            boolean arriveBy = "arrive".equals(p.get("mode"));
            Comparator<Dijkstra.Entry> cmp = "fewest".equals(p.get("goal"))
                    ? Dijkstra.FEWEST_INTERCHANGES : Dijkstra.FASTEST;

            // Bonus: closed stations, separated by "|"
            List<String> closed = new ArrayList<>();
            String closedParam = p.get("closed");
            if (closedParam != null) {
                for (String c : closedParam.split("\\|")) {
                    if (!c.trim().isEmpty()) closed.add(c.trim());
                }
            }

            Itinerary it = Planner.planByName(index, "1".equals(p.get("wait")) ? stateGraphWait : stateGraph, from, to, arriveBy, time, cmp, closed);
            send(ex, 200, "application/json; charset=utf-8", toJson(it));
        } catch (IllegalArgumentException e) {
            send(ex, 200, "application/json; charset=utf-8",
                    "{\"ok\":false,\"error\":" + q(e.getMessage()) + "}");
        }
    }

    static String need(Map<String, String> p, String key) {
        String v = p.get(key);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException("Please fill in: " + key);
        }
        return v.trim();
    }

    static String toJson(Itinerary it) {
        StringBuilder sb = new StringBuilder("{\"ok\":true");
        sb.append(",\"depart\":").append(q(TimeUtil.format(it.departMinutes)));
        sb.append(",\"arrive\":").append(q(TimeUtil.format(it.arriveMinutes)));
        sb.append(",\"total\":").append(it.totalMinutes);
        sb.append(",\"interchanges\":").append(it.interchanges);
        sb.append(",\"summary\":").append(q(it.summary()));
        sb.append(",\"steps\":[");
        for (int i = 0; i < it.lines.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(q(it.lines.get(i)));
        }
        sb.append("],\"path\":[");
        for (int i = 0; i < it.path.size(); i++) {
            State s = it.path.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"code\":").append(q(s.getStation().getCode()))
                    .append(",\"name\":").append(q(s.getStation().getName()))
                    .append(",\"line\":").append(q(s.getLine())).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    // ---------- helpers ----------

    static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null) return m;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            m.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    // JSON string with quotes and escapes
    static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') sb.append('\\').append(c);
            else if (c == '\n') sb.append("\\n");
            else sb.append(c);
        }
        return sb.append("\"").toString();
    }

    static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}