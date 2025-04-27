import utils.*;
import visitor.DepthFirstVisitor;
import syntaxtree.*;
import visitor.GJNoArguDepthFirst;

import java.util.*;

public class TypeCheckVisitor extends DepthFirstVisitor {
    /*@Override
    public void visit(Goal node) {
        //Type check Goal
        //  ⊢ g
        *//*
         * from -> MainClass()
         * to -> ( TypeDeclaration() )*
         * f2 -> <EOF>
         *//*
        node.from.accept(this);
        node.to.accept(this);
        node.f2.accept(this);
    }*/

    // ----------------------------HELPER FUNCTIONS----------------------------

    // classname
    public static String classname(MainClass c) {
        return c.f1.f0.tokenImage;
    }
    public static String classname(ClassDeclaration c) {
        return c.f1.f0.tokenImage;
    }
    public static String classname(ClassExtendsDeclaration c) {
        return c.f1.f0.tokenImage;
    }

    // linkset
    public static HashSet<Edge> linkset(MainClass c) { return new HashSet<>(); }
    public static HashSet<Edge> linkset(ClassDeclaration c) { return new HashSet<>();}
    public static HashSet<Edge> linkset(ClassExtendsDeclaration c) {
        Edge p = new Edge(c.f1.f0.tokenImage, c.f3.f0.tokenImage);
        HashSet<Edge> set = new HashSet<>();
        set.add(p);
        return set;
    }

    // methodname
    public static String methodname(MethodDeclaration m) { return m.f2.f0.tokenImage; }

    // distinct
    public static boolean distinct(List<String> ids) {
        HashSet<String> set = new HashSet<>(ids);
        return set.size() == ids.size();
    }

    // acyclic
    public static boolean acyclic(Graph g) {
        try {
            Graph g_ = new Graph(g.E());
            for (String root : g.keySet()) {
                dfs(g_, root, new HashSet<String>());
                g_.remove(root);
                /*
                If root is part of cycle, dfs is guaranteed to throw CycleFound.
                Removing root can not destroy cycle (and certainly cannot create
                one). In Even if by a little bit, removing should speed up graph
                dfs.
                 */
            }
        } catch (CycleFound e) {
            return false;
        }
        return true;
    }
    static class CycleFound extends Throwable {}
    private static void dfs( Graph g, String root, HashSet<String> cache ) throws CycleFound {
        if ( cache.contains( root ) ) {
            throw new CycleFound();
        }
        HashSet<String> newCache = new HashSet<>( cache );
        newCache.add( root );
        String neighbor = g.get( root );
        if ( neighbor != null ) {
            dfs( g, neighbor, newCache );
        } else {
            throw new TypeCheckFailed("No outgoing edge from: " + root);
        }
    }

    static class ExtendsVisitor extends GJNoArguDepthFirst<String> {
        public String visit(ClassExtendsDeclaration n) { return n.f3.f0.tokenImage; }
        public String visit(ClassDeclaration n) { return null; }
    }

    static class IDVisitor extends GJNoArguDepthFirst<String> {
        public String visit(MainClass n) { return n.f1.f0.tokenImage; }
        public String visit(ClassDeclaration n) { return n.f1.f0.tokenImage; }
        public String visit(ClassExtendsDeclaration n) { return n.f1.f0.tokenImage; }
        public String visit(VarDeclaration n) { return n.f1.f0.tokenImage; }
    }
    static class FindTypeVisitor extends GJNoArguDepthFirst<Types> {
        public Types visit(VarDeclaration n) { return n.f0.f0.choice.accept(this); }
        public Types visit(Type n) { return n.f0.choice.accept(this); }
        public Types visit(NodeChoice n) { return n.choice.accept(this); }
        public Types visit(IntegerType n) { return Types.INT; }
        public Types visit(BooleanType n) { return Types.BOOL; }
        public Types visit(ArrayType n) { return Types.ARRAY; }
        public Types visit(Identifier n) {
            Types t = Types.CLASS;
            t.setClassName(n.f0.tokenImage);
            return t;
        }
        public Types visit(FormalParameter n) { return visit(n.f0); }
    }

