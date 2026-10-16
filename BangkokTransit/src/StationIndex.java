import java.util.*;

// The list of choices for the dropdowns.
// Most stations are listed by name. A name with two codes (Bang Wa, Tao Poon)
// is listed once per platform, e.g. "Bang Wa (S12)" and "Bang Wa (BL33)",
// so the rider chooses which platform they start or end at.
public class StationIndex {
    private final Map<String, List<String>> codesByName = new TreeMap<>();

    public StationIndex(Graph g) {
        Map<String, List<Vertex>> byName = new HashMap<>();
        for (Vertex v : g.getVertices()) {
            byName.computeIfAbsent(v.getName(), k -> new ArrayList<>()).add(v);
        }
        for (Map.Entry<String, List<Vertex>> e : byName.entrySet()) {
            List<Vertex> list = e.getValue();
            if (list.size() == 1) {
                codesByName.put(e.getKey(), new ArrayList<>(List.of(list.get(0).getCode())));
            } else {
                for (Vertex v : list) {
                    codesByName.put(e.getKey() + " (" + v.getCode() + ")",
                            new ArrayList<>(List.of(v.getCode())));
                }
            }
        }
    }

    public Set<String> names() { return codesByName.keySet(); }

    // null if the name is unknown
    public List<String> codesOf(String name) { return codesByName.get(name); }
}