import java.nio.file.*;

// Finds the folder that holds bangkok_network_graph.csv (and the "web" folder).
// It tries a few common places, so no full path like C:/Users/... is needed.
public class ProjectDir {
    static final String CSV = "bangkok_network_graph.csv";

    // Only fill this in if the automatic search fails on your computer.
    static final String MANUAL_DIR = "";

    public static Path find() {
        String[] candidates = { MANUAL_DIR, ".", "..", "BangkokTransit", "../BangkokTransit" };
        for (String c : candidates) {
            if (c.isEmpty()) continue;
            Path p = Paths.get(c);
            if (Files.exists(p.resolve(CSV))) {
                return p.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("Cannot find " + CSV
                + ". Put it in the project folder, or set MANUAL_DIR in ProjectDir.java.");
    }

    public static String csvPath() {
        return find().resolve(CSV).toString();
    }
}
