import java.io.File;
import java.io.FileNotFoundException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.ArrayList;

public class Lexer {
	private ArrayList<Token> tokens;
	private int position = 0;

	public Lexer(File file) {
		Scanner scanner;
		try {
			scanner = new Scanner(file);
		} catch (FileNotFoundException e) {
			System.out.println("Lexer could not open file "
					+ file.getAbsolutePath());
			throw new RuntimeException(e);
		}
		int pos = 0;
		tokens = new ArrayList<>();
		while (scanner.hasNext()) {
			String token_from_file = scanner.next();
			try {
				tokens.add(Token.match(token_from_file, pos));
			}
			catch (InvalidTokenException e) {
				throw new NoSuchElementException(e.getMessage());
			}
			pos++;
		}
	}
}
