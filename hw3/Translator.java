import minijava.syntaxtree.*;
import minijava.visitor.*;

import java.util.*;

import utils.*;

import sparrowCode.*;
import sparrowCode.Instruction.Type;

@SuppressWarnings("DuplicatedCode")
public class Translator extends DepthFirstVisitor {
    private final Goal goal;

    private final Graph inheritance;
    private final HashMap<String, Typedef> typedefs = new HashMap<>();

    private final HashMap<String, HashSet<String>> defines = new HashMap<>();

    private Program program;
    private FunDecl funDecl;
    private final HashMap<String, Integer> methodTable = new HashMap<>();
    private HashMap<String, Integer> fields = new HashMap<>();
    private LinkedHashMap<String, String> env = new LinkedHashMap<>();
    private LinkedList<Instruction> instCache = new LinkedList<>();
    private StringBuilder args = new StringBuilder();
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
    private int c = 0;
    private String newConst() {
        String cnst = "c" + c;
        c++;
        return cnst;
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
        env = new LinkedHashMap<>();
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
        env = new LinkedHashMap<>();
        env.put("this", "this");
        if ( n.f4.present() ) {
            n.f4.node.accept(this);
        }
        LinkedList<String> params = new LinkedList<>(env.values());
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
    public void visit(FormalParameterList n) { n.f0.accept(this); n.f1.accept(this); }
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
        String loop_start = newLabel(), loop_end = newLabel();
        instCache.add(new Instruction(Type.LABEL, loop_start));
        n.f2.accept(this);
        String cont = returnID;
        instCache.add(new Instruction(Type.IF0, cont, loop_end));
        n.f4.accept(this);
        instCache.add(new Instruction(Type.GOTO, loop_start));
        instCache.add(new Instruction(Type.LABEL, loop_end));
    }

    public void visit(PrintStatement n) { // Done
        n.f2.accept(this);
        instCache.add(new Instruction(Type.PRINT, returnID));
    }

    public void visit(AssignmentStatement n) { // Done
        n.f2.accept(this);
        String id = idVisitor.visit(n.f0);
        if ( env.containsKey(id) ) {
            String e0 = env.get(id);
            instCache.add(new Instruction(Type.ID, e0, returnID));
        } else {
            // If the program already type checked and the identifier is not in the environment, it must be a field
            int field_index = (fields.get(id)+1)*4;
            instCache.add(new Instruction(Type.ARRAY, "this", Integer.toString(field_index), returnID));
        }
    }

    public void visit(ArrayAssignmentStatement n) {
        n.f0.accept(this);
        String id = returnID;

        n.f2.accept(this);
        String index = returnID;
        String idx_plus_one = newTemp();
        String array_offset = newTemp();
        String offset_base = newTemp();
        String one = newConst();
        String four = newConst();
        instCache.add(new Instruction(Type.INT, one, "1"));
        instCache.add(new Instruction(Type.INT, four, "4"));
        // Increment and mult for sparrow indexing
        instCache.add(new Instruction(Type.ADD, idx_plus_one, index, one));

        String lowbound = newTemp(), highbound = newTemp(), len = newTemp(),
                err = newLabel(), highboundcheck = newLabel();
        instCache.add(new Instruction(Type.LESS, lowbound, idx_plus_one, one));
        instCache.add(new Instruction(Type.IF0, lowbound, highboundcheck)); // assert idx >= 0
        instCache.add(new Instruction(Type.LABEL, err));
        instCache.add(new Instruction(Type.ERROR, "array index out of bounds"));
        instCache.add(new Instruction(Type.LABEL, highboundcheck));
        instCache.add(new Instruction(Type.INDEX, len, id, "0"));
        instCache.add(new Instruction(Type.LESS, highbound, index, len));
        instCache.add(new Instruction(Type.IF0, highbound, err)); // assert idx < len

        instCache.add(new Instruction(Type.MULT, array_offset, idx_plus_one, four));
        instCache.add(new Instruction(Type.ADD, offset_base, id, array_offset));
        n.f5.accept(this);
        String val = returnID;
        instCache.add(new Instruction(Type.ARRAY, offset_base, "0", val));
    }

    public void visit(ArrayLength n) { // Done
        n.f0.accept(this);
        String id = returnID;
        String ret = newTemp();
        instCache.add(new Instruction(Type.INDEX, ret, id, "0"));
        returnID = ret;
    }

    public void visit(ArrayLookup n) { // Done
        n.f0.accept(this);
        String lst = returnID;
        n.f2.accept(this);
        String idx = returnID;

        String one = newConst();
        String idx_plus_one = newTemp();
        instCache.add(new Instruction(Type.INT, one, "1"));
        instCache.add(new Instruction(Type.ADD, idx_plus_one, idx, one));

        String lowbound = newTemp(), highbound = newTemp(), len = newTemp(),
                err = newLabel(), highboundcheck = newLabel();
        instCache.add(new Instruction(Type.LESS, lowbound, idx_plus_one, one));
        instCache.add(new Instruction(Type.IF0, lowbound, highboundcheck)); // assert idx >= 0
        instCache.add(new Instruction(Type.LABEL, err));
        instCache.add(new Instruction(Type.ERROR, "array index out of bounds"));
        instCache.add(new Instruction(Type.LABEL, highboundcheck));
        instCache.add(new Instruction(Type.INDEX, len, lst, "0"));
        instCache.add(new Instruction(Type.LESS, highbound, idx, len));
        instCache.add(new Instruction(Type.IF0, highbound, err)); // assert idx < len

        String four = newConst();
        instCache.add(new Instruction(Type.INT, four, "4"));
        String idx_sparrow = newTemp();
        String base = newTemp();
        instCache.add(new Instruction(Type.MULT, idx_sparrow, idx_plus_one, four));
        instCache.add(new Instruction(Type.ADD, base, lst, idx_sparrow));
        String ret = newTemp();
        instCache.add(new Instruction(Type.INDEX, ret, base, "0"));
        returnID = ret;
    }

