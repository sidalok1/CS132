package sparrowCode;

import java.util.LinkedList;
import java.util.StringJoiner;

public class Program {
    public LinkedList<FunDecl> functions;
    public Program(LinkedList<FunDecl> functions) {
        this.functions = functions;
    }
    public Program() {
        this.functions = new LinkedList<>();
    }
    public LinkedList<FunDecl> functions() {
        return new LinkedList<>(functions);
    }
    public FunDecl main() {
        return functions().peekFirst();
    }
    public String toString() {
        StringJoiner sj = new StringJoiner("\n\n");
        for (FunDecl fun : functions()) { sj.add(fun.toString()); }
        return sj.toString();
    }

}
