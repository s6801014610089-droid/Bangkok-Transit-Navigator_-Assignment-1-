import java.io.*;
import java.util.*;

public class GraphLoader {
    // The CSV skips Sirindhorn (between BL05 and BL06). Set to false to use the file exactly as given.
    public static final boolean ADD_SIRINDHORN = true;

    public static Graph load(String path) throws IOException {
        Graph g = new Graph();
        Set<String> seen = new HashSet<>();

        BufferedReader br = new BufferedReader(new FileReader(path));
        br.readLine(); // skip the header row
        String row;
        while ((row = br.readLine()) != null) {
            String[] f = row.split(",");
            String srcCode = f[0].trim();
            String srcName = f[1].trim();
            String tgtCode = f[2].trim();
            String tgtName = f[3].trim();
            String type = f[4].trim();
            String line = f[5].trim();

            Vertex u = g.insertVertex(srcCode, srcName);
            Vertex v = g.insertVertex(tgtCode, tgtName);

            // Self-loop (Siam, CEN -> CEN): do not make an edge, just remember
            // that this station needs a platform change between its own lines.
            if (srcCode.equals(tgtCode)) {
                g.markCrossPlatform(u);
                continue;
            }

            // Skip duplicate edges (e.g. BL01-BL32). The key ignores direction,
            // so A-B and B-A give the same key. The first row seen is kept.
            String key = srcCode.compareTo(tgtCode) < 0
                    ? srcCode + "-" + tgtCode
                    : tgtCode + "-" + srcCode;
            if (!seen.add(key)) {
                continue;
            }

            // Insert the missing station Sirindhorn between BL05 and BL06.
            // Its real code (BL06) is already used by Bang Phlat in this file,
            // so it gets the placeholder code BL05A.
            if (ADD_SIRINDHORN && srcCode.equals("BL05") && tgtCode.equals("BL06")) {
                Vertex sirin = g.insertVertex("BL05A", "Sirindhorn");
                g.insertEdge(u, sirin, type, line);
                g.insertEdge(sirin, v, type, line);
                u.addLine(line);
                v.addLine(line);
                sirin.addLine(line);
                continue;
            }

            g.insertEdge(u, v, type, line);

            // Only Line Track rows name a real line; Interchange rows
            // contain a description instead.
            if (type.equals("Line Track")) {
                u.addLine(line);
                v.addLine(line);
            }
        }
        br.close();
        return g;
    }
}
