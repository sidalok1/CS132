package hw4utils;
import IR.token.Identifier;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringJoiner;

public class CodeBlock {
    public LinkedList<Inst> instructions;
    public Identifier returnID;
    public CodeBlock(List<Inst> instructions, Identifier returnID) {
        this.instructions = new LinkedList<>(instructions);
        this.returnID = returnID;
    }
    public CodeBlock() {
        this.instructions = new LinkedList<>();
        this.returnID = null;
    }

    public void add(Inst inst) {
        instructions.add(inst);
    }
    public void front(Inst inst) {
        instructions.addFirst(inst);
    }
    public void back(Inst inst) {
        instructions.addLast(inst);
    }

    public String toString() {
        StringJoiner sj = new StringJoiner("\n");
        for (Inst instruction : instructions) { sj.add(instruction.toString()); }
        if ( returnID != null ) {
            sj.add("return " + returnID);
        } else {
            sj.add("return");
        }
        return sj.toString();
    }
}
