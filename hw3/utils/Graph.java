package utils;

import minijava.syntaxtree.*;
import minijava.visitor.GJNoArguDepthFirst;

import java.util.*;

public class Graph extends HashMap<String, String> {
    public static class GraphVisitor extends GJNoArguDepthFirst<Graph> {
        public Graph visit(Goal n) {return n.f1.accept(this); }
        public Graph visit(NodeListOptional n) {
            Graph g = new Graph();
            for (Node node : n.nodes) {
                Graph ng = node.accept(this);
                g.putAll(ng);
            }
            return g;
        }
        public Graph visit(TypeDeclaration n) { return n.f0.choice.accept(this); }
        public Graph visit(ClassDeclaration n) { return new Graph(); }
        public Graph visit(ClassExtendsDeclaration n) {
            Graph g = new Graph();
            g.add(new Edge(n.f1.f0.tokenImage, n.f3.f0.tokenImage));
            return g;
        }
    }
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
    public Graph() { super(); }
    public Graph( Goal n ) {
        super();
        Graph g = (new GraphVisitor()).visit(n);
        for ( Edge edge : g.E() ) {
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
