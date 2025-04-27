package utils;

import java.util.HashMap;
import java.util.Stack;

public class TypeEnvironment extends HashMap<String, Stack<Types>> {
    public Graph classRelations;
    public void push(String id, Types t) {
        Stack<Types> stack = this.get(id);
        if (stack == null) {
            stack = new Stack<>();
            stack.push(t);
            this.put(id, stack);
        } else {
            stack.push(t);
        }
    }
    public Types pop(String id) {
        Stack<Types> stack = this.get(id);
        if (stack != null) {
            return stack.pop();
        } else {
            return null;
        }
    }

    public Stack<Types> getCopy(String id) {
        Stack<Types> stack = new Stack<>();
        stack.addAll(this.getOrDefault(id, new Stack<>()));
        return stack;
    }

    public TypeEnvironment copy() {
        TypeEnvironment tEnv = new TypeEnvironment();
        tEnv.putAll(this);
        return tEnv;
    }
    public TypeEnvironment merge(TypeEnvironment higherPrecedence) {
        TypeEnvironment tEnv = this.copy();
        for (Entry<String, Stack<Types>> e : higherPrecedence.entrySet()) {
            Stack<Types> env = tEnv.putIfAbsent(e.getKey(), e.getValue());
            if (env != null) {
                env.addAll(e.getValue());
            }
        }
        return tEnv;
    }
}
