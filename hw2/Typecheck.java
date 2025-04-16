import miniJavaParser.MiniJavaParser;
import syntaxtree.Goal;
import syntaxtree.Node;
import visitor.*;

import java.io.IOException;
import java.io.InputStreamReader;

public class Typecheck {
    public static void main(String[] args) {
        try {
            new MiniJavaParser(new InputStreamReader(System.in));
            Goal root = MiniJavaParser.Goal();
            TypeCheckVisitor visitor = new TypeCheckVisitor();
            visitor.visit(root);
        } catch ( Exception e ) {
            System.out.println("Type error");
        }
        System.out.println("Program type checked successfully");
    }
}
