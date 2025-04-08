package parsetree;

public class ParseFailed extends RuntimeException {
	public ParseFailed(String message) {
		super(message);
	}
}
