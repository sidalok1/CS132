package parsetree;

/**
 * Generic exception, which was used in debugging to more easily identify when a
 * failure was or was not due to the Lexer implementation
 */
public class InvalidTokenException extends RuntimeException {
	public InvalidTokenException(String message) { super(message); }
}