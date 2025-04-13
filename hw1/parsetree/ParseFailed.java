package parsetree;

/**
 * Generic exception, which was used in debugging to more easily identify when a
 * failure was or was not due to the Parser implementation
 */
public class ParseFailed extends RuntimeException {
	public ParseFailed(String message) {
		super(message);
	}
}
