import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import parsetree.*;

public class Lexer {
	public Queue<Terminal> getTokens() {
		return (Queue<Terminal>) tokens.clone();
	}



	private final LinkedList<Terminal> tokens;
	private final BufferedReader scanner;

	public Lexer(BufferedReader scanner) throws IOException {
		this.scanner = scanner;
		tokens = new LinkedList<>();
		int pos = 0;
		int next = this.scanner.read();
		Terminal nextToken;
		while (next != -1) {
			if (next == ' ' || next == '\t' || next == '\n' || next == '\r') {
				pos++;
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
					pos += this.findInBuffer("System.out.println", pos);
					nextToken = Terminal.SYS_OUT_PRINTLN;
					break;
				case 'i' :
					pos += this.findInBuffer("if", pos);
					nextToken = Terminal.IF;
					break;
				case 'e' :
					pos += this.findInBuffer("else", pos);
					nextToken = Terminal.ELSE;
					break;
				case 'w' :
					pos += this.findInBuffer("while", pos);
					nextToken = Terminal.WHILE;
					break;
				case 't' :
					pos += this.findInBuffer("true", pos);
					nextToken = Terminal.TRUE;
					break;
				case 'f' :
					pos += this.findInBuffer("false", pos);
					nextToken = Terminal.FALSE;
					break;
				default :
					throw new InvalidTokenException((char) next, pos);
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

	/*@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int tablevel = 0;
		for (Terminal token : tokens) {
			switch (token) {
				case LPAREN: case RPAREN: case EXCLAMATION: case TRUE: case FALSE:
					sb.append(token.pattern);
					break;
				case SYS_OUT_PRINTLN: case ELSE:
					sb.append("\t".repeat(tablevel));
					sb.append(token.pattern);
					break;
				case SEMICOLON:
					sb.append(token.pattern);
					sb.append('\n');
					break;
				case IF: case WHILE:
					sb.append("\t".repeat(tablevel));
					sb.append(token.pattern);
					sb.append(' ');
					break;
				case LBRACKET:
					sb.append(' ');
					sb.append(token.pattern);
					sb.append('\n');
					tablevel++;
					break;
				case RBRACKET:
					tablevel--;
					sb.append("\t".repeat(tablevel));
					sb.append(token.pattern);
					sb.append('\n');
					break;
			}
		}
		return sb.toString();
	}*/
}
