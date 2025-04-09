import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Parse {
	/**
	 * Parse class main method: entry point into the parser.
	 * <p>
	 * The contents of the file to be parsed should be REDIRECTED to the
	 * process via standard input. The program will not attempt to open a
	 * file passed as an argument.
	 * @param args Unused.
	 */
	public static void main(String[] args) {
		Lexer lexer;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			lexer = new Lexer(reader);
		} catch (Exception e) {
			System.out.println("Parse error");
			return;
		}
		try {
			new Parser(lexer.getTokens());
			System.out.println("Program parsed successfully");
		} catch (Exception e) {
			System.out.println("Parse error");
		}
	}
}