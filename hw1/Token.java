import java.util.regex.Pattern;

public enum Token {
	LBRACKET("\\{"),
	RBRACKET("\\}"),
	SYS_OUT_PRINTLN("System.out.println"),
	LPAREN("\\("),
	RPAREN("\\)"),
	SEMICOLON(";"),
	IF("if"),
	ELSE("else"),
	WHILE("while"),
	TRUE("true"),
	FALSE("false"),
	EXCLAMATION("!");

	private final Pattern pattern;

	Token(String s) {
		this.pattern = Pattern.compile(s);
	}

	public static Token match(String s, int pos) {
		for (Token t : Token.values()) {
			if (t.pattern.matcher(s).matches()) {
				return t;
			}
		}
		throw new InvalidTokenException(s, pos);
	}
}
