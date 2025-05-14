import minijava.syntaxtree.*;
import minijava.visitor.*;

import java.util.*;

import utils.*;

import sparrowCode.*;
import sparrowCode.Instruction.Type;

@SuppressWarnings("DuplicatedCode")
public class Translator extends DepthFirstVisitor {
    private final Goal goal;

    private Graph inheritance;
    private final HashMap<String, Typedef> typedefs = new HashMap<>();

    private final HashMap<String, HashSet<String>> defines = new HashMap<>();

    private Program program;
    private FunDecl funDecl;
    private final HashMap<String, Integer> methodTable = new HashMap<>();
    private HashMap<String, Integer> fields = new HashMap<>();
    private HashMap<String, String> env = new HashMap<>();
    private LinkedList<Instruction> instCache = new LinkedList<>();
    private String returnID;
    private String thisClass;
    public Translator(Goal root) {
        goal = root;
        inheritance = new Graph(root);
        analyzeTypes();
        root.accept(this);
    }
    public String getProgram() { return program.toString(); }


    //helpers
    private int p = 0;
    private String newParam() {
        String param = "p" + p;
        p++;
        return param;
    }
    private int v = 0;
    private String newVar() {
        String var = "v" + v;
        v++;
        return var;
    }
    private int e = 0;
    private String newTemp() {
        String temp = "e" + e;
        e++;
        return temp;
    }
    private int l = 0;
    private String newLabel() {
        String label = "l" + l;
        l++;
        return label;
    }
    private void analyzeTypes() {
        ClassVisitor cv = new ClassVisitor();
        ClassExtendsVisitor ev = new ClassExtendsVisitor();
        LinkedHashSet<String> order = new LinkedHashSet<>();
        for ( String outer : inheritance.keySet() ) {
            String cls = outer;
            while ( cls != null ) {
                order.add(cls);
                cls = inheritance.get(cls);
            }
        }
        Stack<String> s = new Stack<>();
        s.addAll(order);
        for ( Node n : goal.f1.nodes ) { // non-inherited classes
            String cls = n.accept(idVisitor);
            if ( cls != null && !order.contains(cls) ) { s.push(cls); }
        }
        while ( !s.isEmpty() ) {
            String cls = s.pop();
            Typedef classDef = new Typedef();
            Node c = findClass(cls);
            assert c != null;
            String superclass = c.accept(superclassVisitor);
            if ( superclass != null ) {
                Typedef superclassDef = typedefs.get(superclass);
                classDef.fields = new LinkedHashSet<>(superclassDef.fields);
                classDef.methods = new LinkedHashSet<>(superclassDef.methods);
            } else {
                classDef.fields = new LinkedHashSet<>();
                classDef.methods = new LinkedHashSet<>();
            }
            LinkedHashSet<MethodDeclaration> methods = c.accept(methodVisitor);
            for ( MethodDeclaration m : methods ) {
                String mname = m.accept(idVisitor);
                defines.computeIfAbsent(cls, k -> new HashSet<>()).add(mname);
                classDef.methods.add(mname);
            }
            LinkedHashSet<VarDeclaration> fields = c.accept(fieldsVisitor);
            for ( VarDeclaration f : fields ) { classDef.fields.add(f.accept(idVisitor)); }
            typedefs.put(cls, classDef);
        }
        LinkedHashSet<String> allMethods = new LinkedHashSet<>();
        for ( Typedef tdef : typedefs.values() ) {
            allMethods.addAll(tdef.methods);
        }
        ArrayList<String> mstrings = new ArrayList<>(allMethods);
        for ( String method : mstrings ) { methodTable.put(method, mstrings.indexOf(method)); }
    }
    private Node findClass(String classname) {
        for (Node n : goal.f1.nodes) {
            String name = n.accept(idVisitor);
            if (name.equals(classname)) {
                return n;
            }
        }
        return null;
    }
    private String definedIn(String method, String classname) {
        assert method != null && classname != null;
        if (defines.get(classname).contains(method)) { return classname; }
        else {
            return definedIn(method, inheritance.get(classname));
        }
    }

    //Visitors
    private final IDVisitor idVisitor = new IDVisitor();
    private final MethodVisitor methodVisitor = new MethodVisitor();
    private final FieldsVisitor fieldsVisitor = new FieldsVisitor();
    private final SuperclassVisitor superclassVisitor = new SuperclassVisitor();

