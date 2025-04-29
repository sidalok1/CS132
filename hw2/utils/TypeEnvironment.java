package utils;

import java.util.HashMap;
import java.util.Stack;

public class TypeEnvironment extends HashMap<String, Types> {

    public TypeEnvironment copy() {
        TypeEnvironment tEnv = new TypeEnvironment();
        tEnv.putAll(this);
        return tEnv;
    }
    public TypeEnvironment merge(TypeEnvironment higherPrecedence) {
        TypeEnvironment tEnv = this.copy();
        tEnv.putAll(higherPrecedence);
        return tEnv;
    }
}
