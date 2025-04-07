import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Parse {
	public static void main(String[] args) {
		if (args.length == 2 && args[0].equals("<")) {
			File file = null;
			try {
				file = new File(args[1]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Lexer lexer = new Lexer(file);
		}
	}
}