    public void visit(NodeListOptional n) {
        for (Node node : n.nodes) {
            node.accept(this);
        }
    }
    public void visit(Goal n) {
        program = new Program();
        n.f0.accept(this);
        n.f1.accept(this);
    }
    public void visit(MainClass n) {
        FunDecl f = new FunDecl();
        String main = idVisitor.visit(n.f1);
        env = new HashMap<>();
        LinkedList<String> params = new LinkedList<>(env.keySet());
        n.f14.accept(this);
        instCache = new LinkedList<>();
        n.f15.accept(this);
        CodeBlock b = new CodeBlock(instCache, returnID);
        f.FunctionName = "Main";
        f.block = b;
        f.paramIDs = params;
        program.functions.add(f);
    }
    public void visit(ClassExtendsDeclaration n) {
        String id = idVisitor.visit(n);
        thisClass = id;
        Typedef typedef = typedefs.get(id);
        FunDecl constructor = new FunDecl();
        CodeBlock initCode = new CodeBlock();
        fields = new HashMap<>();
        ArrayList<String> classFields = new ArrayList<>(typedef.fields);
        for ( String f : classFields ) {
            fields.put(f, classFields.indexOf(f));
        }
        for ( Node method : n.f6.nodes ) { method.accept(this); }

        String ret = newTemp();
        String mTable = newTemp();
        String e0 = newTemp();
        initCode.instructions.add(
                new Instruction(Type.INT, e0, Integer.toString((typedef.fields.size()+1)*4))
        );
        initCode.instructions.add(
                new Instruction(Type.ALLOC, ret, e0)
        );
        String e1 = newTemp();
        initCode.instructions.add(
                new Instruction(Type.INT, e1, Integer.toString((methodTable.size())*4))
        );
        initCode.instructions.add(
                new Instruction(Type.ALLOC, mTable, e1)
        );
        initCode.instructions.add(
                new Instruction(Type.ARRAY, ret, "0", mTable)
        );
        for ( String m : typedef.methods ) {
            // The logic here needs to change, since the methods need to have a specific order as defined by global
            // method table
            String definedInClass = definedIn(m, id);
            String e2  = newTemp();
            initCode.instructions.add(
                    new Instruction(Type.FUNC, e2, definedInClass + "_" + m)
            );
            int i = methodTable.get(m) * 4;
            initCode.instructions.add(
                    new Instruction(Type.ARRAY, mTable, Integer.toString(i), e2)
            );
        }
        initCode.returnID = ret;
        constructor.FunctionName = id;
        constructor.block = initCode;
        program.functions.add(constructor);
    }
    public void visit(ClassDeclaration n) {
        String id = idVisitor.visit(n);
        thisClass = id;
        Typedef typedef = typedefs.get(id);
        FunDecl constructor = new FunDecl();
        CodeBlock initCode = new CodeBlock();
        fields = new HashMap<>();
        ArrayList<String> classFields = new ArrayList<>(typedef.fields);
        for ( String f : classFields ) {
            fields.put(f, classFields.indexOf(f));
        }
        for ( Node method : n.f4.nodes ) { method.accept(this); }

        String ret = newTemp();
        String mTable = newTemp();
        String e0 = newTemp();
        initCode.instructions.add(
                new Instruction(Type.INT, e0, Integer.toString((typedef.fields.size()+1)*4))
        );
        initCode.instructions.add(
                new Instruction(Type.ALLOC, ret, e0)
        );
        String e1 = newTemp();
        initCode.instructions.add(
                new Instruction(Type.INT, e1, Integer.toString((methodTable.size())*4))
        );
        initCode.instructions.add(
                new Instruction(Type.ALLOC, mTable, e1)
        );
        initCode.instructions.add(
                new Instruction(Type.ARRAY, ret, "0", mTable)
        );
        for ( String m : typedef.methods ) {
            // Class does not extend another, all methods must be defined here
            String e2  = newTemp();
            initCode.instructions.add(
                    new Instruction(Type.FUNC, e2, id + "_" + m)
            );
            int i = methodTable.get(m) * 4;
            initCode.instructions.add(
                    new Instruction(Type.ARRAY, mTable, Integer.toString(i), e2)
            );
        }
        initCode.returnID = ret;
        constructor.FunctionName = id;
        constructor.block = initCode;
        program.functions.add(constructor);
    }