    static class TypeEnvVisitor extends GJNoArguDepthFirst<TypeEnvironment> {
        public TypeEnvironment visit(ClassDeclaration c) {
            TypeEnvironment fields = new TypeEnvironment();
            for (Node n : c.f3.nodes) {
                FindTypeVisitor v1 = new FindTypeVisitor();
                IDVisitor v2 = new IDVisitor();
                Types t = n.accept(v1);
                String id = n.accept(v2);
                fields.push(id, t);
            }
            return fields;
        }
        public TypeEnvironment visit(ClassExtendsDeclaration c) {
            TypeEnvironment fields = new TypeEnvironment();
            for (Node n : c.f5.nodes) {
                FindTypeVisitor v1 = new FindTypeVisitor();
                IDVisitor v2 = new IDVisitor();
                Types t = n.accept(v1);
                String id = n.accept(v2);
                fields.push(id, t);
            }
            return fields;
        }
    }

    static class GraphVisitor extends GJNoArguDepthFirst<Graph> {
        public Graph visit(Goal n) {return n.f1.accept(this); }
        public Graph visit(NodeListOptional n) {
            Stack<Edge> edges = new Stack<>();
            for (Node node : n.nodes) {
                IDVisitor v1 = new IDVisitor();
                String id = node.accept(v1);
                ExtendsVisitor v3 = new ExtendsVisitor();
                String superclassID = node.accept(v3);
                if (superclassID != null) {
                    edges.push(new Edge(id, superclassID));
                }
            }
            return new Graph(edges);
        }
    }

    private HashMap<String, TypeEnvironment> globalFields;

    static class FieldMappingVisitor extends GJNoArguDepthFirst<HashMap<String, TypeEnvironment>> {
        public HashMap<String, TypeEnvironment> visit( Goal n ) { return n.f1.accept( this ); }
        public HashMap<String, TypeEnvironment> visit( NodeListOptional n ) {
            TypeEnvVisitor v0 = new TypeEnvVisitor();
            GraphVisitor v1 = new GraphVisitor();
            IDVisitor v2 = new IDVisitor();
            HashMap<String, TypeEnvironment> fieldMap = new HashMap<>();
            HashMap<String, TypeEnvironment> baseMap = new HashMap<>();
            for (Node node : n.nodes) {
                String id = node.accept( v2 );
                TypeEnvironment env = node.accept( v0 );
                baseMap.put(id, env);
            }
            Graph inheritanceGraph = n.accept( v1 );
            if ( acyclic(inheritanceGraph) ) {
                throw new TypeCheckFailed("Inheritance graph not acyclic");
            }
            for ( String id : baseMap.keySet() ) {
                Stack<String> superclasses = new Stack<>();
                String next = id;
                while ( next != null ) {
                    superclasses.push( next );
                    next = inheritanceGraph.get( next );
                }
                TypeEnvironment env = new TypeEnvironment();
                while ( !superclasses.empty() ) {
                    String cls = superclasses.pop();
                    env.merge(baseMap.get(cls));
                }
                fieldMap.put(id, env);
            }
            return fieldMap;
        }
    }

    protected static Goal mainGoal;
    static class ClassVisitor extends GJNoArguDepthFirst<HashSet<ClassDeclaration>> {
        public HashSet<ClassDeclaration> visit(Goal node) {
            HashSet<ClassDeclaration> classes = new HashSet<>();
            for ( Node typeDec : node.f1.nodes ) {
                classes.addAll( typeDec.accept(this) );
            }
            return classes;
        }
        public HashSet<ClassDeclaration> visit(TypeDeclaration node) { return node.f0.accept(this); }
        public HashSet<ClassDeclaration> visit(ClassDeclaration node) {
            HashSet<ClassDeclaration> classes = new HashSet<>();
            classes.add(node);
            return classes;
        }
        public HashSet<ClassDeclaration> visit(ClassExtendsDeclaration node) {
            return new HashSet<>();
        }
    }
    static class ClassExtendsVisitor extends GJNoArguDepthFirst<HashSet<ClassExtendsDeclaration>> {
        public HashSet<ClassExtendsDeclaration> visit(Goal node) {
            HashSet<ClassExtendsDeclaration> classes = new HashSet<>();
            for ( Node typeDec : node.f1.nodes ) {
                classes.addAll( typeDec.accept(this) );
            }
            return classes;
        }
        public HashSet<ClassExtendsDeclaration> visit(TypeDeclaration node) { return node.f0.accept(this); }
        public HashSet<ClassExtendsDeclaration> visit(ClassExtendsDeclaration node) {
            HashSet<ClassExtendsDeclaration> classes = new HashSet<>();
            classes.add(node);
            return classes;
        }
        public HashSet<ClassExtendsDeclaration> visit(ClassDeclaration node) {
            return new HashSet<>();
        }
    }
    static class MethodVisitor extends GJNoArguDepthFirst<HashSet<MethodDeclaration>> {
        public HashSet<MethodDeclaration> visit(MethodDeclaration node) {
            HashSet<MethodDeclaration> methods = new HashSet<>();
            methods.add(node);
            return methods;
        }
        public HashSet<MethodDeclaration> visit(ClassDeclaration node) {
            HashSet<MethodDeclaration> methods = new HashSet<>();
            for (Node n : node.f4.nodes) {
                methods.addAll(n.accept(this));
            }
            return methods;
        }
        public HashSet<MethodDeclaration> visit(ClassExtendsDeclaration node) {
            HashSet<MethodDeclaration> methods = new HashSet<>();
            for (Node n : node.f6.nodes) {
                methods.addAll(n.accept(this));
            }
            return methods;
        }
    }
    static class FieldsVisitor extends GJNoArguDepthFirst<HashSet<VarDeclaration>> {
        public HashSet<VarDeclaration> visit(ClassDeclaration node) {
            HashSet<VarDeclaration> fields = new HashSet<>();
            for (Node n : node.f3.nodes ) {
                fields.addAll(n.accept(this));
            }
            return fields;
        }
        public HashSet<VarDeclaration> visit(ClassExtendsDeclaration node) {
            HashSet<VarDeclaration> fields = new HashSet<>();
            for (Node n : node.f5.nodes ) {
                fields.addAll(n.accept(this));
            }
            return fields;
        }
        public HashSet<VarDeclaration> visit(VarDeclaration node) {
            HashSet<VarDeclaration> fields = new HashSet<>();
            fields.add(node);
            return fields;
        }
    }

