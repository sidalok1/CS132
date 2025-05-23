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

    private final HashMap<String, String> funcMangler = new HashMap<>();
    private final HashMap<String, String> constMangler = new HashMap<>();

    private Program program;
    private FunDecl funDecl;
    private final HashMap<String, Integer> methodTable = new HashMap<>();
    private final HashMap<String, Integer> fields = new HashMap<>();
    private LinkedHashMap<String, String> env_params = new LinkedHashMap<>();
    private LinkedHashMap<String, String> env_vars = new LinkedHashMap<>();
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
    private int f = 0;
    private String newFunc() {
        String func = "f" + f;
        f++;
        return func;
    }

    private void analyzeTypes() {
        Stack<String> order = new Stack<>();
        for ( String outer : inheritance.keySet() ) {
            String cls = outer;
            while ( cls != null ) {
                order.add(cls);
                cls = inheritance.get(cls);
            }
        }
        for ( Node n : goal.f1.nodes ) { // non-inherited classes
            String cls = n.accept(idVisitor);
            if ( cls != null && !order.contains(cls) ) { order.push(cls); }
        }
        while ( !order.isEmpty() ) {
            String cls = order.pop();
            if ( typedefs.containsKey(cls) ) { continue; }
            constMangler.put(cls, newFunc() + cls);
            Typedef classDef = new Typedef();
            classDef.classname = cls;
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
                //defines.computeIfAbsent(cls, k -> new HashSet<>()).add(mname);
                classDef.methods.add(mname);
                String canonicalMethod = cls + "_" + mname;
                funcMangler.put(canonicalMethod, newFunc() + mname);
            }
            LinkedHashSet<VarDeclaration> fields = c.accept(fieldsVisitor);
            for ( VarDeclaration f : fields ) {
                String fname = f.accept(idVisitor);
                defines.computeIfAbsent(cls, k -> new HashSet<>()).add(fname);
                classDef.fields.add(cls + "_" + f.accept(idVisitor));
            }
            typedefs.put(cls, classDef);
        }
        LinkedHashSet<String> allMethods = new LinkedHashSet<>();
        LinkedHashSet<String> allFields = new LinkedHashSet<>();
        for ( Typedef tdef : typedefs.values() ) {
            allMethods.addAll(tdef.methods);
            allFields.addAll(tdef.fields);
        }
        ArrayList<String> mstrings = new ArrayList<>(allMethods);
        ArrayList<String> fstrings = new ArrayList<>(allFields);
        for ( String method : mstrings ) { methodTable.put(method, mstrings.indexOf(method)); }
        for ( String field : fstrings ) { fields.put(field, fstrings.indexOf(field)); }
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
    private String definedIn(String identifier, String classname) {
        assert identifier != null && classname != null;
        if (defines.containsKey(classname) && defines.get(classname).contains(identifier)) { return classname; }
        else {
            return definedIn(identifier, inheritance.get(classname));
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
        env_params = new LinkedHashMap<>();
        LinkedList<String> params = new LinkedList<>(env_params.keySet());
        n.f14.accept(this);
        instCache = new LinkedList<>();
        n.f15.accept(this);
        CodeBlock b = new CodeBlock(instCache, returnID);
        f.FunctionName = newFunc() + main;
        funcMangler.put(main, f.FunctionName);
        f.block = b;
        f.paramIDs = params;
        program.functions.add(f);
    }
    public void visit(ClassExtendsDeclaration n) {
        String id = idVisitor.visit(n);
        String superclass = superclassVisitor.visit(n);
        thisClass = id;
        FunDecl constructor = new FunDecl();
        CodeBlock initCode = new CodeBlock();
        String ret = newTemp() + "_" + id + "_instance";
        String superinit = newTemp() + "_" + superclass + "_constructor";
        String scls = constMangler.get(superclass);
        initCode.instructions.add(
                new Instruction(Type.FUNC, superinit, scls)
        );
        initCode.instructions.add(
                new Instruction(Type.CALL, ret, superinit)
        );
        String methodtable = newTemp() + "_" + id + "_methodTable";
        initCode.instructions.add(
                new Instruction(Type.INDEX, methodtable, ret, "0")
        );
        for ( Node method : n.f6.nodes ) {
            method.accept(this);
            String mname = method.accept(idVisitor);
            int midx = methodTable.get(mname)*4;
            String mvar = newTemp() + "_" + mname;
            String canonicalName = id + "_" + mname;
            initCode.instructions.add(
                    new Instruction(Type.FUNC, mvar, funcMangler.get(canonicalName))
            );
            initCode.instructions.add(
                    new Instruction(Type.ARRAY, methodtable, Integer.toString(midx), mvar)
            );
        }
        initCode.returnID = ret;
        constructor.FunctionName = constMangler.get(id);
        constructor.block = initCode;
        program.functions.add(constructor);
    }
    public void visit(ClassDeclaration n) {
        String id = idVisitor.visit(n);
        thisClass = id;
        Typedef typedef = typedefs.get(id);
        FunDecl constructor = new FunDecl();
        CodeBlock initCode = new CodeBlock();

        String ret = newTemp() + "_" + id + "_instance";
        String mTable = newTemp() + "_" + id + "_methodTable";
        String numFields = newTemp() + "_fieldByteCount";
        initCode.instructions.add(
                new Instruction(Type.INT, numFields, Integer.toString((fields.size()+1)*4))
        );
        initCode.instructions.add(
                new Instruction(Type.ALLOC, ret, numFields)
        );
        String numMethods = newTemp() + "_methodByteCount";
        initCode.instructions.add(
                new Instruction(Type.INT, numMethods, Integer.toString((methodTable.size())*4))
        );
        initCode.instructions.add(
                new Instruction(Type.ALLOC, mTable, numMethods)
        );
        initCode.instructions.add(
                new Instruction(Type.ARRAY, ret, "0", mTable)
        );
        for ( Node method : n.f4.nodes ) {
            method.accept(this);
            String mname = method.accept(idVisitor);
            String mvar = newTemp() + "_" + mname;
            String canonicalName = id + "_" + mname;
            initCode.instructions.add(
                    new Instruction(Type.FUNC, mvar, funcMangler.get(canonicalName))
            );
            int midx = methodTable.get(mname)*4;
            initCode.instructions.add(
                    new Instruction(Type.ARRAY, mTable, Integer.toString(midx), mvar)
            );
        }
        initCode.returnID = ret;
        constructor.FunctionName =  constMangler.get(id);
        constructor.block = initCode;
        program.functions.add(constructor);
    }



    public void visit(MethodDeclaration n) {
        FunDecl f = new FunDecl();
        String fname = idVisitor.visit(n);
        env_params = new LinkedHashMap<>();
        env_params.put("this", "this");
        if ( n.f4.present() ) {
            n.f4.node.accept(this);
        }
        LinkedList<String> params = new LinkedList<>(env_params.values());
        env_vars = new LinkedHashMap<>();
        n.f7.accept(this);
        instCache = new LinkedList<>();
        n.f8.accept(this);
        n.f10.accept(this);
        CodeBlock b = new CodeBlock(instCache, returnID);
        String canonicalName = thisClass + "_" + fname;
        f.FunctionName = funcMangler.get(canonicalName);
        f.block = b;
        f.paramIDs = params;
        program.functions.add(f);
    }
    public void visit(FormalParameterRest n) { n.f1.accept(this); }
    public void visit(FormalParameter n) {
        String param = newParam();
        String id = idVisitor.visit(n);
        env_params.put(id, param + "_" + id);
    }
    public void visit(VarDeclaration n) {
        String var = newVar();
        String id = idVisitor.visit(n);
        env_vars.put(id, var + "_" + id);
    }

    public void visit(Block n) {
        for ( Node node : n.f1.nodes ) {
            node.accept(this);
        }
    }

    public void visit(IfStatement n) {
        n.f2.accept(this);
        String e0 = returnID, l = newLabel();
        instCache.add(new Instruction(Type.IF0, e0, l + "_else"));
        n.f4.accept(this);
        instCache.add(new Instruction(Type.GOTO, l + "_endif"));
        instCache.add(new Instruction(Type.LABEL, l + "_else"));
        n.f6.accept(this);
        instCache.add(new Instruction(Type.LABEL, l + "_endif"));
    }

    public void visit(WhileStatement n) { // Done
        String loop = newLabel(), loop_end = newLabel();
        instCache.add(new Instruction(Type.LABEL, loop + "_while_start"));
        n.f2.accept(this); // check condition
        String cont = returnID;
        instCache.add(new Instruction(Type.IF0, cont, loop + "_while_end")); // if condition is false
        n.f4.accept(this);
        instCache.add(new Instruction(Type.GOTO, loop + "_while_start"));
        instCache.add(new Instruction(Type.LABEL, loop + "_while_end"));
    }

    public void visit(PrintStatement n) { // Done
        n.f2.accept(this);
        instCache.add(new Instruction(Type.PRINT, returnID));
    }

    public void visit(AssignmentStatement n) { // Done
        n.f2.accept(this);
        String id = idVisitor.visit(n.f0);
        if ( env_vars.containsKey(id) ) {
            String e0 = env_vars.get(id);
            instCache.add(new Instruction(Type.ID, e0, returnID));
        } else if ( env_params.containsKey(id) ) {
            String e0 = env_params.get(id);
            instCache.add(new Instruction(Type.ID, e0, returnID));
        } else {
            // If the program already type checked and the identifier is not in the environment, it must be a field
            String fromClass = definedIn(id, thisClass);
            int field_index = (fields.get(fromClass + "_" + id)+1)*4;
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
                l = newLabel();
        instCache.add(new Instruction(Type.LESS, lowbound, idx_plus_one, one));
        instCache.add(new Instruction(Type.IF0, lowbound, l + "_check_high_bound")); // assert idx >= 0
        instCache.add(new Instruction(Type.LABEL, l + "_error"));
        instCache.add(new Instruction(Type.ERROR, "array index out of bounds"));
        instCache.add(new Instruction(Type.LABEL, l + "_check_high_bound"));
        instCache.add(new Instruction(Type.INDEX, len, id, "0"));
        instCache.add(new Instruction(Type.LESS, highbound, index, len));
        instCache.add(new Instruction(Type.IF0, highbound, l + "_error")); // assert idx < len

        instCache.add(new Instruction(Type.MULT, array_offset, idx_plus_one, four));
        instCache.add(new Instruction(Type.ADD, offset_base, id, array_offset));
        n.f5.accept(this);
        String val = returnID;
        instCache.add(new Instruction(Type.ARRAY, offset_base, "0", val));
    }

    public void visit(ArrayLength n) { // Done
        n.f0.accept(this);
        String id = returnID;
        String ret = newTemp() + "_" + id + "_length";
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
                l = newLabel();
        instCache.add(new Instruction(Type.LESS, lowbound, idx_plus_one, one));
        instCache.add(new Instruction(Type.IF0, lowbound, l + "_check_high_bound")); // assert idx >= 0
        instCache.add(new Instruction(Type.LABEL, l + "_error"));
        instCache.add(new Instruction(Type.ERROR, "array index out of bounds"));
        instCache.add(new Instruction(Type.LABEL, l + "_check_high_bound"));
        instCache.add(new Instruction(Type.INDEX, len, lst, "0"));
        instCache.add(new Instruction(Type.LESS, highbound, idx, len));
        instCache.add(new Instruction(Type.IF0, highbound, l + "_error")); // assert idx < len

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
        String lessthanzero = newTemp(), l = newLabel();
        instCache.add(new Instruction(Type.LESS, lessthanzero, len, zero)); // lessthanzero = ( len < zero ) ? 1 : 0
        instCache.add(new Instruction(Type.IF0, lessthanzero, l + "_valid")); // if (len < 0):
        instCache.add(new Instruction(Type.ERROR, "Argument to alloc() must be positive")); // throw AllocError()
        instCache.add(new Instruction(Type.LABEL, l + "_valid")); // else:
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
        String cls = n.f1.accept(idVisitor);
        String ret = newTemp() + "_" + cls;
        String constructor = newTemp();
        String constructorName = constMangler.get(cls);
        instCache.add(new Instruction(Type.FUNC, constructor, constructorName));
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
        String l = newLabel();
        n.f0.accept(this);
        String cls = returnID;
        String method = n.f2.accept(idVisitor);
        String ret = newTemp() + "_" + method + "_ret";
        String mtable = newTemp() + "_methodTable";
        instCache.add(new Instruction(Type.IF0, cls, l + "_nullptr"));
        instCache.add(new Instruction(Type.INDEX, mtable, cls, "0"));
        instCache.add(new Instruction(Type.IF0, mtable, l + "_nullptr"));
        String func = newTemp() + "_" + method;
        int i = methodTable.get(method) * 4;
        instCache.add(new Instruction(Type.INDEX, func, mtable, Integer.toString(i)));
        StringBuilder oldArgs = new StringBuilder(args);
        args = new StringBuilder(cls);
        if ( n.f4.present() ) { n.f4.accept(this); }
        instCache.add(new Instruction(Type.CALL, ret, func, args.toString()));
        args = oldArgs;
        instCache.add(new Instruction(Type.GOTO, l + "_valid"));
        instCache.add(new Instruction(Type.LABEL, l + "_nullptr"));
        instCache.add(new Instruction(Type.ERROR, "null pointer"));
        instCache.add(new Instruction(Type.LABEL, l + "_valid"));
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
        String result = newTemp(), l = newLabel();

        n.f0.accept(this);
        String left = returnID;

        instCache.add(new Instruction(Type.IF0, left, l + "_resultFalse"));

        n.f2.accept(this);
        String right = returnID;

        instCache.add(new Instruction(Type.IF0, right, l + "_resultFalse"));

        instCache.add(new Instruction(Type.INT, result, "1"));
        instCache.add(new Instruction(Type.GOTO, l + "_andExpEnd"));
        instCache.add(new Instruction(Type.LABEL, l + "_resultFalse"));
        instCache.add(new Instruction(Type.INT, result, "0"));
        instCache.add(new Instruction(Type.LABEL, l + "_andExpEnd"));

        returnID = result;
    }

    public void visit(NotExpression n) { // Done
        String inverse = newTemp();

        n.f1.accept(this);
        String value = returnID;
        String one = newConst();

        instCache.add(new Instruction(Type.INT, one, "1"));
        instCache.add(new Instruction(Type.SUB, inverse, one, value));

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
        returnID = newTemp() + "_intLiteral";
        instCache.add(new Instruction(Type.INT, returnID, n.f0.tokenImage));
    }
    public void visit(TrueLiteral n) { // Done
        returnID = newTemp() + "_trueLiteral";
        instCache.add(new Instruction(Type.INT, returnID, "1"));
    }
    public void visit(FalseLiteral n) { // Done
        returnID = newTemp() + "_falseLiteral";
        instCache.add(new Instruction(Type.INT, returnID, "0"));
    }
    // All identifiers must be declared before ANY statement, meaning if you find
    //an identifier in a statement it MUST be a local or a field, and identifiers
    //in declarations should be added to the appropriate LinkedHashSet
    public void visit(Identifier n) {
        String id = idVisitor.visit(n);
        if ( env_vars.containsKey(id) ) {
            returnID = env_vars.get(id);
        } else if ( env_params.containsKey(id) ) {
            returnID = env_params.get(id);
        } else {
            // This method should only be called if a field is being read, not written
            // Field can only be written to in assign statement, so this is the general
            //case where a field is being read. Add one since index zero is address of method table
            String fromClass = definedIn(id, thisClass);
            int i = (fields.get(fromClass + "_" + id)+1)*4;
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
    public String visit(TypeDeclaration node) { return node.f0.choice.accept(this); }
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