    public void visit(MethodDeclaration n) {
        FunDecl f = new FunDecl();
        String fname = idVisitor.visit(n);
        env = new HashMap<>();
        env.put("this", newParam());
        if ( n.f4.present() ) {
            n.f4.node.accept(this);
        }
        LinkedList<String> params = new LinkedList<>(env.keySet());
        n.f7.accept(this);
        instCache = new LinkedList<>();
        n.f8.accept(this);
        n.f10.accept(this);
        CodeBlock b = new CodeBlock(instCache, returnID);
        f.FunctionName = thisClass + "_" + fname;
        f.block = b;
        f.paramIDs = params;
        program.functions.add(f);
    }
    public void visit(FormalParameterRest n) { n.f1.accept(this); }
    public void visit(FormalParameter n) {
        String param = newParam();
        String id = idVisitor.visit(n);
        env.put(id, param);
    }
    public void visit(VarDeclaration n) {
        String var = newVar();
        String id = idVisitor.visit(n);
        env.put(id, var);
    }

    public void visit(Block n) {
        for ( Node node : n.f1.nodes ) {
            node.accept(this);
        }
    }

    public void visit(IfStatement n) {
        n.f2.accept(this);
        String e0 = returnID, lFalse = newLabel(), lEnd = newLabel();
        instCache.add(new Instruction(Type.IF0, e0, lFalse));
        n.f4.accept(this);
        instCache.add(new Instruction(Type.GOTO, lEnd));
        instCache.add(new Instruction(Type.LABEL, lFalse));
        n.f6.accept(this);
        instCache.add(new Instruction(Type.LABEL, lEnd));
    }

    public void visit(WhileStatement n) { // Done
        String lLoop = newLabel(), lEnd = newLabel();
        instCache.add(new Instruction(Type.LABEL, lLoop));
        n.f2.accept(this);
        String e0 = returnID;
        instCache.add(new Instruction(Type.IF0, e0, lEnd));
        n.f4.accept(this);
        instCache.add(new Instruction(Type.GOTO, lLoop));
        instCache.add(new Instruction(Type.LABEL, lEnd));
    }

    public void visit(PrintStatement n) { // Done
        LinkedList<Instruction> block = new LinkedList<>(instCache);
        n.f2.accept(this);
        instCache.add(new Instruction(Type.PRINT, returnID));
        block.addAll(instCache);
        instCache = block;
    }

    public void visit(AssignmentStatement n) { // Done
        n.f2.accept(this);
        String id = idVisitor.visit(n.f0);
        if ( env.containsKey(id) ) {
            String e0 = env.get(id);
            instCache.add(new Instruction(Type.ID, e0, returnID));
        } else {
            int i = (fields.get(id)+1)*4;
            instCache.add(new Instruction(Type.ARRAY, "this", Integer.toString(i), returnID));
        }
    }

    public void visit(ArrayAssignmentStatement n) {
        n.f0.accept(this);
        String id = returnID;

        n.f2.accept(this);
        String index = returnID;
        String e0 = newTemp();
        String e1 = newTemp();
        String e2 = newTemp();
        String one = newTemp();
        String four = newTemp();
        instCache.add(new Instruction(Type.INT, one, "1"));
        instCache.add(new Instruction(Type.INT, four, "4"));
        // Increment and mult for sparrow indexing
        // Need to check if out of bounds in minijava produces undefined behaviour or runtime exception
        instCache.add(new Instruction(Type.ADD, e0, index, one));
        instCache.add(new Instruction(Type.MULT, e1, e0, four));
        instCache.add(new Instruction(Type.ADD, e2, id, e1));
        n.f5.accept(this);
        String val = returnID;
        instCache.add(new Instruction(Type.ARRAY, e2, "0", val));
    }

    public void visit(ArrayLength n) { // Done
        n.f0.accept(this);
        String id = returnID;
        String ret = newTemp();
        instCache.add(new Instruction(Type.INDEX, ret, id, "0"));
        returnID = ret;
    }

    public void visit(ArrayLookup n) { // Implement out of bounds checking
        n.f0.accept(this);
        String e0 = returnID;
        n.f2.accept(this);
        String e1 = returnID;
        String one = newTemp();
        String four = newTemp();
        instCache.add(new Instruction(Type.INT, one, "1"));
        instCache.add(new Instruction(Type.INT, four, "4"));
        String e2 = newTemp();
        String e3 = newTemp();
        String e4 = newTemp();
        instCache.add(new Instruction(Type.ADD, e2, e1, one));
        instCache.add(new Instruction(Type.MULT, e3, e2, four));
        instCache.add(new Instruction(Type.ADD, e4, e3, e0));
        String ret = newTemp();
        instCache.add(new Instruction(Type.INDEX, ret, e4, "0"));
        returnID = ret;
    }

