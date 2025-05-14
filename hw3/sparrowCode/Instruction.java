package sparrowCode;

import java.util.ArrayList;
import java.util.Arrays;

public class Instruction {
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
        ID,
        ALLOC,
        PRINT,
        ERROR,
        GOTO,
        IF0,
        CALL;
    }
    public Type type;
    public String[] params;
    public Instruction(Type type, String ... params) {
        this.type = type;
        this.params = params;
    }
    public String toString() {
        switch (type) {
            case LABEL: return params[0] + ": ";
            case INT:
            case ID: return params[0] + " = " + params[1];
            case FUNC: return params[0] + " = @ " + params[1];
            case ADD: return params[0] + " = " + params[1] + " + " + params[2];
            case SUB: return params[0] + " = " + params[1] + " - " + params[2];
            case MULT: return params[0] + " = " + params[1] + " * " + params[2];
            case LESS: return params[0] + " = " + params[1] + " < " + params[2];
            case INDEX: return params[0] + " = [ " + params[1] + " + " + params[2] + " ]";
            case ARRAY: return "[ " + params[0] + " + " + params[1] + " ] = " + params[2];
            case ALLOC: return params[0] + " = alloc ( " + params[1] + " )";
            case PRINT: return "print ( " + params[0] + " )";
            case ERROR: return "error ( " + params[0] + " )";
            case GOTO: return "goto " + params[0];
            case IF0: return "if0 ( " + params[0] + " ) goto " + params[1];
            case CALL: return params[0] + " = call " + params[1] + " ( " + (params.length == 3 ? params[2] : "") + " )";
            default: return "";
        }
    }
}
