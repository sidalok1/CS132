public enum Token {
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

	Token(String s) {
		this.pattern = s;
	}

	@Override
	public String toString() {
		return this.pattern;
	}
}
