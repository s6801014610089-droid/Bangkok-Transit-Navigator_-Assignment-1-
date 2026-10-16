import java.util.*;

public class MinHeap<T> {
    private List<T> data = new ArrayList<>();
    private Comparator<T> cmp;

    public MinHeap(Comparator<T> cmp) {
        this.cmp = cmp;
    }

    public boolean isEmpty() { return data.isEmpty(); }
    public int size() { return data.size(); }
    public T peekMin() { return data.get(0); }

    private void swap(int i, int j) {
        T tmp = data.get(i);
        data.set(i, data.get(j));
        data.set(j, tmp);
    }

    public void insert(T item) {
        data.add(item);
        upHeap(data.size() - 1);
    }

    public T removeMin() {
        T min = data.get(0);
        T last = data.remove(data.size() - 1);
        if (!data.isEmpty()) {
            data.set(0, last);
            downHeap(0);
        }
        return min;
    }

    private void upHeap(int i) {
        while (i > 0) {
            int p = (i - 1) / 2;
            if (cmp.compare(data.get(i), data.get(p)) < 0) {
                swap(i, p);
                i = p;
            } else {
                break;
            }
        }
    }

    private void downHeap(int i) {
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            if (left >= data.size()) {
                break;
            }
            int smallest = left;
            if (right < data.size() && cmp.compare(data.get(right), data.get(left)) < 0) {
                smallest = right;
            }
            if (cmp.compare(data.get(smallest), data.get(i)) < 0) {
                swap(i, smallest);
                i = smallest;
            } else {
                break;
            }
        }
    }
}
