package hw4utils;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringJoiner;

public class CodeBlock {
    public LinkedList<Inst> instructions;
    public String returnID;
    public CodeBlock(List<Inst> instructions, String returnID) {
        this.instructions = new LinkedList<>(instructions);
        this.returnID = returnID;
    }
    public CodeBlock() {
        this.instructions = new LinkedList<>();
        this.returnID = "";
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
