package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Graph extends HashMap<String, String> {
    /**
     * A graph intended for representing class inheritance relations.
     * Java (and therefore minijava) forbids single inheritance, so
     * only one outgoing edge is supported
     * @param pairs Edge: (Optional) edges that make up the graph
     */
    public Graph( Iterable<Edge> pairs ) {
        super();
        for ( Edge edge : pairs ) {
            this.put( edge.from(), edge.to() );
        }
    }

    /**
     * Add an edge to the graph
     * @param e: Edge
     */
    public void add( Edge e ) {
        this.put( e.from(), e.to() );
    }

    /**
     * List of all edges in this grah
     * @return List of Edges
     */
    public ArrayList<Edge> E() {
        ArrayList<Edge> edges = new ArrayList<>();
        for ( Entry<String, String> e : this.entrySet() ) {
            edges.add( new Edge( e.getKey(), e.getValue() ) );
        }
        return edges;
    }

    /**
     * Set of every node connected to an edge (which is every node)
     * @return Set of String, each representing a node
     */
    public HashSet<String> V() {
        HashSet<String> nodes = new HashSet<>();
        for ( Entry<String, String> e : this.entrySet() ) {
            nodes.add( e.getValue() );
            nodes.add( e.getKey() );
        }
        return nodes;
    }

    /**
     * All edges such that there exists at least one directed edge
     * @return Set of strings representing nodes
     */
    public HashSet<String> Internals() {
        return new HashSet<>( this.keySet() );
    }

    /**
     * All edges such that there exists no incomind directed edges
     * @return Set of strings representing nodes
     */
    public HashSet<String> Externals() {
        HashSet<String> internals = this.Internals();
        HashSet<String> difference = new HashSet<>( this.V() );
        difference.removeAll( internals );
        return difference;
    }

}
