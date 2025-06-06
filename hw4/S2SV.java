import java.io.InputStream;

import IR.SparrowParser;
import IR.syntaxtree.*;

public class S2SV {
    public static void main(String [] args) throws Exception {
        InputStream in = System.in;
        new SparrowParser(in);
        Program root = SparrowParser.Program();
        RegisterAllocator program = new RegisterAllocator(root);
        System.out.println(program.toString());
    }
}