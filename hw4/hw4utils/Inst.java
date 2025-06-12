package hw4utils;
import IR.token.*;
import java.util.*;

public class Inst {
    public static class InvalidInstruction extends RuntimeException {}

    public Type type;
    private Reg rs1, rs2;
    public Reg rd;
    private Identifier id;
    private Label label;
    private FunctionName func;
    private String str;
    private int imm;
    private ArrayList<Identifier> args;

    public Inst(Label l, boolean isGoto) {
        if (isGoto) {
            type = Type.GOTO;
        } else {
            type = Type.LABEL;
        }
        label = l;
    }

    public Inst(Reg rd, FunctionName f) {
        type = Type.FUNC;
        this.rd = rd;
        func = f;
    }

    public Inst(Reg rd, Identifier id) {
        type = Type.IDtoREG;
        this.rd = rd;
        this.id = id;
    }

    public Inst(Identifier id, Reg rs) {
        type = Type.REGtoID;
        this.rs1 = rs;
        this.id = id;
    }

    public Inst(Reg rs, Label l) {
        type = Type.IF0;
        this.rs1 = rs;
        label = l;
    }

    public Inst(Reg rd, int imm) {
        type = Type.INT;
        this.rd = rd;
        this.imm = imm;
    }

    public Inst(Reg rd, Reg rs1, int imm) {
        this.rd = rd;
        this.rs1 = rs1;
        this.imm = imm;
        this.type = Type.INDEX;
    }
    public Inst(Reg rs1, int imm, Reg rs2) {
        this.rs1 = rs1;
        this.imm = imm;
        this.rs2 = rs2;
        this.type = Type.ARRAY;
    }
    public Inst(Reg rd, Reg rs1, List<Identifier> args) {
        this.type = Type.CALL;
        this.rd = rd;
        this.rs1 = rs1;
        this.args = new ArrayList<>(args);
    }
    public Inst(Reg rd, Reg rs1, boolean regToReg) {
        if (regToReg) {
            this.type = Type.REG;
        } else {
            this.type = Type.ALLOC;
        }
        this.rs1 = rs1;
        this.rd = rd;
    }
    public Inst(Reg rs1) {
        this.rs1 = rs1;
        this.type = Type.PRINT;
    }
    public Inst(String msg) {
        this.type = Type.ERROR;
        this.str = msg;
    }
    public static enum Arith{ add, sub, mul, les }
    public Inst(Reg rd, Reg rs1, Reg rs2, Arith arith) {
        switch (arith) {
            case add:
                this.type = Type.ADD;
                break;
            case sub:
                this.type = Type.SUB;
                break;
            case mul:
                this.type = Type.MULT;
                break;
            case les:
                this.type = Type.LESS;
                break;
        }
        this.rs1 = rs1;
        this.rs2 = rs2;
        this.rd = rd;
    }
    public String toString() {
        switch (type) {
            case LABEL: return label + ": ";
            case INT: return "\t" + rd + " = " + imm;
            case REG: return "\t" + rd + " = " + rs1;
            case REGtoID: return "\t" + id + " = " + rs1;
            case IDtoREG: return "\t" + rd + " = " + id;
            case FUNC: return "\t" + rd + " = @ " + func;
            case ADD: return "\t" + rd + " = " + rs1 + " + " + rs2;
            case SUB: return "\t" + rd + " = " + rs1 + " - " + rs2;
            case MULT: return "\t" + rd + " = " + rs1 + " * " + rs2;
            case LESS: return "\t" + rd + " = " + rs1 + " < " + rs2;
            case INDEX: return "\t" + rd + " = [ " + rs1 + " + " + imm + " ]";
            case ARRAY: return "\t" + "[ " + rs1 + " + " + imm + " ] = " + rs2;
            case ALLOC: return "\t" + rd + " = alloc ( " + rs1 + " )";
            case PRINT: return "\t" + "print ( " + rs1 + " )";
            case ERROR: return "\t" + "error ( " + str + " )";
            case GOTO: return "\t" + "goto " + label;
            case IF0: return "\t" + "if0 " + rs1 + " goto " + label;
            case CALL: {
                StringJoiner sj = new StringJoiner(" ", " ( ", " )");
                for ( Identifier s : args) { sj.add(s.toString()); }
                return "\t" + rd + " = call " + rs1 + sj.toString();
            }
            default: return "";
        }
    }
}
