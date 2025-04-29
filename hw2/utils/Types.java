package utils;

public final class Types {
    public static final TypeEnum INT = TypeEnum.INT;
    public static final TypeEnum BOOL = TypeEnum.BOOL;
    public static final TypeEnum ARRAY = TypeEnum.ARRAY;
    public static final TypeEnum CLASS = TypeEnum.CLASS;
    public static final TypeEnum NULL = TypeEnum.NULL;
    public static enum TypeEnum {
        INT,
        BOOL,
        ARRAY,
        CLASS,
        NULL;
    }
    private final TypeEnum type;
    private final String className;
    public Types(TypeEnum type) {
        this.type = type;
        this.className = null;
    }
    public Types(TypeEnum type, String className) {
        this.type = type;
        this.className = className;
    }

    public TypeEnum type() { return type; }
    public String className() { return className; }
    public String toString() {
        if (this.type == TypeEnum.CLASS) {
            return "class " + className;
        }
        else {
            return type.toString();
        }
    }
    public boolean equals(Types other) {
        if (this.type.equals(other.type)) {
            if (this.type == TypeEnum.CLASS) {
                assert this.className != null;
                return this.className.equals(other.className);
            }
            else {
                return true;
            }
        }
        else {
            return false;
        }
    }

    public boolean INT() { return this.type == TypeEnum.INT; }
    public boolean BOOL() { return this.type == TypeEnum.BOOL; }
    public boolean ARRAY() { return this.type == TypeEnum.ARRAY; }
    public boolean CLASS() { return this.type == TypeEnum.CLASS; }
    public boolean NULL() { return this.type == TypeEnum.NULL; }

    public static class NoNameException extends RuntimeException {}

}
