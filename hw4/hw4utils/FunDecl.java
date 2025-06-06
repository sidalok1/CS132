package hw4utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class FunDecl {
    public String FunctionName;
    public List<String> paramIDs;
    public CodeBlock block;
    public FunDecl(String functionName, List<String> paramIDs, CodeBlock block) {
        FunctionName = functionName;
        this.paramIDs = paramIDs;
        this.block = block;
    }
    public FunDecl() {
        FunctionName = "";
        paramIDs = new ArrayList<>();
        block = null;
    }
    public String toString() {
        StringJoiner sj = new StringJoiner(" ", " ( ", " )");
        for (String paramID : paramIDs) { sj.add(paramID); }
        return "func " + FunctionName + sj.toString() + "\n" + block.toString();

    }
}
