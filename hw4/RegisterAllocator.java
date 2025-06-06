import IR.syntaxtree.*;
import IR.visitor.*;
import hw4utils.*;

import java.util.*;

public class RegisterAllocator extends DepthFirstVisitor {

    public Prog program;

    public String toString() {
        return program.toString();
    }

    public RegisterAllocator(Program p) {
        program = new Prog();
        for ( Node n : p.f0.nodes) {
            n.accept(this);
        }
    }

    private String id;
    private String label;
    public void visit(FunctionName n) {
        String fname = n.f0.tokenImage;
    }
    private ArrayList<String> params = null;
    private CodeBlock code;
    LivenessAnalysis analysis;

    private static class InstructionVisitor extends GJNoArguDepthFirst<ArrayList<Instruction>> {
        ArrayList<Instruction> instructions;
        public ArrayList<Instruction> visit(Instruction n) { instructions.add(n); return null; }
        public ArrayList<Instruction> visit(Block n) {
            instructions = new ArrayList<>(n.f0.nodes.size());
            n.f0.accept(this);
            return instructions;
        }
    }
    Reg rd, rs1, rs2;
    HashMap<String, Reg> where;

    public void visit(Block n) {
        int maxsave = analysis.LinearScan(params);
        for (int i = 0; i < maxsave; i++) {
            code.instructions.add(
                    new Inst(Inst.Type.REGtoID, Reg.save(i), "_saved_"+i)
            );
        }
        LinkedList<Interval> intervals = new LinkedList<>(analysis.intervals());
        LinkedList<Interval> active = new LinkedList<>();
        ArrayList<Instruction> instructions = n.accept(new InstructionVisitor());
        where = new HashMap<>();
        for ( Instruction i : instructions ) {
            int time = instructions.indexOf(i)+1;
            LinkedList<Interval> toRemove = new LinkedList<>();
            for ( Interval t : active) {
                if (t.stop <= time) {
                    where.remove(t.id);
                    toRemove.add(t);
                }
            }
            active.removeAll(toRemove);
            for ( Interval t : intervals ) {
                if (t.start <= time) {
                    where.put(t.id, t.reg);
                    active.add(t);
                }
            }
            Iterator<String> _t = analysis.use(i).iterator();
            String s1 = (_t.hasNext()) ? _t.next() : null;
            String s2 = (_t.hasNext()) ? _t.next() : null;
            boolean restore1 = false;
            boolean restore2 = false;
            if ( s1 != null ) {
                rs1 = where.get(s1);
                if ( rs1.equals(Reg.STACK) ) {
                    if (where.containsValue(Reg.reserve(0))) {
                        restore1 = true;
                        code.instructions.add(
                                new Inst(Inst.Type.REGtoID, Reg.reserve(0), "_restore_0")
                        );
                    }
                    code.instructions.add(
                            new Inst(Inst.Type.IDtoREG, Reg.reserve(0), s1)
                    );
                }
            }
            if ( s2 != null ) {
                rs2 = where.get(s2);
                if ( rs2.equals(Reg.STACK) ) {
                    if (where.containsValue(Reg.reserve(1))) {
                        restore2 = true;
                        code.instructions.add(
                                new Inst(Inst.Type.REGtoID, Reg.reserve(1), "_restore_1")
                        );
                    }
                    code.instructions.add(
                            new Inst(Inst.Type.IDtoREG, Reg.reserve(1), s2)
                    );
                }
            }
            _t = analysis.def(i).iterator();
            String def = (_t.hasNext()) ? _t.next() : null;
            boolean writeback = false;
            if ( def != null ) {
                rd = where.get(def);
                if ( rd.equals(Reg.STACK) ) {
                    if (!restore1 && where.containsValue(Reg.reserve(0))) {
                        restore1 = true;
                        code.instructions.add(
                                new Inst(Inst.Type.REGtoID, Reg.reserve(0), "_restore_0")
                        );
                    }
                    writeback = true;
                    rd = Reg.reserve(0);
                }
            }
            i.f0.choice.accept(this);
            if ( writeback ) {
                code.instructions.add(
                        new Inst(Inst.Type.REGtoID, Reg.reserve(0), def)
                );
            }
            if ( restore1 ) {
                code.instructions.add(
                        new Inst(Inst.Type.IDtoREG, Reg.reserve(0), "_restore_0")
                );
            }
            if ( restore2 ) {
                code.instructions.add(
                        new Inst(Inst.Type.IDtoREG, Reg.reserve(1), "_restore_1")
                );
            }
        }
        for (int i = 0; i < maxsave; i++) {
            code.instructions.add(
                    new Inst(Inst.Type.IDtoREG, Reg.save(i), "_saved_"+i)
            );
        }
        String ret = n.f2.f0.tokenImage;
        Reg r = where.get(ret);
        if ( !r.equals(Reg.STACK) ) {
            code.instructions.add(new Inst(Inst.Type.REGtoID, r, ret));
        }
        code.returnID = ret;
    }



    public void visit(Identifier n) { id = n.f0.tokenImage; }

