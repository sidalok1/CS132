import utils.*;
import visitor.DepthFirstVisitor;
import syntaxtree.*;
import visitor.GJNoArguDepthFirst;

import java.util.*;

public class TypeCheckVisitor extends DepthFirstVisitor {
    private TypeEnvironment A;
    private String C = null;
    private Graph inheritance;
    private Types returnType;
    private HashSet<String> declaredTypes;
    private HashMap<String, TypeEnvironment> globalFields;
    public void visit(NodeListOptional node) {
        for (Node n : node.nodes) {
            n.accept(this);
        }
    }
    public void visit(Goal node) {
        mainGoal = node;
        //Class names are distinct
        IDVisitor idVisitor = new IDVisitor();
        LinkedList<String> names = new LinkedList<>();
        names.add(classname(node.f0));
        for (Node n : node.f1.nodes) {
            names.add(n.accept((idVisitor)));
        }
        if (!distinct(names)) {
            throw new TypeCheckFailed("Type check failed at Goal :" + node.toString() + "(classnames were not distinct)");
        }
        declaredTypes = new HashSet<>(names);
        // Inheretance graph acyclic
        inheritance = (new GraphVisitor()).visit(node);
        if (!acyclic(inheritance)) {
            throw new TypeCheckFailed("Type check failed at Goal :" + node.toString() + "(not acyclic)");
        }
        globalFields = (new FieldMappingVisitor()).visit(node);
        // Main class type checks
        node.f0.accept(this);
        // Type declarations type check
        node.f1.accept(this);
    }
    public void visit(MainClass node) {
        // Field IDs are all distinct
        TypeEnvironment mainEnv = fields(classname(node));
        LinkedList<String> names = new LinkedList<>(mainEnv.keySet());
        if (!distinct(names)) {
            throw new TypeCheckFailed("Type check failed at MainClass :" + node.toString() + "(field IDs not distinct)");
        }
        // Statements type check
        A = mainEnv;
        C = classname(node);
        node.f15.accept(this);
    }
    public void visit(ClassDeclaration node) {
        // Field IDs and method names are all distinct
        TypeEnvironment env = fields(classname(node));
        LinkedList<String> names = new LinkedList<>(env.keySet());
        IDVisitor idVisitor = new IDVisitor();
        for (Node n : node.f4.nodes) {
            names.add(n.accept(idVisitor));
        }
        if (!distinct(names)) {
            throw new TypeCheckFailed(
                    "Type check failed at ClassDeclaration :" + node.toString() + "(IDs not distinct)");
        }

        C = classname(node);
        // methods type check
        node.f4.accept(this);
    }
    public void visit(ClassExtendsDeclaration node) {
        // Field IDs and method names are all distinct
        TypeEnvironment env = fields(classname(node));
        LinkedList<String> fields = new LinkedList<>(env.keySet());
        LinkedList<String> methods = new LinkedList<>();
        IDVisitor idVisitor = new IDVisitor();
        for (Node n : node.f6.nodes) {
            methods.add(n.accept(idVisitor));
        }
        LinkedList<String> names = new LinkedList<>();
        names.addAll(fields);
        names.addAll(methods);
        if (!distinct(names)) {
            throw new TypeCheckFailed(
                    "Type check failed at ClassExtendsDeclaration :" + node.toString() + "(IDs not distinct)");
        }
        // No overloading of methods
        for (String m : methods) {
            if (!noOverloading(node.f1.f0.tokenImage, node.f3.f0.tokenImage, m)) {
                throw new TypeCheckFailed(
                        "Type check failed at ClassExtendsDeclaration :" + node.toString() +
                                "(method " + m + " is overloaded)");
            }
        }
        // methods type check
        C = classname(node);
        node.f6.accept(this);
    }
    public void visit(MethodDeclaration node) {
        // Params and locals are distinct
        IDListVisitor idVisitor = new IDListVisitor();
        LinkedList<String> params = idVisitor.visit(node.f4);
        LinkedList<String> locals = idVisitor.visit(node.f7);
        LinkedList<String> names = new LinkedList<>(params);
        names.addAll(locals);
        if (!distinct(names)) {
            throw new TypeCheckFailed("Type check failed at MethodDeclaration :" + node.toString() + "(IDs not distinct)");
        }
        // Set new type environment A
        A = fields(C).merge((new TypeEnvVisitor()).visit(node));
        // Statements type check
        node.f8.accept(this);
        // Expression result is subtype of return value
        node.f10.accept(this);
        FindTypeVisitor findTypeVisitor = new FindTypeVisitor();
        Types declaredType = findTypeVisitor.visit(node.f1);
        if (!isSubtype(returnType, declaredType)) {
            throw new TypeCheckFailed("Type check failed at MethodDeclaration :" + node.toString() +
                    "\n(" + returnType.toString() + " is not a subtype of " + declaredType + ")");
        }
    }

