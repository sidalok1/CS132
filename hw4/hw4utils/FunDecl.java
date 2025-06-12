package hw4utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import IR.token.*;

public class FunDecl {
    public FunctionName FunctionName;
    public List<Identifier> paramIDs;
    public CodeBlock block;
    public FunDecl(FunctionName functionName, List<Identifier> paramIDs, CodeBlock block) {
        FunctionName = functionName;
        this.paramIDs = paramIDs;
        this.block = block;
    }
    public FunDecl() {
        FunctionName = null;
        paramIDs = new ArrayList<Identifier>();
        block = null;
    }
    public String toString() {
        StringJoiner sj = new StringJoiner(" ", " ( ", " )");
        for (Identifier paramID : paramIDs) { sj.add(paramID.toString()); }
        return "func " + FunctionName + sj.toString() + "\n" +
                block.toString();

    }
}
