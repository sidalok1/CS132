import minijava.*;
import minijava.syntaxtree.*;
import java.io.InputStreamReader;

public class J2S {
    public static void main(String[] args) {
        new MiniJavaParser(new InputStreamReader(System.in));
        try {
            Goal root = MiniJavaParser.Goal();
            Translator t = new Translator(root);
            System.out.println(t.getProgram());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