    public void visit(ArrayAllocationExpression n) { // Done
        String ret = newTemp();

        n.f3.accept(this);
        String zero = newConst();
        String len = returnID;
        instCache.add(new Instruction(Type.INT, zero, "0"));
        String lessthanzero = newTemp(), label = newLabel();
        instCache.add(new Instruction(Type.LESS, lessthanzero, len, zero)); // lessthanzero = ( len < zero ) ? 1 : 0
        instCache.add(new Instruction(Type.IF0, lessthanzero, label)); // if (len < 0):
        instCache.add(new Instruction(Type.ERROR, "Argument to alloc() must be positive")); // throw AllocError()
        instCache.add(new Instruction(Type.LABEL, label)); // else:
        String one = newConst();
        instCache.add(new Instruction(Type.INT, one, "1"));
        String four = newConst();
        instCache.add(new Instruction(Type.INT, four, "4"));
        String lenplusone = newTemp(), sparrowlen = newTemp();
        // Add by one and mult by 4 to get sparrow array size (add one bc array[0] = array.length())
        instCache.add(new Instruction(Type.ADD, lenplusone, len, one));
        instCache.add(new Instruction(Type.MULT, sparrowlen, four, lenplusone));
        instCache.add(new Instruction(Type.ALLOC, ret, sparrowlen));
        instCache.add(new Instruction(Type.ARRAY, ret, "0", len));

        returnID = ret;
    }

    public void visit(AllocationExpression n) {
        String ret = newTemp();
        String cls = n.f1.accept(idVisitor);
        String constructor = newTemp();
        instCache.add(new Instruction(Type.FUNC, constructor, cls));
        instCache.add(new Instruction(Type.CALL, ret, constructor));
        returnID = ret;
    }

    static class ListVisitor extends GJNoArguDepthFirst<NodeListOptional> {
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
        args = new StringBuilder(cls);
        if ( n.f4.present() ) { n.f4.accept(this); }
        instCache.add(new Instruction(Type.CALL, ret, func, args.toString()));
        returnID = ret;
    }
    public void visit(ExpressionList n) {
        n.f0.accept(this);
        args.append(" ");
        args.append(returnID);
        for ( Node node : n.f1.nodes ) {
            node.accept(this);
            args.append(" ");
            args.append(returnID);
        }
    }
    public void visit(ExpressionRest n) {
        n.f1.accept(this);
    }

    public void visit(AndExpression n) { // Done
        String result = newTemp();

        n.f0.accept(this);
        String left = returnID;

        n.f2.accept(this);
        String right = returnID;

        String one = newConst(), sum = newTemp();
        instCache.add(new Instruction(Type.INT, one, "1"));
        instCache.add(new Instruction(Type.ADD, sum, left, right));
        instCache.add(new Instruction(Type.LESS, result, one, sum));

        returnID = result;
    }

    public void visit(NotExpression n) { // Done
        String inverse = newTemp(), negated = newTemp();

        n.f1.accept(this);
        String zero = newConst();
        String value = returnID;

        instCache.add(new Instruction(Type.INT, zero, "0"));
        instCache.add(new Instruction(Type.SUB, negated, zero, value));
        instCache.add(new Instruction(Type.MULT, inverse, negated, negated));

        returnID = inverse;
    }

    public void visit(PlusExpression n) { // Done
        String sum = newTemp();

        n.f0.accept(this);
        String left = returnID;

        n.f2.accept(this);
        String right = returnID;

        instCache.add(new Instruction(Type.ADD, sum, left, right));
        returnID = sum;
    }

    public void visit(MinusExpression n) { // Done
        String difference = newTemp();

        n.f0.accept(this);
        String left = returnID;

        n.f2.accept(this);
        String right = returnID;

        instCache.add(new Instruction(Type.SUB, difference, left, right));
        returnID = difference;
    }

    public void visit(TimesExpression n) { // Done
        String product = newTemp();

        n.f0.accept(this);
        String factor1 = returnID;

        n.f2.accept(this);
        String factor2 = returnID;

        instCache.add(new Instruction(Type.MULT, product, factor1, factor2));
        returnID = product;
    }

    public void visit(CompareExpression n) { // Done
        String value = newTemp();

        n.f0.accept(this);
        String left = returnID;

        n.f2.accept(this);
        String right = returnID;
        instCache.add(new Instruction(Type.LESS, value, left, right));
        returnID = value;
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
    public void visit(BracketExpression n) { n.f1.accept(this); }
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
