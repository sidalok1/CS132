import IR.token.Identifier;
import minijava.syntaxtree.FormalParameter;
import sparrowv.*;
import sparrowv.visitor.*;
import java.util.*;
import hw5utils.*;

public class CodeGenerator implements Visitor {
    ArrayList<Function> functions;
    Function f;


    public CodeGenerator(Program program) {
        program.accept(this);
    }
    public void visit(Program n) {
        functions = new ArrayList<>();
        n.funDecls.forEach(fn -> fn.accept(this));
    }
    public void visit(FunctionDecl n) {
        f = new Function(this.func(n.functionName.name));
        this.findIDs(n);
        labels = new HashMap<>(); //make sure mangled labels enforce scoping
        n.block.accept(this);
        functions.add(f);
    }
    public void visit(Block n) {
        f.ret = n.return_id.toString();
        n.instructions.forEach(inst -> inst.accept(this));
    }

    public void visit(Add n) {
        f.add(RV.ADD(Reg.reg(n.lhs), Reg.reg(n.arg1), Reg.reg(n.arg2)));
    }
    public void visit(Alloc n) {
        f.add(RV.ADDI(Reg.a1, Reg.reg(n.size), 0));
        f.add(RV.LI(Reg.a0, ECalls.sbrk.toString()));
        f.add(RV.ECALL());
        f.add(RV.ADDI(Reg.reg(n.lhs), Reg.a0, 0));
    }
    public void visit(Call n) {
        Stack<Identifier> args = new Stack<>();
        for (Identifier arg : n.args) { args.push(arg); }
        int c = -4;
        while (!args.isEmpty()) {
            String arg = args.pop().toString();
            int idx = f.indexOf(arg);
            Reg source = f.locals.contains(arg) ? Reg.sp : Reg.fp;
            f.add(RV.LW(Reg.t6, source, idx*4));
            f.add(RV.SW(Reg.t6, c, Reg.sp));
            c -= 4;
        }
        c += 4;
        f.add(RV.ADDI(Reg.sp, Reg.sp, c));
        f.add(RV.JALR(Reg.reg(n.callee)));
        f.add(RV.ADDI(Reg.sp, Reg.sp,-c));
        f.add(RV.ADDI(Reg.reg(n.lhs), Reg.a0, 0));
    }
    public void visit(ErrorMessage n) {
        f.add(RV.LA(Reg.a0, this.strings(n.msg)));
        f.add(RV.CALL("error"));
    }
    public void visit(Goto n) {
        f.add(RV.JAL(Reg.zero, this.mangle(n.label.toString())));
    }
    public void visit(IfGoto n) {
        String pass = "pass" + i;
        i++;
        f.add(RV.BNEZ(Reg.reg(n.condition), pass));
        f.add(RV.JAL(Reg.zero, this.mangle(n.label.toString())));
        f.add(RV.LABEL(pass));
    }
    public void visit(LabelInstr n) {
        f.add(RV.LABEL(this.mangle(n.label.toString())));
    }
    public void visit(LessThan n) {
        f.add(RV.SLT(Reg.reg(n.lhs), Reg.reg(n.arg1), Reg.reg(n.arg2)));
    }
    public void visit(Load n) {
        f.add(RV.LW(Reg.reg(n.lhs), Reg.reg(n.base), n.offset));
    }
    public void visit(Move_Id_Reg n) {
        String id = n.lhs.toString();
        Reg r = f.locals.contains(id) ? Reg.sp : Reg.fp;
        int i = f.indexOf(id);
        f.add(RV.SW(Reg.reg(n.rhs), i * 4, r));
    }
    public void visit(Move_Reg_FuncName n) {
        f.add(RV.LA(Reg.reg(n.lhs), this.func(n.rhs.name)));
    }
    public void visit(Move_Reg_Id n) {
        String id = n.rhs.toString();
        Reg r = f.locals.contains(id) ? Reg.sp : Reg.fp;
        int i = f.indexOf(id);
        f.add(RV.LW(Reg.reg(n.lhs), r, i * 4));
    }
    public void visit(Move_Reg_Integer n) {
        f.add(RV.LI(Reg.reg(n.lhs), n.rhs));
    }
    public void visit(Move_Reg_Reg n) {
        f.add(RV.ADDI(Reg.reg(n.lhs), Reg.reg(n.rhs), 0));
    }
    public void visit(Multiply n) {
        f.add(RV.MUL(Reg.reg(n.lhs), Reg.reg(n.arg1), Reg.reg(n.arg2)));
    }
    public void visit(Print n) {
        f.add(RV.ADDI(Reg.a0, Reg.reg(n.content), 0));
        f.add(RV.CALL("print"));
    }
    public void visit(Store n) {
        f.add(RV.SW(Reg.reg(n.rhs), n.offset, Reg.reg(n.base)));
    }
    public void visit(Subtract n) {
        f.add(RV.SUB(Reg.reg(n.lhs), Reg.reg(n.arg1), Reg.reg(n.arg2)));
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        for ( ECalls e : ECalls.values() ) {
            ret.append(".equiv ")
            .append(e.toString())
            .append(", ")
            .append(ecalls.get(e))
            .append("\n");
        }
        ret.append("\n.text\n\n");
        functions.get(0).start = true;
        functions.forEach(f -> ret.append(f.toString()).append("\n\n"));
        ret.append(print).append("\n\n");
        ret.append(error).append("\n\n");
        ret.append(".data\n\n");
        for (String s : data.keySet()) {
            String t = data.get(s);
            ret.append(".globl ").append(t).append("\n");
            ret.append(t).append(":\n");
            ret.append("\t.asciiz ").append(s).append("\n");
            ret.append("\t.align 2\n\n");
        }
        return ret.toString();
    }
    HashMap<String, String> names = new HashMap<>();
    private int k = 0;
    private String func(String name) {
        if (names.containsKey(name)) {
            return names.get(name);
        } else {
            String mangled = "_" + k + "_" + name;
            names.put(name, mangled);
            k++;
            return mangled;
        }
    }
    HashMap<String, String> labels = new HashMap<>();
    private int i = 0;
    private String mangle(String label) {
        if (labels.containsKey(label)) {
            return labels.get(label);
        } else {
            String mangled = "_" + label + i;
            labels.put(label, mangled);
            i++;
            return mangled;
        }
    }
    HashMap<String, String> data = new HashMap<>();
    int j = 0;
    private String strings(String s) {
        if (!data.containsKey(s)) {
            String mangled = "_msg_" + j;
            data.put(s, mangled);
            j++;
            return mangled;
        } else {
            return data.get(s);
        }
    }
    private void findIDs(FunctionDecl dec) {
        for (Identifier p : dec.formalParameters) {
            this.f.params.add(p.toString());
        }
        IDVisitor v = new IDVisitor();
        TreeSet<String> locals = new TreeSet<>();
        for (Instruction i : dec.block.instructions) {
            String var = i.accept(v);
            if (!this.f.params.contains(var) && var != null) {
                locals.add(var);
            }
        }
        f.locals.addAll(locals);
    }

