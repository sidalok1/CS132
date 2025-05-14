package sparrowCode;

import java.util.LinkedList;
import java.util.StringJoiner;

public class FunDecl {
    public String FunctionName;
    public LinkedList<String> paramIDs;
    public CodeBlock block;
    public FunDecl(String functionName, LinkedList<String> paramIDs, CodeBlock block) {
        FunctionName = functionName;
        this.paramIDs = paramIDs;
        this.block = block;
    }
    public FunDecl() {
        FunctionName = "";
        paramIDs = new LinkedList<>();
        block = null;
    }
    public String toString() {
        StringJoiner sj = new StringJoiner(" ", " ( ", " )");
        for (String paramID : paramIDs) { sj.add(paramID); }
        return "func " + FunctionName + sj.toString() + "\n" + block.toString();

    }
}
