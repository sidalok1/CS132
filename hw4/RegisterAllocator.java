import IR.token.Identifier;
import sparrow.*;
import sparrow.visitor.*;
import hw4utils.*;

import java.util.*;

public class RegisterAllocator implements Visitor {

    public Prog program;

    public String toString() {
        return program.toString();
    }

    public RegisterAllocator(Program p) {
        program = new Prog();
        for ( FunctionDecl n : p.funDecls) {
            n.accept(this);
        }
    }

    private String id;
    private String label;
    private ArrayList<Identifier> params = null;
    private CodeBlock code;
    LivenessAnalysis analysis;

    public void visit(Program n) {
        program = new Prog();
        n.funDecls.forEach(f -> f.accept(this));
    }
    FunDecl fun;
    boolean isMain;
    public void visit(FunctionDecl n) {
        analysis = new LivenessAnalysis(n);
        params = new ArrayList<>(n.formalParameters);
        ArrayList<Identifier> ids = new ArrayList<>();
        if (params.size() > 6) {
            params.subList(6, params.size()).forEach(id -> ids.add(mangle(id)));
        }
        isMain = n.parent.funDecls.indexOf(n) == 0;
        n.block.accept(this);
        fun = new FunDecl(n.functionName, ids, code);
        program.functions.add(fun);
    }
    EnumSet<Reg> usedSaveRegister;

    private static EnumSet<Reg> tempset = Reg.tempset;
    private static EnumSet<Reg> saveset = EnumSet.of(
            Reg.s1, Reg.s2, Reg.s3, Reg.s4, Reg.s5, Reg.s6, Reg.s7, Reg.s8, Reg.s9
    );

