package utils;

public class TypeCheckFailed extends RuntimeException {
    public TypeCheckFailed(String message) {
        super(message);
    }
}
