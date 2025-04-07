import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class Lexer {
	private ArrayList<Token> tokens;
	private int position = 0;
	private BufferedReader scanner;

	public Lexer(BufferedReader scanner) throws IOException {
		this.scanner = scanner;
		tokens = new ArrayList<>();
		int pos = 0;
		int next = this.scanner.read();
		Token nextToken;
		while (next != -1) {
			if (next == ' ' || next == '\t' || next == '\n' || next == '\r') {
				pos++;
				next = this.scanner.read();
				continue;
			}
			nextToken =
				switch (next) {
					case '{' -> Token.LBRACKET;
					case '}' -> Token.RBRACKET;
					case '(' -> Token.LPAREN;
					case ')' -> Token.RPAREN;
					case ';' -> Token.SEMICOLON;
					case '!' -> Token.EXCLAMATION;
					case 'S' -> {
						pos += this.findInBuffer("System.out.println", pos);
						yield Token.SYS_OUT_PRINTLN;
					}
					case 'i' -> {
						pos += this.findInBuffer("if", pos);
						yield Token.IF;
					}
					case 'e' -> {
						pos += this.findInBuffer("else", pos);
						yield Token.ELSE;
					}
					case 'w' -> {
						pos += this.findInBuffer("while", pos);
						yield Token.WHILE;
					}
					case 't' -> {
						pos += this.findInBuffer("true", pos);
						yield Token.TRUE;
					}
					case 'f' -> {
						pos += this.findInBuffer("false", pos);
						yield Token.FALSE;
					}
					default -> {
						throw new InvalidTokenException((char) next, pos);
					}
				};
			tokens.add(nextToken);
			pos++;
			next = this.scanner.read();
		}
	}

	private int findInBuffer(String s, int pos) throws IOException {
		char start = s.charAt(0);
		int str_len = s.length();
		char[] cbuf = new char[str_len];
		cbuf[0] = start;
		if (this.scanner.read(cbuf, 1, str_len-1) != str_len-1 ||
								!Arrays.equals(cbuf, s.toCharArray())) {
			throw new InvalidTokenException(start, pos);
		}
		return str_len-1;
	}
}
