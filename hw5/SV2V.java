import java.io.InputStream;

import IR.SparrowParser;
import IR.visitor.SparrowVConstructor;
import IR.syntaxtree.Node;
import IR.registers.Registers;

import sparrowv.Program;

public class SV2V {
    public static void main(String[] args) throws Exception {
        Registers.SetRiscVregs();
        InputStream in = System.in;
        new SparrowParser(in);
        Node root = SparrowParser.Program();
        SparrowVConstructor constructor = new SparrowVConstructor();
        root.accept(constructor);
        Program program = constructor.getProgram();
        CodeGenerator generator = new CodeGenerator(program);
        System.out.println(generator);
    }
}