    // Statement sub nodes
    public void visit(Block node) { node.f1.accept(this); } // Block type checks if inner statements type check
    public void visit(AssignmentStatement node) {
        Types t1 = getType(node.f0.f0.tokenImage);
        if ( t1.NULL() ) {
            throw new TypeCheckFailed("Type check failed at AssignmentStatement :" + node.toString() +
                    "\n(ID not in current type environment)");
        }
        node.f2.accept(this);
        Types t2 = returnType;
        if (!isSubtype(t2, t1)) {
            throw new TypeCheckFailed("Type check failed at statement: " + node.toString() +
                    "\n(expression returns " + t1.toString() + " not " + t2.toString() + ")");
        }
    }
    public void visit(ArrayAssignmentStatement node) {
        // Identifier refers to an array
        Types arrayType = A.get(ID(node.f0));
        if ( !arrayType.ARRAY() ) {
            throw new TypeCheckFailed("Type check failed at statement: " + node.toString() +
                    "\n(" + ID(node.f0) + " is a " + arrayType.toString() + " not an array)");
        }
        // Index expression returns an integer
        node.f2.accept(this);
        Types indexType = returnType;
        if ( !indexType.INT() ) {
            throw new TypeCheckFailed("Type check failed at statement: " + node.toString() +
                    "\n(index " + ID(node.f0) + " is a " + indexType.toString() + " not an int)");
        }
        // MiniJava only allows integer arrays, so expression must return integer
        node.f5.accept(this);
        Types valueType = returnType;
        if ( !valueType.INT() ) {
            throw new TypeCheckFailed("Type check failed at statement: " + node.toString() +
                    "\n(attempt to assign non integer expression to array)");
        }
    }
    public void visit(IfStatement node) {
        // Conditional expression must evaluate to a boolean
        node.f2.accept(this);
        Types conditionalType = returnType;
        if ( !conditionalType.BOOL() ) {
            throw new TypeCheckFailed("Type check failed at statement: " + node.toString() +
                    "\n(conditional expression returns " + conditionalType.toString() + " not boolean)");
        }
        // Type check both statements
        node.f4.accept(this);
        node.f6.accept(this);
    }
    public void visit(WhileStatement node) {
        node.f2.accept(this);
        Types conditionalType = returnType;
        if ( !conditionalType.BOOL() ) {
            throw new TypeCheckFailed("Type check failed at statement: " + node.toString() +
                    "\n(conditional expression returns " + conditionalType.toString() + " not boolean)");
        }
        // Type check body
        node.f4.accept(this);
    }
    public void visit(PrintStatement node) {
        node.f2.accept(this);
        Types printType = returnType;
        if ( !printType.INT() ) {
            throw new TypeCheckFailed("Type check failed at statement: " + node.toString() +
                    "\n(print expression returns " + printType.toString() + " not int)");
        }
    }

