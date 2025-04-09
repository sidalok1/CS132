package parsetree;

/**
 * Data structure representing a Token, or Terminal symbol
 * @see Symbol
 */
public enum Terminal implements Symbol {
	LBRACKET("{"),
	RBRACKET("}"),
	SYS_OUT_PRINTLN("System.out.println"),
	LPAREN("("),
	RPAREN(")"),
	SEMICOLON(";"),
	IF("if"),
	ELSE("else"),
	WHILE("while"),
	TRUE("true"),
	FALSE("false"),
	EXCLAMATION("!");

	public final String pattern;

	Terminal(String s) {
		this.pattern = s;
	}

	/**
	 * The underlying token, as a string
	 * @return token string
	 */
	@Override
	public String toString() {
		return this.pattern;
	}
}