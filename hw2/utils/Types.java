package utils;

public enum Types {
    INT,
    BOOL,
    ARRAY,
    CLASS;

    private String className;
    private boolean nameSet;
    public void setClassName(String name) {
        if (!nameSet && this.equals(Types.CLASS)) {
            this.className = name;
        }
    }

    public static class NoNameException extends RuntimeException {}

    public String getClassName() throws NoNameException {
        if (nameSet) return this.className;
        else throw new NoNameException(); }
}