    // Expression or Primary Expression sub nodes
    public void visit(AndExpression node) {
        node.f0.accept(this);
        Types leftType = returnType;
        node.f2.accept(this);
        Types rightType = returnType;
        if ( !leftType.BOOL() || !rightType.BOOL() ) {
            throw new TypeCheckFailed("Type check failed at expression: " + node.toString() +
                    "\n(both sides of AND expressions must be boolean)");
        }
        returnType = new Types(Types.BOOL);
    }
    public void visit(CompareExpression node) {
        node.f0.accept(this);
        Types leftType = returnType;
        node.f2.accept(this);
        Types rightType = returnType;
        if ( !leftType.INT() || !rightType.INT() ) {
            throw new TypeCheckFailed("Type check failed at expression: " + node.toString() +
                    "\n(both sides of COMPARE expressions must be int)");
        }
        returnType = new Types(Types.BOOL);
    }
    public void visit(PlusExpression node) {
        node.f0.accept(this);
        Types leftType = returnType;
        node.f2.accept(this);
        Types rightType = returnType;
        if ( !leftType.INT() || !rightType.INT() ) {
            throw new TypeCheckFailed("Type check failed at expression: " + node.toString() +
                    "\n(both sides of ADD expressions must be int)");
        }
        returnType = new Types(Types.INT);
    }
    public void visit(MinusExpression node) {
        node.f0.accept(this);
        Types leftType = returnType;
        node.f2.accept(this);
        Types rightType = returnType;
        if ( !leftType.INT() || !rightType.INT() ) {
            throw new TypeCheckFailed("Type check failed at expression: " + node.toString() +
                    "\n(both sides of SUB expressions must be int)");
        }
        returnType = new Types(Types.INT);
    }
    public void visit(TimesExpression node) {
        node.f0.accept(this);
        Types leftType = returnType;
        node.f2.accept(this);
        Types rightType = returnType;
        if ( !leftType.INT() || !rightType.INT() ) {
            throw new TypeCheckFailed("Type check failed at expression: " + node.toString() +
                    "\n(both sides of MUL expressions must be int)");
        }
        returnType = new Types(Types.INT);
    }
    public void visit(ArrayLookup node) {
        node.f0.accept(this);
        Types arrayType = returnType;
        if ( !arrayType.ARRAY() ) {
            throw new TypeCheckFailed("Type check failed at expression: " + node.toString() +
                    "\n(attempt to index into a " + arrayType.toString() + ")");
        }
        node.f2.accept(this);
        Types indexType = returnType;
        if ( !indexType.INT() ) {
            throw new TypeCheckFailed("Type check failed at expression: " + node.toString() +
                    "\n(index expression returns " + indexType.toString() + " not int)");
        }
        returnType = new Types(Types.INT);
    }
    public void visit(ArrayLength node) {
        node.f0.accept(this);
        Types arrayType = returnType;
        if ( !arrayType.ARRAY() ) {
            throw new TypeCheckFailed("Type check failed at expression: " + node.toString() +
                    "\nlength method not defined on a " + arrayType.toString() + ")");
        }
        returnType = new Types(Types.INT);
    }
    public void visit(MessageSend node) {
        node.f0.accept(this);
        Types classType = returnType;
        if ( !classType.CLASS() ) {
            throw new TypeCheckFailed("Type check failed at method call: " + node.toString() +
                    "\n(class not defined in this context)");
        }
        String methodName = ID(node.f2);
        MethodType target = methodtype(classType.className(), methodName);
        if ( target == null ) {
            throw new TypeCheckFailed("Type check failed at method call: " + node.toString() +
                    "\n(no such mathod defined for " + classType.toString() + ")");
        }
        LinkedList<Expression> arguments = (new ExpressionListVisitor()).visit(node.f4);
        LinkedList<Types> argumentTypes = new LinkedList<>();
        for ( Expression arg : arguments ) {
            arg.accept(this);
            argumentTypes.add(returnType);
        }
        for ( Types paramType : target.parameters().values() ) {
            Types argType = argumentTypes.pollFirst();
            if ( argType == null || !isSubtype(argType, paramType) ) {
                throw new TypeCheckFailed("Type check failed at method call: " + node.toString() +
                        "\n(arg is null or is not a subtype of " + returnType.toString() + ")");
            }
        }
        returnType = target.returnType();
    }
    // PrimaryExpression visit methods
    public void visit(IntegerLiteral node) { returnType = new Types(Types.INT); }
    public void visit(TrueLiteral node) { returnType = new Types(Types.BOOL); }
    public void visit(FalseLiteral node) { returnType = new Types(Types.BOOL); }
    public void visit(Identifier node) {
        Types t = getType(ID(node));
        if ( t.NULL() ) {
            throw new TypeCheckFailed("Type check failed at primary expression: " + node.toString() +
                    "\n(" + ID(node) + " not defined in this context)");
        } else {
            returnType = t;
        }
    }
    public void visit(ThisExpression node) {
        if (C != null) {
            returnType = new Types(Types.CLASS, C);
        } else {
            throw new TypeCheckFailed("Type check failed at this expression: " + node.toString() +
                    "\n('this' keyword used outside of a method definition)");
        }
    }
    public void visit(ArrayAllocationExpression node) {
        node.f3.accept(this);
        if ( !returnType.INT() ) {
            throw new TypeCheckFailed("Type check failed at array allocation expression: " + node.toString() +
                    "\n(expression does not return an int)");
        } else {
            returnType = new Types(Types.ARRAY);
        }
    }
    public void visit(AllocationExpression node) {
        if ( !declaredTypes.contains(ID(node.f1)) ) {
            throw new TypeCheckFailed("Type check failed at allocation expression: " + node.toString() +
                    "\n(" + ID(node.f1) + "not defined)");
        }
        returnType = new Types(Types.CLASS, ID(node.f1));
    }
    public void visit(NotExpression node) {
        node.f1.accept(this);
        if ( !returnType.BOOL() ) {
            throw new TypeCheckFailed("Type check failed at not expression: " + node.toString() +
                    "\n(expression does not return a bool)");
        }
    }
    public void visit(BracketExpression node) {
        node.f1.accept(this);
        // return type set implicitly
    }


