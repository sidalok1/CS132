package utils;

public final class Edge {
    private final String from;
    private final String to;
    public Edge(String from, String to) {
        this.from = from;
        this.to = to;
    }
    public String from() { return from; }
    public String to() { return to; }
    public String toString() { return from + " -> " + to; }
}
