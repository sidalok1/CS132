package hw4utils;

import java.util.*;

public class Inst {
    public static class InvalidInstruction extends RuntimeException {}
    public static enum Type {
        LABEL,
        INT,
        FUNC,
        ADD,
        SUB,
        MULT,
        LESS,
        INDEX,
        ARRAY,
        REG,
        REGtoID,
        IDtoREG,
        ALLOC,
        PRINT,
        ERROR,
        GOTO,
        IF0,
        CALL;
    }
    public Type type;
    private Reg rs1, rs2;
    public Reg rd;
    private String id, label, func, str;
    private int imm;
    private ArrayList<String> args;
    private static final HashSet<Type> arith = new HashSet<Type>(Arrays.asList(Type.ADD, Type.SUB, Type.MULT, Type.LESS));

    public Inst(Type type, String s) {
        if (!type.equals(Type.LABEL) && !type.equals(Type.GOTO) && !type.equals(Type.ERROR))
            { throw new InvalidInstruction(); }
        this.type = type;
        if (type.equals(Type.ERROR)) {
            str = s;
        } else {
            label = s;
        }
    }
    public Inst(Type type, Reg r, String s) {
        this.type = type;
        switch (type) {
            case FUNC:
                this.rd = r;
                this.func = s;
                break;
            case IF0:
                this.rs1 = r;
                this.label = s;
                break;
            case REGtoID:
                this.rs1 = r;
                this.id = s;
                break;
            case IDtoREG:
                this.rd = r;
                this.id = s;
                break;
            case INT:
                this.rd = r;
                this.imm = Integer.parseInt(s);
                break;
            default:
                throw new InvalidInstruction();
        }
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
    public Inst(Reg rd, Reg rs1, List<String> args) {
        this.type = Type.CALL;
        this.rd = rd;
        this.rs1 = rs1;
        this.args = new ArrayList<>(args);
    }
    public Inst(Type type, Reg rd, Reg rs1) {
        if (!type.equals(Type.REG) && !type.equals(Type.ALLOC)) { throw new InvalidInstruction(); }
        this.type = type;
        this.rs1 = rs1;
        this.rd = rd;
    }
    public Inst(Reg rs1) {
        this.rs1 = rs1;
        this.type = Type.PRINT;
    }
    public Inst(Type type, Reg rd, Reg rs1, Reg rs2) {
        if (!arith.contains(type)) { throw new InvalidInstruction(); }
        this.type = type;
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
                for ( String s : args) { sj.add(s); }
                return "\t" + rd + " = call " + rs1 + sj.toString();
            }
            default: return "";
        }
    }
}