    public void visit(FunctionDeclaration n) {
        code = new CodeBlock();
        where = new HashMap<>();
        int instructionCount = 1 + n.f5.f0.nodes.size();
        params = new ArrayList<>(n.f3.nodes.size());
        for ( Node i : n.f3.nodes ) { i.accept(this); params.add(id); }
        String fname = n.f1.f0.tokenImage;
        analysis = new LivenessAnalysis(n);
        n.f5.accept(this);
        int argpos = Math.min(params.size(), 6);
        params.subList(0, argpos).clear();
        program.functions.add(new FunDecl(fname, params, code));
    }

    public void visit(Label n) { label = n.f0.tokenImage; }
    public void visit(LabelWithColon n) {
        n.f0.accept(this);
        code.instructions.add(new Inst(Inst.Type.LABEL, label));
    }

    public void visit(SetInteger n) {
        code.instructions.add(new Inst(Inst.Type.INT, rd, n.f2.f0.tokenImage));
    }

    public void visit(SetFuncName n) {
        code.instructions.add(new Inst(Inst.Type.FUNC, rd, n.f3.f0.tokenImage));
    }

    public void visit(Add n) {
        code.instructions.add(new Inst(Inst.Type.ADD, rd, rs1, rs2));
    }
    public void visit(Subtract n) {
        code.instructions.add(new Inst(Inst.Type.SUB, rd, rs1, rs2));
    }
    public void visit(Multiply n) {
        code.instructions.add(new Inst(Inst.Type.MULT, rd, rs1, rs2));
    }
    public void visit(LessThan n) {
        code.instructions.add(new Inst(Inst.Type.LESS, rd, rs1, rs2));
    }

    public void visit(Load n) {
        int imm = Integer.parseInt(n.f5.f0.tokenImage);
        code.instructions.add(new Inst(rd, rs1, imm));
    }

    public void visit(Store n) {
        int imm = Integer.parseInt(n.f3.f0.tokenImage);
        code.instructions.add(new Inst(rs1, imm, rs2));
    }

    public void visit(Move n) {
        code.instructions.add(new Inst(Inst.Type.REG, rd, rs1));
    }

    public void visit(Alloc n) {
        code.instructions.add(new Inst(Inst.Type.ALLOC, rd, rs1));
    }

    public void visit(Print n) {
        code.instructions.add(new Inst(rs1));
    }

    public void visit(ErrorMessage n) {
        String msg = n.f2.f0.tokenImage;
        code.instructions.add(new Inst(Inst.Type.ERROR, msg));
    }

    public void visit(Goto n) {
        code.instructions.add(new Inst(Inst.Type.GOTO, n.f1.f0.tokenImage));
    }

    public void visit(If n) {
        String l = n.f3.f0.tokenImage;
        code.instructions.add(new Inst(Inst.Type.IF0, rs1, l));
    }
    public void visit(IfGoto n) {
        String l = n.f3.f0.tokenImage;
        code.instructions.add(new Inst(Inst.Type.IF0, rs1, l));
    }

    public void visit(Call n) {
        LinkedList<String> args = new LinkedList<>();
        for ( Node i : n.f5.nodes ) {
            i.accept(this);
            args.add(id);
        }
        int i = 0;
        while ( !args.isEmpty() && i < 6 ) {
            String arg = args.pollFirst();
            Reg reg = where.get(arg);
            if ( reg.equals(Reg.STACK) ) {
                code.instructions.add(new Inst(Inst.Type.IDtoREG, Reg.arg(i), arg));
            } else {
                code.instructions.add(new Inst(Inst.Type.REG, Reg.arg(i), reg));
            }
            i++;
        }
        EnumMap<Reg, String> temp_restore1 = new EnumMap<>(Reg.class);
        EnumMap<Reg, Reg> temp_restore2 = new EnumMap<>(Reg.class);
        int j = 0;
        for ( Reg t : Reg.tempset ) {
            if ( where.containsValue(t) && !t.equals(rd) ) { // t needs to be saved
                String str = "_temp_"+j;
                j++;
                Reg loc = null;
                if ( !where.values().containsAll(Reg.saveset) ) { // t can be save in regfile
                    for ( Reg s : Reg.saveset ) {
                        if ( !where.containsValue(s) ) {
                            where.put(str, s);
                            loc = s;
                            break;
                        }
                    }
                    code.instructions.add(new Inst(Inst.Type.REG, loc, t));
                    temp_restore2.put(t, loc);
                } else { // t must be put on stack
                    temp_restore1.put(t, str);
                    code.instructions.add(new Inst(Inst.Type.REGtoID, t, str));
                }
            }
        }
        code.instructions.add(new Inst(rd, rs1, args));
        for ( Reg t : temp_restore1.keySet() ) {
            code.instructions.add(new Inst(Inst.Type.IDtoREG, t, temp_restore1.get(t)));
        }
        for ( Reg t : temp_restore2.keySet() ) {

            code.instructions.add(new Inst(Inst.Type.REG, t, temp_restore2.get(t)));
        }
        for ( ; j >= 0; j-- ) {
            String str = "_temp_"+j;
            where.remove(str);
        }
    }

}
