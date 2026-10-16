import java.util.*;

public class HeapTest {
    public static void main(String[] args) {
        MinHeap<Integer> h = new MinHeap<Integer>(Comparator.naturalOrder());
        h.insert(5);
        h.insert(3);
        h.insert(8);
        h.insert(1);
        h.insert(4);
        System.out.println("min = " + h.peekMin());
        System.out.println("size = " + h.size());

        System.out.print("sorted: ");
        while (!h.isEmpty()) {
            System.out.print(h.removeMin() + " ");
        }
        System.out.println();

        // extra check: 1000 random numbers must come out in sorted order
        MinHeap<Integer> big = new MinHeap<Integer>(Comparator.naturalOrder());
        Random rnd = new Random(1);
        for (int k = 0; k < 1000; k++) {
            big.insert(rnd.nextInt(10000));
        }
        boolean ok = true;
        int prev = -1;
        while (!big.isEmpty()) {
            int x = big.removeMin();
            if (x < prev) {
                ok = false;
            }
            prev = x;
        }
        System.out.println("1000 random numbers sorted correctly: " + ok);
    }
}