    static class FormalParametersVisitor extends GJNoArguDepthFirst<LinkedList<FormalParameter>> {
        public LinkedList<FormalParameter> visit(FormalParameter node) {
            LinkedList<FormalParameter> lst = new LinkedList<>();
            lst.add(node);
            return lst;
        }
        public LinkedList<FormalParameter> visit(FormalParameterList node) {
            LinkedList<FormalParameter> lst = new LinkedList<>();
            lst.add(node.f0);
            for (Node n : node.f1.nodes ) {
                lst.addAll(n.accept(this));
            }
            return lst;
        }
        public LinkedList<FormalParameter> visit(FormalParameterRest node) {
            LinkedList<FormalParameter> lst = new LinkedList<>();
            lst.add(node.f1);
            return lst;
        }
        public LinkedList<FormalParameter> visit(NodeOptional node) {
            LinkedList<FormalParameter> lst = new LinkedList<>();
            if (node.present()) {
                lst.addAll(node.node.accept(this));
            }
            return lst;
        }
    }

    static MethodType methodtypeconstruct(MethodDeclaration node) {
        FindTypeVisitor ft = new FindTypeVisitor();
        HashMap<String, Types> params = new HashMap<>();
        if (node.f4.present()) {
            FormalParametersVisitor fpv = new FormalParametersVisitor();
            for ( FormalParameter p : node.f4.accept(fpv)) {
                Types t = ft.visit(p);
                params.put(p.f1.f0.tokenImage, p.f0.accept(ft));
            }
        }
        Types returnType = ft.visit(node.f1);
        return new MethodType(params, returnType);
    }

    static MethodType methodtype(String classname, String methodname) {
        MethodType m = null;
        HashSet<MethodDeclaration> methods = null;
        IDVisitor v = new IDVisitor();
        for ( ClassDeclaration cls : (new ClassVisitor()).visit(mainGoal)) {
            if (Objects.equals(v.visit(cls), classname)) {
                methods = (new MethodVisitor()).visit(cls);
            }
        }
        if (methods == null) {
            for ( ClassExtendsDeclaration ecls : (new ClassExtendsVisitor()).visit(mainGoal)) {
                methods = (new MethodVisitor()).visit(ecls);
            }
        }
        //
        if (methods != null) {
            for (MethodDeclaration md : methods) {
                if (Objects.equals(methodname(md), methodname)) {
                    m = methodtypeconstruct(md);
                }
            }
        }
        return m;
    }

    static boolean noOverloading (String class1, String class2, String methodname) {
        MethodType m1 = methodtype(class1, methodname);
        if (m1 != null) {
            MethodType m2 = methodtype(class2, methodname);
            return (m2 == null || m1 == m2);
        } else { return true; }
    }

    static class VisitorError extends RuntimeException {};
}
