import java.io.File;
import java.nio.file.Path;

import static java.nio.file.Files.newBufferedReader;

public class Parse {
	public static void main(String[] args) {
		if (args.length == 2 && args[0].equals("<")) {
			File file = null;
			Lexer lexer;
			try {
				lexer = new Lexer(newBufferedReader(Path.of(args[1])));
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			System.out.println(lexer.toString());
		}
	}
}