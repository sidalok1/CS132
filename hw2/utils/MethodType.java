package utils;

import java.util.LinkedHashMap;
import java.util.Stack;

// Java 8 does not have record classes, this is an equivalent implementation
public final class MethodType {
    private final LinkedHashMap<String, Types> parameters;
    private final Types returnType;
    public MethodType( LinkedHashMap<String, Types> parameters, Types returnType ) {
        this.parameters = parameters;
        this.returnType = returnType;
    }
    public LinkedHashMap<String, Types> parameters() { return parameters; }
    public Types returnType() { return returnType; }

    public String toString() { return parameters.toString() + " -> " + returnType.toString(); }

    public boolean equals(MethodType other) {
        Stack<Types> a = new Stack<Types>();
        a.addAll(parameters().values());
        Stack<Types> b = new Stack<Types>();
        b.addAll(other.parameters().values());
        if (a.size() != b.size()) return false;
        while (!a.isEmpty()) {
            Types t1 = a.pop();
            Types t2 = b.pop();
            if (!t1.equals(t2)) return false;
        }
        return returnType.equals(other.returnType);
    }
}
