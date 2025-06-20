package hw4utils;

import java.util.Comparator;

public class IntervalComparator implements Comparator<Interval> {
    @Override
    public int compare(Interval o1, Interval o2) {
        int cmp;
        switch (this.type) {
            case START:
                cmp = o1.start - o2.start;
                break;
            case STOP:
                cmp = o1.stop - o2.stop;
                break;
            case LENGTH:
                cmp = o1.length() - o2.length();
                break;
            case END:
                cmp = o2.stop - o1.stop;
                break;
            default:
                cmp = 0;
                break;
        }
        return (cmp != 0) ? cmp : o1.id.toString().compareTo(o2.id.toString());
    }

    public static enum Type {
        START, STOP, LENGTH, END
    }
    private final Type type;
    public IntervalComparator(Type type) {
        this.type = type;
    }
}
