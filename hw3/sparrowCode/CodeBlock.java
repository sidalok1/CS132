package sparrowCode;
import java.util.LinkedList;
import java.util.StringJoiner;

public class CodeBlock {
    public LinkedList<Instruction> instructions;
    public String returnID;
    public CodeBlock(LinkedList<Instruction> instructions, String returnID) {
        this.instructions = instructions;
        this.returnID = returnID;
    }
    public CodeBlock() {
        this.instructions = new LinkedList<>();
        this.returnID = "";
    }

    public String toString() {
        StringJoiner sj = new StringJoiner("\n\t", "\t", "");
        for (Instruction instruction : instructions) { sj.add(instruction.toString()); }
        if ( returnID != null ) {
            sj.add("return " + returnID);
        } else {
            sj.add("return");
        }
        return sj.toString();
    }
}
