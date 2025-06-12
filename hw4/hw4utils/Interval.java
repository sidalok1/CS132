package hw4utils;
import IR.token.Identifier;

public class Interval {
    public final int start, stop;
    public final Identifier id;
    public Reg reg;
    public Interval(int start, int stop, Identifier id) {
        this.start = start;
        this.stop = stop;
        this.id = id;
    }
    public int length() { return stop - start; }
}