    TreeMap<Identifier, Reg> where;
    private Reg assign(Identifier id) {
        Reg r = null;
        if ( !where.values().containsAll(tempset) ) {
            for ( Reg t : tempset ) {
                if (!where.containsValue(t)) {
                    r = t;
                    break;
                }
            }
        } else if ( !where.values().containsAll(saveset) ) {
            for ( Reg s : saveset ) {
                if (!where.containsValue(s)) {
                    usedSaveRegister.add(s);
                    r = s;
                    break;
                }
            }
        } else {
            for ( Interval ivl : active ) {
                if (ivl.start != time) {
                    r = where.remove(ivl.id);
                    code.add( new Inst(mangle(ivl.id), r) );
                    where.put(ivl.id, Reg.STACK);
                    break;
                }
            }
        }
        if ( where.containsKey(id) ) {
            Reg source = where.remove(id);
            assert ( Reg.argset.contains(source) || source == Reg.STACK );
            if ( source == Reg.STACK ) {
                code.add(new Inst(r, mangle(id)));
            } else {
                code.add(new Inst(r, source, true));
            }
        }
        assert ( r != null );
        where.put(id, r);
        return r;
    }
    private void remove(Identifier id) {
        assert (where.containsKey(id));
        where.remove(id);
    }
    Reg rs1, rs2;
    ArrayList<Identifier> callArgs;
    private class ReadVisitor implements Visitor {
        public Reg read(Identifier id, int i) {
            Reg r = where.get(id);
            if ( r == Reg.STACK ) {
                Reg t = i == 0 ? Reg.s10 : Reg.s11;
                code.add( new Inst(t, mangle(id)));
                return t;
            } else {
                return r;
            }
        }
        public Reg read(Identifier id) {
            assert (where.containsKey(id));
            Reg r = where.get(id);
            if ( r == Reg.STACK ) {
                for ( Interval ivl : active ) {
                    if ( ivl.start != time ) { // otherwise you'll evict a variable being defined
                        r = where.remove(ivl.id);
                        code.add( new Inst(mangle(ivl.id), r) );
                        where.put(ivl.id, Reg.STACK);
                        code.add( new Inst(r, mangle(id)) );
                        where.put(id, r);
                        return r;
                    }
                }
            } else {
                return r;
            }
            assert (false);
            return null;
        }
        public void visit(Program p) {}
        public void visit(FunctionDecl n) {}
        public void visit(Block n) {}
        public void visit(Add n) { rs1 = read(n.arg1, 0); rs2 = read(n.arg2, 1); }
        public void visit(Alloc n) { rs1 = read(n.size, 0); }
        public void visit(Call n) {
            ArrayList<Identifier> parameters = new ArrayList<>(n.args);
            for ( int i = 0 ; i < parameters.size() ; i++ ) {
                Identifier id = parameters.get(i);
                Reg loc = where.get(id);
                Reg arg = Reg.arg(i);
                if ( arg == Reg.STACK && loc != Reg.STACK ) {
                    code.add(new Inst(mangle(id), loc));
                } else if ( arg != Reg.STACK && loc == Reg.STACK ) {
                    code.add(new Inst(arg, mangle(id)));
                } else if (arg != Reg.STACK) {
                    code.add(new Inst(arg, loc, true));
                }
            }
            rs1 = read(n.callee, 0);
            if ( parameters.size() > 6) {
                callArgs = new ArrayList<>(parameters.size()-6);
                parameters.subList(6, parameters.size()).forEach(id -> callArgs.add(mangle(id)));
            } else {
                callArgs = new ArrayList<>();
            }
        }
        public void visit(ErrorMessage n) {}
        public void visit(Goto n) {}
        public void visit(IfGoto n) { rs1 = read(n.condition, 0); }
        public void visit(LabelInstr n) {}
        public void visit(LessThan n) { rs1 = read(n.arg1, 0); rs2 = read(n.arg2, 1); }
        public void visit(Load n) { rs1 = read(n.base, 0); }
        public void visit(Move_Id_FuncName n) {}
        public void visit(Move_Id_Id n) { rs1 = read(n.rhs, 0); }
        public void visit(Move_Id_Integer n) {}
        public void visit(Multiply n) { rs1 = read(n.arg1, 0); rs2 = read(n.arg2, 1); }
        public void visit(Print n) { rs1 = read(n.content); }
        public void visit(Store n) { rs1 = read(n.base, 0); rs2 = read(n.rhs, 1); }
        public void visit(Subtract n) { rs1 = read(n.arg1, 0); rs2 = read(n.arg2, 1); }
    }
    ReadVisitor read = new ReadVisitor();
    TreeSet<Interval> active;
    int time;
    private Identifier mangle(Identifier id) {
        return new Identifier("_" + id.toString());
    }
    public void visit(Block n) {
        code = new CodeBlock();
        Identifier ret = n.return_id;
        usedSaveRegister = EnumSet.noneOf(Reg.class);
        this.where = new TreeMap<>(new LivenessAnalysis.IDComparator());
        TreeSet<Interval> intervals = this.analysis.intervals;
        active = new TreeSet<>(new IntervalComparator(IntervalComparator.Type.END));

        for ( int i = 0; i < params.size(); i++) {
            Identifier id = params.get(i);
            where.put(id, Reg.arg(i));
        }
        for ( time = 0 ; time < n.instructions.size() ; time++ ) {
            Instruction inst = n.instructions.get(time);
            inst.accept(read);
            List<Interval> activate = new ArrayList<>(), kill = new ArrayList<>() ;
            for ( Interval ivl : this.active ) {
                if ( ivl.stop == time ) {
                    kill.add(ivl);
                    this.remove(ivl.id);
                }
            }
            this.active.removeAll(kill);
            for ( Interval ivl : intervals ) {
                if ( ivl.start == time ) {
                    activate.add(ivl);
                    this.assign(ivl.id);
                }
            }
            this.active.addAll(activate);
            intervals.removeAll(activate);
            inst.accept(this);
        }
        if ( where.get(ret) != Reg.STACK ) {
            code.add(new Inst(mangle(ret),where.get(ret)));
        }
        if ( !isMain ) {
            for ( Reg s : usedSaveRegister ) {
                Identifier save = new Identifier("save_" + s.name() );
                code.front(new Inst(save, s));
                code.back(new Inst(s, save));
            }
        }
        code.returnID = mangle(ret);
    }
    public void visit(Add n) {
        Reg rd = where.get(n.lhs);
        if (where.get(n.lhs) != null) {
            if ( rd == Reg.STACK ) {
                code.add(new Inst(Reg.s10, rs1, rs2, Inst.Arith.add));
                code.add(new Inst(mangle(n.lhs), Reg.s10));
            } else {
                code.add(new Inst(where.get(n.lhs), rs1, rs2, Inst.Arith.add));
            }
        }
    }
    public void visit(Alloc n) {
        Reg rd = where.get(n.lhs);
        if (where.get(n.lhs) != null) {
            if ( rd == Reg.STACK ) {
                code.add(new Inst(Reg.s10, rs1, false));
                code.add(new Inst(mangle(n.lhs), Reg.s10));
            } else {
                code.add(new Inst(where.get(n.lhs), rs1, false));
            }
        }
    }
    HashMap<Identifier, Reg> saved = new HashMap<>() ;
    LivenessAnalysis.IDComparator comp = new LivenessAnalysis.IDComparator();
    private void saveAllExcept(Identifier ignore) {
        saved.clear();
        for ( Map.Entry<Identifier, Reg> e : where.entrySet() ) {
            if ( tempset.contains(e.getValue()) && !comp.equals(e.getKey(), ignore) ) {
                saved.put(e.getKey(), e.getValue());
                if ( saveset.containsAll(where.values()) ) {
                    code.add(new Inst(mangle(e.getKey()), e.getValue()));
                    e.setValue(Reg.STACK);
                } else {
                    for ( Reg s : saveset ) {
                        if ( !where.containsValue(s) && s != rs1 ) {
                            usedSaveRegister.add(s);
                            code.add(new Inst(s, e.getValue(), true));
                            e.setValue(s);
                            break;
                        }
                    }
                }
            }
        }
    }
    private void restore () {
        for ( Map.Entry<Identifier, Reg> e : saved.entrySet() ) {
            Identifier id = e.getKey();
            Reg save = where.remove(id);
            Reg old = e.getValue();
            if ( old == Reg.STACK && save != Reg.STACK ) {
                code.add(new Inst(mangle(id), save));
            } else if ( old != Reg.STACK && save != Reg.STACK ) {
                code.add(new Inst(old, save, true));
            } else if (old != Reg.STACK) {
                code.add(new Inst(old, mangle(id)));
            }
            where.put(id, old);
        }
    }
    public void visit(Call n) {
        Reg rd = where.get(n.lhs);
        if (where.get(n.lhs) != null) {
            saveAllExcept(n.lhs);
            if ( rd == Reg.STACK ) {
                code.add(new Inst(Reg.s10, rs1, callArgs));
                code.add(new Inst(mangle(n.lhs), Reg.s10));
            } else {
                code.add(new Inst(where.get(n.lhs), rs1, callArgs));
            }
            restore();
        }
    }
    public void visit(ErrorMessage n) {
        code.add(new Inst(n.msg));
    }
    public void visit(Goto n) {
        code.add(new Inst(n.label, true));
    }
    public void visit(IfGoto n) {
        code.add(new Inst(rs1, n.label));
    }
    public void visit(LabelInstr n) {
        code.add(new Inst(n.label, false));
    }
    public void visit(LessThan n) {
        Reg rd = where.get(n.lhs);
        if (where.get(n.lhs) != null) {
            if ( rd == Reg.STACK ) {
                code.add(new Inst(Reg.s10, rs1, rs2, Inst.Arith.les));
                code.add(new Inst(mangle(n.lhs), Reg.s10));
            } else {
                code.add(new Inst(where.get(n.lhs), rs1, rs2, Inst.Arith.les));
            }
        }
    }
    public void visit(Load n) {
        Reg rd = where.get(n.lhs);
        if (where.get(n.lhs) != null) {
            if ( rd == Reg.STACK ) {
                code.add(new Inst(Reg.s10, rs1, n.offset));
                code.add(new Inst(mangle(n.lhs), Reg.s10));
            } else {
                code.add(new Inst(where.get(n.lhs), rs1, n.offset));
            }
        }
    }
    public void visit(Move_Id_FuncName n) {
        Reg rd = where.get(n.lhs);
        if (where.get(n.lhs) != null) {
            if ( rd == Reg.STACK ) {
                code.add(new Inst(Reg.s10, n.rhs));
                code.add(new Inst(mangle(n.lhs), Reg.s10));
            } else {
                code.add(new Inst(where.get(n.lhs), n.rhs));
            }
        }
    }
    public void visit(Move_Id_Id n) {
        Reg rd = where.get(n.lhs);
        if (where.get(n.lhs) != null) {
            if ( rd == Reg.STACK ) {
                code.add(new Inst(Reg.s10, rs1, true));
                code.add(new Inst(mangle(n.lhs), Reg.s10));
            } else {
                code.add(new Inst(where.get(n.lhs), rs1, true));
            }
        }
    }
    public void visit(Move_Id_Integer n) {
        Reg rd = where.get(n.lhs);
        if (where.get(n.lhs) != null) {
            if ( rd == Reg.STACK ) {
                code.add(new Inst(Reg.s10, n.rhs));
                code.add(new Inst(mangle(n.lhs), Reg.s10));
            } else {
                code.add(new Inst(where.get(n.lhs), n.rhs));
            }
        }
    }
    public void visit(Multiply n) {
        Reg rd = where.get(n.lhs);
        if (where.get(n.lhs) != null) {
            if ( rd == Reg.STACK ) {
                code.add(new Inst(Reg.s10, rs1, rs2, Inst.Arith.mul));
                code.add(new Inst(mangle(n.lhs), Reg.s10));
            } else {
                code.add(new Inst(where.get(n.lhs), rs1, rs2, Inst.Arith.mul));
            }
        }
    }
    public void visit(Print n) {
        code.add(new Inst(rs1));
    }
    public void visit(Store n) {
        code.add(new Inst(rs1, n.offset, rs2));
    }
    public void visit(Subtract n) {
        Reg rd = where.get(n.lhs);
        if (where.get(n.lhs) != null) {
            if ( rd == Reg.STACK ) {
                code.add(new Inst(Reg.s10, rs1, rs2, Inst.Arith.sub));
                code.add(new Inst(mangle(n.lhs), Reg.s10));
            } else {
                code.add(new Inst(where.get(n.lhs), rs1, rs2, Inst.Arith.sub));
            }
        }
    }

}
