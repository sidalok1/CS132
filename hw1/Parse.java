import parsetree.ParseFailed;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.ParseException;

import static java.nio.file.Files.newBufferedReader;

public class Parse {
	public static void main(String[] args) {
		if (args.length == 2 && args[0].equals("<")) {
			File file = null;
			Lexer lexer;
			try {
				Path path = FileSystems.getDefault().getPath(args[1]);
				lexer = new Lexer(newBufferedReader(path));
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			Parser parser;
			try {
				parser = new Parser(lexer.getTokens());
			} catch (ParseFailed e) {
				System.out.println("Parse error");
				return;
			}
			System.out.println("Program parsed successfully");
		}
	}
}