    public void visit(ArrayAllocationExpression n) { // Done
        String ret = newTemp();

        n.f3.accept(this);
        String zero = newTemp();
        String e1 = returnID;
        instCache.add(new Instruction(Type.INT, zero, "0"));
        String e2 = newTemp(), label = newLabel();
        instCache.add(new Instruction(Type.LESS, e2, e1, zero)); // e2 = ( e1 < zero ) ? 1 : 0
        instCache.add(new Instruction(Type.IF0, e2, label)); // if (e1 < 0):
        instCache.add(new Instruction(Type.ERROR, "Argument to alloc() must be positive")); // throw AllocError()
        instCache.add(new Instruction(Type.LABEL, label)); // else:
        String one = newTemp();
        instCache.add(new Instruction(Type.INT, one, "1"));
        String four = newTemp();
        instCache.add(new Instruction(Type.INT, four, "4"));
        String e3 = newTemp(), e4 = newTemp();
        // Add by one and mult by 4 to get sparrow array size (add one bc array[0] = array.length())
        instCache.add(new Instruction(Type.ADD, e3, e1, one));
        instCache.add(new Instruction(Type.MULT, e4, four, e3));
        instCache.add(new Instruction(Type.ALLOC, ret, e4));
        instCache.add(new Instruction(Type.ARRAY, ret, zero, e1));

        returnID = ret;
    }

    public void visit(AllocationExpression n) {
        String ret = newTemp();
        String cls = n.f1.accept(idVisitor);
        String e0 = newTemp();
        instCache.add(new Instruction(Type.FUNC, e0, cls));
        instCache.add(new Instruction(Type.CALL, ret, e0));
        returnID = ret;
    }

    class ListVisitor extends GJNoArguDepthFirst<NodeListOptional> {
        public NodeListOptional visit(NodeOptional n) {
            if ( n.present() ) return n.node.accept(this);
            return null;
        }
        public NodeListOptional visit(NodeListOptional n) { return n; }
    }

    public void visit(MessageSend n) {
        String ret = newTemp();
        n.f0.accept(this);
        String cls = returnID;
        String method = n.f2.accept(idVisitor);
        String mtable = newTemp();
        instCache.add(new Instruction(Type.INDEX, mtable, cls, "0"));
        String func = newTemp();
        int i = methodTable.get(method) * 4;
        instCache.add(new Instruction(Type.INDEX, func, mtable, Integer.toString(i)));
        StringBuilder args = new StringBuilder(cls);
        NodeListOptional argList = new ListVisitor().visit(n.f4);
        if ( argList != null ) {
            for ( Node a : argList.nodes ) {
                a.accept(this);
                args.append(", ").append(returnID);
            }
        }
        instCache.add(new Instruction(Type.CALL, ret, func, args.toString()));
        returnID = ret;
    }

    public void visit(AndExpression n) { // Done
        String ret = newTemp();

        n.f0.accept(this);
        String e1 = returnID;

        n.f2.accept(this);
        String e2 = returnID;

        instCache.add(new Instruction(Type.MULT, ret, e1, e2));
        returnID = ret;
    }

    public void visit(NotExpression n) { // Done
        String ret = newTemp();

        n.f1.accept(this);
        String negativeOne = newTemp();
        String e0 = returnID;

        instCache.add(new Instruction(Type.INT, negativeOne, "-1"));
        instCache.add(new Instruction(Type.MULT, ret, e0, negativeOne));

        returnID = ret;
    }

    public void visit(PlusExpression n) { // Done
        String ret = newTemp();

        n.f0.accept(this);
        String e1 = returnID;

        n.f2.accept(this);
        String e2 = returnID;

        instCache.add(new Instruction(Type.ADD, ret, e1, e2));
        returnID = ret;
    }

    public void visit(MinusExpression n) { // Done
        String ret = newTemp();

        n.f0.accept(this);
        String e1 = returnID;

        n.f2.accept(this);
        String e2 = returnID;

        instCache.add(new Instruction(Type.SUB, ret, e1, e2));
        returnID = ret;
    }

    public void visit(TimesExpression n) { // Done
        String ret = newTemp();

        n.f0.accept(this);
        String e1 = returnID;

        n.f2.accept(this);
        String e2 = returnID;

        instCache.add(new Instruction(Type.MULT, ret, e1, e2));
        returnID = ret;
    }

    public void visit(CompareExpression n) { // Done
        String ret = newTemp();

        n.f0.accept(this);
        String e1 = returnID;

        n.f2.accept(this);
        String e2 = returnID;
        instCache.add(new Instruction(Type.LESS, ret, e1, e2));
        returnID = ret;
    }

