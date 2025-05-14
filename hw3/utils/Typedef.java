package utils;

import java.util.LinkedHashSet;

public final class Typedef {
    public String classname;
    public LinkedHashSet<String> fields;
    public LinkedHashSet<String> methods;

    public Typedef(String classname, LinkedHashSet<String> fields, LinkedHashSet<String> methods) {
        this.classname = classname;
        this.fields = fields;
        this.methods = methods;
    }
    public Typedef() {
        classname = null;
        fields = null;
        methods = null;
    }
}
