import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import parsetree.*;

public class Lexer {
	private final LinkedList<Terminal> tokens;
	public Queue<Terminal> getTokens() {
		return tokens;
	}

	private final BufferedReader scanner;

	/**
	 * Constructor for the Lexer, which takes in a BufferedReader and automatically
	 * parses for tokens from the grammar.
	 * @param scanner Standard Input buffered reader
	 * @throws IOException Thrown if reading from buffer fails
	 * @throws InvalidTokenException Generic exception indicating Lexer failed.
	 */
	public Lexer(BufferedReader scanner) throws InvalidTokenException, IOException {
		this.scanner = scanner;
		tokens = new LinkedList<>();
		int next = this.scanner.read();
		Terminal nextToken;
		while (next != -1) {
			//Skip whitespace
			if (next == ' ' || next == '\t' || next == '\n' || next == '\r') {
				next = this.scanner.read();
				continue;
			}
			switch (next) {
				case '{':
					nextToken = Terminal.LBRACKET;
					break;
				case '}':
					nextToken = Terminal.RBRACKET;
					break;
				case '(' :
					nextToken =  Terminal.LPAREN;
					break;
				case ')' :
					nextToken =  Terminal.RPAREN;
					break;
				case ';' :
					nextToken =  Terminal.SEMICOLON;
					break;
				case '!' :
					nextToken =  Terminal.EXCLAMATION;
					break;
				case 'S':
					this.findInBuffer("System.out.println");
					nextToken = Terminal.SYS_OUT_PRINTLN;
					break;
				case 'i' :
					this.findInBuffer("if");
					nextToken = Terminal.IF;
					break;
				case 'e' :
					this.findInBuffer("else");
					nextToken = Terminal.ELSE;
					break;
				case 'w' :
					this.findInBuffer("while");
					nextToken = Terminal.WHILE;
					break;
				case 't' :
					this.findInBuffer("true");
					nextToken = Terminal.TRUE;
					break;
				case 'f' :
					this.findInBuffer("false");
					nextToken = Terminal.FALSE;
					break;
				default :
					throw new InvalidTokenException("Could not find character '" + next + "' in input stream");
			}
			tokens.add(nextToken);
			next = this.scanner.read();
		}
	}

	/**
	 * For use when you know what the next token in the buffer must be
	 * @param s String to search for
	 * @throws InvalidTokenException Generic Lexer failure exception
	 * @throws IOException Reading from buffer failed
	 */
	private void findInBuffer(String s) throws InvalidTokenException, IOException {
		char start = s.charAt(0);
		int str_len = s.length();
		char[] cbuf = new char[str_len];
		cbuf[0] = start;
		if (this.scanner.read(cbuf, 1, str_len-1) != str_len-1 ||
								!Arrays.equals(cbuf, s.toCharArray())) {
			throw new InvalidTokenException("Could not find '" + s + "' in buffer");
		}
	}
}
