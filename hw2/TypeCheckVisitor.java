import visitor.DepthFirstVisitor;
import syntaxtree.*;
public class TypeCheckVisitor extends DepthFirstVisitor {
    /*@Override
    public void visit(Goal node) {
        //Type check Goal
        //  ⊢ g
        *//*
         * f0 -> MainClass()
         * f1 -> ( TypeDeclaration() )*
         * f2 -> <EOF>
         *//*
        node.f0.accept(this);
        node.f1.accept(this);
        node.f2.accept(this);
    }*/


}
