public class Move {
    public final State to;
    public final int minutes;
    public final int interchanges; // 1 if this move is a line change, else 0
    public final boolean isTransfer; // true = walk/cross platform, false = ride

    public Move(State to, int minutes, int interchanges, boolean isTransfer) {
        this.to = to;
        this.minutes = minutes;
        this.interchanges = interchanges;
        this.isTransfer = isTransfer;
    }
}