    private static class IDVisitor implements RetVisitor<String> {
        public String visit(Program n) { return null; }
        public String visit(FunctionDecl n) { return null; }
        public String visit(Block n) { return null; }
        public String visit(LabelInstr n) { return null; }
        public String visit(Move_Reg_Integer n) { return null; }
        public String visit(Move_Reg_FuncName n) { return null; }
        public String visit(Add n) { return null; }
        public String visit(Subtract n) { return null; }
        public String visit(Multiply n) { return null; }
        public String visit(LessThan n) { return null; }
        public String visit(Load n) { return null; }
        public String visit(Store n) { return null; }
        public String visit(Move_Reg_Reg n) { return null; }
        public String visit(Move_Id_Reg n) {
            return n.lhs.toString();
        }
        public String visit(Move_Reg_Id n) {
            return n.rhs.toString();
        }
        public String visit(Alloc n) { return null; }
        public String visit(Print n) { return null; }
        public String visit(ErrorMessage n) { return null; }
        public String visit(Goto n) { return null; }
        public String visit(IfGoto n) { return null; }
        public String visit(Call n) { return null; }
    }

    private static enum ECalls {
        print_int, print_string, sbrk, exit, print_char, exit2;
        public String toString() {
            return "@" + name();
        }
    }

    static EnumMap<ECalls, Integer> ecalls = new EnumMap<ECalls, Integer>(ECalls.class) {{
        put(ECalls.print_int, 1);
        put(ECalls.print_string, 4);
        put(ECalls.sbrk, 9);
        put(ECalls.exit, 10);
        put(ECalls.print_char, 11);
        put(ECalls.exit2, 17);
    }};
    public static final Function print = new Function("print", new ArrayList<>(Arrays.asList(
            RV.ADDI(Reg.a1, Reg.a0, 0),
            RV.LI(Reg.a0, ECalls.print_int.toString()),
            RV.ECALL(),
            RV.LI(Reg.a1, 10),
            RV.LI(Reg.a0, ECalls.print_char.toString()),
            RV.ECALL(),
            RV.RET()
    )));
    public static final Function error = new Function("error", new ArrayList<>(Arrays.asList(
            RV.ADDI(Reg.a1, Reg.a0, 0),
            RV.LI(Reg.a0, ECalls.print_string.toString()),
            RV.ECALL(),
            RV.LI(Reg.a1, 10),
            RV.LI(Reg.a0, ECalls.print_char.toString()),
            RV.ECALL(),
            RV.LI(Reg.a0, ECalls.exit.toString()),
            RV.ECALL()
    )));
}