    public void visit(IntegerLiteral n) { // Done
        returnID = newTemp();
        instCache.add(new Instruction(Type.INT, returnID, n.f0.tokenImage));
    }
    public void visit(TrueLiteral n) { // Done
        returnID = newTemp();
        instCache.add(new Instruction(Type.INT, returnID, "1"));
    }
    public void visit(FalseLiteral n) { // Done
        returnID = newTemp();
        instCache.add(new Instruction(Type.INT, returnID, "0"));
    }
    // All identifiers must be declared before ANY statement, meaning if you find
    //an identifier in a statement it MUST be a local or a field, and identifiers
    //in declarations should be added to the appropriate LinkedHashSet
    public void visit(Identifier n) {
        String id = idVisitor.visit(n);
        if ( env.containsKey(id) ) {
            returnID = env.get(id);
        } else {
            // This method should only be called if a field is being read, not written
            // Field can only be written to in assign statement, so this is the general
            //case where a field is being read. Add one since index zero is address of method table
            int i = (fields.get(id)+1)*4;
            String newID = newTemp();
            instCache.add(new Instruction(Type.INDEX, newID, "this", Integer.toString(i)));
            returnID = newID;
        }
    }
    public void visit(ThisExpression n) { returnID = "this"; }

}

class ClassVisitor extends GJNoArguDepthFirst<HashSet<ClassDeclaration>> {
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

class ClassExtendsVisitor extends GJNoArguDepthFirst<HashSet<ClassExtendsDeclaration>> {
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
class SuperclassVisitor extends GJNoArguDepthFirst<String> {
    public String visit(ClassExtendsDeclaration node) { return node.f3.f0.tokenImage; }
    public String visit(ClassDeclaration node) { return null; }
}

class MethodVisitor extends GJNoArguDepthFirst<LinkedHashSet<MethodDeclaration>> {
    public LinkedHashSet<MethodDeclaration> visit(MethodDeclaration node) {
        LinkedHashSet<MethodDeclaration> methods = new LinkedHashSet<>();
        methods.add(node);
        return methods;
    }
    public LinkedHashSet<MethodDeclaration> visit(ClassDeclaration node) {
        LinkedHashSet<MethodDeclaration> methods = new LinkedHashSet<>();
        for (Node n : node.f4.nodes) {
            methods.addAll(n.accept(this));
        }
        return methods;
    }
    public LinkedHashSet<MethodDeclaration> visit(ClassExtendsDeclaration node) {
        LinkedHashSet<MethodDeclaration> methods = new LinkedHashSet<>();
        for (Node n : node.f6.nodes) {
            methods.addAll(n.accept(this));
        }
        return methods;
    }

    @Override
    public LinkedHashSet<MethodDeclaration> visit(TypeDeclaration node) { return node.f0.accept(this);
    }
}

class FieldsVisitor extends GJNoArguDepthFirst<LinkedHashSet<VarDeclaration>> {
    public LinkedHashSet<VarDeclaration> visit(ClassDeclaration node) {
        LinkedHashSet<VarDeclaration> fields = new LinkedHashSet<>();
        for (Node n : node.f3.nodes ) {
            fields.addAll(n.accept(this));
        }
        return fields;
    }
    public LinkedHashSet<VarDeclaration> visit(ClassExtendsDeclaration node) {
        LinkedHashSet<VarDeclaration> fields = new LinkedHashSet<>();
        for (Node n : node.f5.nodes ) {
            fields.addAll(n.accept(this));
        }
        return fields;
    }
    public LinkedHashSet<VarDeclaration> visit(VarDeclaration node) {
        LinkedHashSet<VarDeclaration> fields = new LinkedHashSet<>();
        fields.add(node);
        return fields;
    }
    public LinkedHashSet<VarDeclaration> visit(TypeDeclaration node) { return node.f0.accept(this);
    }
}

class IDVisitor extends GJNoArguDepthFirst<String> {
    public String visit(NodeOptional n) { if (n.present()) return n.node.accept(this); else return null; }
    public String visit(Identifier n) { return n.f0.tokenImage; }
    public String visit(TypeDeclaration n) { return n.f0.accept(this); }
    public String visit(ClassDeclaration n) { return n.f1.accept(this); }
    public String visit(ClassExtendsDeclaration n) { return n.f1.accept(this); }
    public String visit(MethodDeclaration n) { return n.f2.accept(this); }
    public String visit(VarDeclaration n) { return n.f1.accept(this); }
    public String visit(FormalParameter n) { return n.f1.accept(this); }
    public String visit(FormalParameterRest n) { return n.f1.accept(this); }
}
