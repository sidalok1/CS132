import miniJavaParser.MiniJavaParser;
import miniJavaParser.ParseException;
import syntaxtree.*;
import visitor.*;
import java.util.HashSet;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Main function. Redirect file contents of the file you want to typecheck
 */
public class Typecheck {
    public static void main(String[] args) throws ParseException {
        try {
            new MiniJavaParser(new InputStreamReader(System.in));
            Goal root = MiniJavaParser.Goal();
            TypeCheckVisitor visitor = new TypeCheckVisitor();
            visitor.visit(root);
        } catch ( Exception e ) {
            System.out.println("Type error");
            return;
        }
        System.out.println("Program type checked successfully");
    }

}
