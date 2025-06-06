package hw4utils;

public class Interval {
    public final int start, stop;
    public final String id;
    public Reg reg;
    public Interval(int start, int stop, String id) {
        this.start = start;
        this.stop = stop;
        this.id = id;
    }
    public int length() { return stop - start; }
}