    // ----------------------------HELPER FUNCTIONS----------------------------

    public Types getType(String id) {
        if ( !A.containsKey(id) && !declaredTypes.contains(id) ) {
            return new Types(Types.NULL);
        } else {
            return A.getOrDefault(id, new Types(Types.CLASS, id));
        }
    }

    public static String ID(Identifier node) {
        return node.f0.tokenImage;
    }

    public boolean isSubtype(Types t, Types tprime) {
        if (t.CLASS() && tprime.CLASS()) {
            String tClassName = t.className();
            String tPrimeClassName = tprime.className();
            HashSet<String> cache = new HashSet<>();
            cache.add(tPrimeClassName);
            try {
                dfs(inheritance, tClassName, cache);
            } catch (CycleFound e) {
                return true;
            }
            return false;
        }
        return t.equals(tprime);
    }

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
        }
    }

    static class ExtendsVisitor extends GJNoArguDepthFirst<String> {
        public String visit(TypeDeclaration n) { return n.f0.choice.accept(this); }
        public String visit(ClassExtendsDeclaration n) { return n.f3.f0.tokenImage; }
        public String visit(ClassDeclaration n) { return null; }
    }

    static class IDListVisitor extends GJNoArguDepthFirst<LinkedList<String>> {
        static IDVisitor idVisitor = new IDVisitor();
        public LinkedList<String> visit(NodeListOptional node) {
            LinkedList<String> list = new LinkedList<>();
            for (Node n : node.nodes) {
                list.add(n.accept(idVisitor));
            }
            return list;
        }
        public LinkedList<String> visit(VarDeclaration n) {
            LinkedList<String> list = new LinkedList<>();
            list.add(n.accept(idVisitor));
            return list;
        }
        public LinkedList<String> visit(NodeOptional node) {
            LinkedList<String> list = new LinkedList<>();
            if (node.present()) {
                list.addAll(node.node.accept(this));
            }
            return list;
        }
        public LinkedList<String> visit(FormalParameterList n) {
            LinkedList<String> list = new LinkedList<>();
            list.add(n.f0.accept(idVisitor));
            list.addAll(n.f1.accept(this));
            return list;
        }
    }

    static class IDVisitor extends GJNoArguDepthFirst<String> {
        public String visit(MainClass n) { return n.f1.f0.tokenImage; }
        public String visit(TypeDeclaration n) { return n.f0.accept(this); }
        public String visit(ClassDeclaration n) { return n.f1.f0.tokenImage; }
        public String visit(ClassExtendsDeclaration n) { return n.f1.f0.tokenImage; }
        public String visit(VarDeclaration n) { return n.f1.f0.tokenImage; }
        public String visit(FormalParameter n) { return n.f1.f0.tokenImage; }
        public String visit (FormalParameterRest n) { return n.f1.accept(this); }
        public String visit(MethodDeclaration n) { return n.f2.f0.tokenImage; }
    }
    class FindTypeVisitor extends GJNoArguDepthFirst<Types> {
        public Types visit(VarDeclaration n) { return n.f0.f0.choice.accept(this); }
        public Types visit(Type n) { return n.f0.choice.accept(this); }
        public Types visit(NodeChoice n) { return n.choice.accept(this); }
        public Types visit(IntegerType n) { return new Types(Types.INT); }
        public Types visit(BooleanType n) { return new Types(Types.BOOL); }
        public Types visit(ArrayType n) { return new Types(Types.ARRAY); }
        public Types visit(Identifier n) {
            if ( !declaredTypes.contains(ID(n)) ) {
                throw new TypeCheckFailed("Type not declared: " + ID(n));
            } else {
                return new Types(Types.CLASS, ID(n));
            }
        }
        public Types visit(FormalParameter n) { return visit(n.f0); }
    }

    class TypeEnvVisitor extends GJNoArguDepthFirst<TypeEnvironment> {
        public TypeEnvironment visit(TypeDeclaration n) { return n.f0.accept(this); }
        public TypeEnvironment visit(ClassDeclaration c) {
            TypeEnvironment fields = new TypeEnvironment();
            FindTypeVisitor v1 = new FindTypeVisitor();
            IDVisitor v2 = new IDVisitor();
            for (Node n : c.f3.nodes) {
                Types t = n.accept(v1);
                String id = n.accept(v2);
                fields.put(id, t);
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
                fields.put(id, t);
            }
            return fields;
        }
        public TypeEnvironment visit(MethodDeclaration m) {
            TypeEnvironment fields = m.f4.accept(this);
            return fields.merge(m.f7.accept(this));
        }
        public TypeEnvironment visit(FormalParameterList n) {
            TypeEnvironment fields = n.f0.accept(this);
            return fields.merge(n.f1.accept(this));
        }
        public TypeEnvironment visit(NodeOptional n) {
            if (n.present()) {
                return n.node.accept(this);
            } else {
                return new TypeEnvironment();
            }
        }
        public TypeEnvironment visit(NodeListOptional n) {
            TypeEnvironment fields = new TypeEnvironment();
            for ( Node node : n.nodes ) {
                fields = fields.merge(node.accept(this));
            }
            return fields;
        }
        public TypeEnvironment visit(VarDeclaration n) {
            TypeEnvironment fields = new TypeEnvironment();
            FindTypeVisitor v1 = new FindTypeVisitor();
            IDVisitor v2 = new IDVisitor();
            Types t = n.accept(v1);
            String id = n.accept(v2);
            fields.put(id, t);
            return fields;
        }
        public TypeEnvironment visit(FormalParameter n) {
            TypeEnvironment fields = new TypeEnvironment();
            FindTypeVisitor v1 = new FindTypeVisitor();
            IDVisitor v2 = new IDVisitor();
            Types t = n.accept(v1);
            String id = n.accept(v2);
            fields.put(id, t);
            return fields;
        }
        public TypeEnvironment visit(FormalParameterRest n) {
            return n.f1.accept(this);
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


    private TypeEnvironment fields(String C) {
        return globalFields.getOrDefault(C, new TypeEnvironment());
    }
    class FieldMappingVisitor extends GJNoArguDepthFirst<HashMap<String, TypeEnvironment>> {
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
            if ( !acyclic(inheritanceGraph) ) {
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
                    env = env.merge(baseMap.get(cls));
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

    static class ExpressionListVisitor extends GJNoArguDepthFirst<LinkedList<Expression>> {
        public LinkedList<Expression> visit(Expression node) {
            LinkedList<Expression> expressions = new LinkedList<>();
            expressions.add(node);
            return expressions;
        }
        public LinkedList<Expression> visit(ExpressionList node) {
            LinkedList<Expression> expressions = new LinkedList<>();
            expressions.add(node.f0);
            for ( Node n : node.f1.nodes ) {
                expressions.addAll(n.accept(this));
            }
            return expressions;
        }
        public LinkedList<Expression> visit(ExpressionRest node) {
            LinkedList<Expression> expressions = new LinkedList<>();
            expressions.add(node.f1);
            return expressions;
        }
        public LinkedList<Expression> visit(NodeOptional node) {
            LinkedList<Expression> expressions = new LinkedList<>();
            if ( node.present() ) {
                expressions.addAll(node.node.accept(this));
            }
            return expressions;
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
            for ( Node n : node.f1.nodes ) {
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

    MethodType methodtypeconstruct(MethodDeclaration node) {
        FindTypeVisitor ft = new FindTypeVisitor();
        LinkedHashMap<String, Types> params = new LinkedHashMap<>();
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

    MethodType methodtype(String classname, String methodname) {
        HashSet<MethodDeclaration> methods = null;
        IDVisitor v = new IDVisitor();
        for ( ClassDeclaration cls : (new ClassVisitor()).visit(mainGoal)) {
            if (Objects.equals(v.visit(cls), classname)) {
                methods = (new MethodVisitor()).visit(cls);
                for (MethodDeclaration md : methods) {
                    if (Objects.equals(methodname(md), methodname)) {
                        return methodtypeconstruct(md);
                    }
                }
            }
        }
        if (methods == null) {
            for ( ClassExtendsDeclaration ecls : (new ClassExtendsVisitor()).visit(mainGoal)) {
                methods = (new MethodVisitor()).visit(ecls);
                for (MethodDeclaration md : methods) {
                    if (Objects.equals(methodname(md), methodname)) {
                        return methodtypeconstruct(md);
                    }
                }
                return methodtype(ID(ecls.f3), methodname);
            }
        }
        return null;
    }

    boolean noOverloading (String class1, String class2, String methodname) {
        MethodType m1 = methodtype(class1, methodname);
        if (m1 != null) {
            MethodType m2 = methodtype(class2, methodname);
            return ( m2 == null || m1.equals(m2) );
        } else { return true; }
    }

    static class VisitorError extends RuntimeException {};
}
