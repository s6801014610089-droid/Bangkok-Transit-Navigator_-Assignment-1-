# Bangkok Transit Navigator

Assignment 1/2026 — 010153103 Algorithms and Data Structures (KMUTNB)

A web app that plans routes on the Bangkok BTS/MRT network.
Graph ADT (adjacency list), own MinHeap, own Dijkstra, no graph libraries.

## What you need
- JDK 11 or newer (tested with the JDK in IntelliJ IDEA). No other libraries.
- A web browser.

## Folder layout
```
BangkokTransit/
  bangkok_network_graph.csv
  README.md
  src/    all .java files
  web/    index.html, map.js
```
`bangkok_network_graph.csv` and `web/` must be in the same folder.

## How to run (IntelliJ IDEA)
1. Open the project folder in IntelliJ.
2. Open `src/Server.java` and click the green Run arrow next to `main`.
3. The console prints `Server running. Open http://localhost:8080 in your browser.`
4. Open http://localhost:8080 in the browser.
5. Choose From / To, time, Depart at or Arrive by, Fastest or Fewest interchanges, then click **Find Route**.

The program finds the CSV and `web/` folder by itself. If it says
`Cannot find bangkok_network_graph.csv`, set `MANUAL_DIR` in `src/ProjectDir.java`
to the folder that holds the CSV (use `/` in the path).

## How to run (command line)
```
cd BangkokTransit
javac -d out src/*.java
java -cp out Server
```

## Other programs (for testing)
- `Main` – builds the graph and prints counts (98 stations, 102 edges, 99 states, 103 moves).
- `HeapTest`, `StateTest`, `DijkstraTest`, `ItineraryTest` – small checks printed to the console.

## Main files
| File | Job |
|---|---|
| `Graph`, `Vertex`, `Edge`, `GraphLoader` | Graph ADT (adjacency list) and CSV parsing / cleaning |
| `State`, `Move`, `StateGraph` | (station, line) states, so Siam can be modelled correctly |
| `MinHeap` | Own generic priority queue |
| `Dijkstra` | One Dijkstra, two cost functions (fastest, fewest interchanges) |
| `Planner`, `Itinerary`, `TimeUtil` | Depart-at / Arrive-by, itinerary text, time checks |
| `StationIndex`, `Server` | Station names for the web page, HTTP server and API |
| `web/index.html`, `web/map.js` | Web page and SVG map |

## Constants
- Ride between two neighbouring stations: 3 min
- Walking interchange (BTS <-> MRT): 6 min
- Cross-platform interchange at Siam (CEN): 5 min

## Notes
- Station identity is the station code; names are only for display.
  Bang Wa and Tao Poon have two codes each, so the pickers show `Bang Wa (S12)`, `Bang Wa (BL33)`, etc.
- Sirindhorn is added as station `BL05A` between BL05 and BL06.
  To turn it off, set `ADD_SIRINDHORN = false` in `GraphLoader.java`.
- The port is 8080. If it is busy, change `PORT` in `Server.java`.
