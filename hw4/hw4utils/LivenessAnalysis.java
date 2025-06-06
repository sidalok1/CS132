package hw4utils;

import IR.syntaxtree.*;
import IR.visitor.DepthFirstVisitor;
import IR.visitor.GJNoArguDepthFirst;

import java.util.*;

public class LivenessAnalysis extends DepthFirstVisitor {
    private static final int LabelWithColon = 0, SetInteger = 1, SetFuncName = 2, Add = 3, Subtract = 4, Multiply = 5,
            LessThan = 6, Load = 7, Store = 8, Move = 9, Alloc = 10, Print = 11, ErrorMessage = 12, Goto = 13,
            IfGoto = 14, Call = 15;

    static class DefVisitor extends GJNoArguDepthFirst<String> {
        public String visit(SetInteger n) {
            return n.f0.accept(this);
        }
        public String visit(SetFuncName n) {
            return n.f0.accept(this);
        }
        public String visit(Add n) {
            return n.f0.accept(this);
        }
        public String visit(Subtract n) {
            return n.f0.accept(this);
        }
        public String visit(Multiply n) {
            return n.f0.accept(this);
        }
        public String visit(LessThan n) {
            return n.f0.accept(this);
        }
        public String visit(Load n) {
            return n.f0.accept(this);
        }
        public String visit(Move n) {
            return n.f0.accept(this);
        }
        public String visit(Alloc n) {
            return n.f0.accept(this);
        }
        public String visit(Call n) {
            return n.f0.accept(this);
        }
        public String visit(Identifier n) {
            return n.f0.tokenImage;
        }
        public String visit(Instruction n) {
            return n.f0.choice.accept(this);
        }
    }

    static class UseVisitor extends GJNoArguDepthFirst<ArrayList<String>> {
        ArrayList<String> lst = new ArrayList<>();
        public ArrayList<String> visit(SetInteger n) { lst = new ArrayList<>(); return null; }
        public ArrayList<String> visit(SetFuncName n) { lst = new ArrayList<>(); return null; }
        public ArrayList<String> visit(ErrorMessage n) { lst = new ArrayList<>(); return null; }
        public ArrayList<String> visit(LabelWithColon n) { lst = new ArrayList<>(); return null; }
        public ArrayList<String> visit(Goto n) { lst = new ArrayList<>(); return null; }
        public ArrayList<String> visit(Add n) {
            lst = new ArrayList<String>(2);
            n.f2.accept(this);
            n.f4.accept(this);
            return lst;
        }
        public ArrayList<String> visit(Subtract n) {
            lst = new ArrayList<String>(2);
            n.f2.accept(this);
            n.f4.accept(this);
            return lst;
        }
        public ArrayList<String> visit(Multiply n) {
            lst = new ArrayList<String>(2);
            n.f2.accept(this);
            n.f4.accept(this);
            return lst;
        }
        public ArrayList<String> visit(LessThan n) {
            lst = new ArrayList<String>(2);
            n.f2.accept(this);
            n.f4.accept(this);
            return lst;
        }
        public ArrayList<String> visit(Load n) {
            lst = new ArrayList<String>(2);
            n.f3.accept(this);
            n.f5.accept(this);
            return lst;
        }
        public ArrayList<String> visit(Store n) {
            lst = new ArrayList<String>(2);
            n.f1.accept(this);
            n.f6.accept(this);
            return lst;
        }
        public ArrayList<String> visit(Move n) {
            lst = new ArrayList<String>(1);
            n.f2.accept(this);
            return lst;
        }
        public ArrayList<String> visit(Alloc n) {
            lst = new ArrayList<String>(1);
            n.f4.accept(this);
            return lst;
        }
        public ArrayList<String> visit(Print n) {
            lst = new ArrayList<String>(1);
            n.f2.accept(this);
            return lst;
        }
        public ArrayList<String> visit(IfGoto n) {
            lst = new ArrayList<>(1);
            n.f1.accept(this);
            return lst;
        }
        public ArrayList<String> visit(If n) {
            lst = new ArrayList<>(1);
            n.f1.accept(this);
            return lst;
        }
        public ArrayList<String> visit(Call n) {
            lst = new ArrayList<>(n.f5.size() + 1);
            n.f3.accept(this);
            for (Node i : n.f5.nodes) {
                i.accept(this);
            }
            return lst;
        }
        public ArrayList<String> visit(Instruction n) {
            n.f0.choice.accept(this);
            return lst;
        }
        public ArrayList<String> visit(Identifier n) {
            lst.add(n.f0.tokenImage);
            return null;
        }
    }
    HashSet<String> all;
    String ret;
    DefVisitor defID = new DefVisitor();
    UseVisitor useIDs = new UseVisitor();
    int size;
    ArrayList<String> def;
    ArrayList<LinkedHashSet<String>> use;
    ArrayList<TreeSet<String>> in;
    ArrayList<TreeSet<String>> out;
    ArrayList<TreeSet<String>> in_ = null;
    ArrayList<TreeSet<String>> out_ = null;
    public LinkedHashSet<String> use(int time) { return use.get(time - 1); }
    ArrayList<int[]> succ;
    ArrayList<Instruction> block;
    TreeSet<Interval> intervals; // closest thing java collections has to sorted list
    public TreeSet<Interval> intervals() { return intervals; }
    HashMap<String, boolean[]> intervalMap;
    public LivenessAnalysis(FunctionDeclaration f) {
        this.size = f.f5.f0.nodes.size() + 1;
        this.init();
        f.f5.accept(this);
        this.in.get(this.in.size()-1).add(this.ret);
        while ( !this.stop() ) this.iter(); // liveness analysis happens here
        all = new HashSet<>();
        for (int i = 0; i < this.size; i++) {
            all.addAll(union(in.get(i), out.get(i)));
        }
        intervals = new TreeSet<>(new IntervalComparator(IntervalComparator.Type.START));
        intervalMap = new HashMap<>();
        for (String s : all) {
            boolean live;
            int start;
            boolean[] lifetime = new boolean[this.size+2];
            intervalMap.put(s, lifetime);
            start = 0;
            live = in.get(0).contains(s);
            lifetime[0] = live;
            int i;
            for (i = 0; i < this.size; i++) {
                if (out.get(i).contains(s)) {
                    if (!live) {
                        live = true;
                        start = i+1;
                    }
                    lifetime[i+1] = true;
                } else {
                    if (live) {  // live-in and out does not contain s, end of interval
                        live = false;
                        intervals.add(new Interval(start, i+1, s));
                    }
                    lifetime[i+1] = false;
                }
            }
            lifetime[i + 1] = live;
            if (live) { intervals.add(new Interval(start, i+2, s)); }
        }
    }

    public int LinearScan(List<String> args) {
        TreeSet<Interval> active = new TreeSet<>(new IntervalComparator(IntervalComparator.Type.LENGTH));
        EnumSet<Reg> regs = EnumSet.noneOf(Reg.class),
                temp_regs = EnumSet.copyOf(Reg.tempset),
                save_regs = EnumSet.copyOf(Reg.saveset);
        int max = 0;
        for ( Interval i : intervals ) {
            TreeSet<Interval> toRemove = new TreeSet<>(new IntervalComparator(IntervalComparator.Type.LENGTH));
            for ( Interval o : active ) {
                if ( o.stop <= i.start ) {
                    regs.remove(o.reg); // free register
                    toRemove.add(o);
                }
            }
            active.removeAll(toRemove);
            if ( args.contains(i.id) ) {
                i.reg = Reg.arg(args.indexOf(i.id));
            }
            else if ( !regs.containsAll(temp_regs) ) {
            for ( Reg r : temp_regs ) {
                if ( !regs.contains(r) ) {
                    regs.add(r);
                    i.reg = r;
                    break;
                }
            }
            active.add(i);
            }
            else if ( !regs.containsAll(save_regs) ) {
            int idx = 0;
            for ( Reg r : save_regs ) {
                if ( !regs.contains(r) ) {
                    max = Math.max(max, idx);
                    regs.add(r);
                    i.reg = r;
                    break;
                }
                idx++;
            }
            active.add(i);
            }
            else {
                i.reg = Reg.STACK; // spill
            }
        }
        for ( Instruction i : block ) {

        }
        Integer maxparams = (new ParamCounter()).count(this.block);
        return Math.min(max + maxparams, Reg.saved);
    }
    private static class ParamCounter extends GJNoArguDepthFirst<Integer> {
        public Integer count(List<Instruction> nodes) {
            int max = 0;
            for ( Instruction i : nodes ) {
                Integer v = i.accept(this);
                v = (v==null) ? 0 : v;
                max = Math.max(max, v);
            }
            return max;
        }
        public Integer visit(NodeListOptional n) {
            int max = 0;
            for ( Node i : n.nodes ) {
                Integer v = i.accept(this);
                v = (v==null) ? 0 : v;
                max = Math.max(max, v);
            }
            return max;
        }
        public Integer visit(Instruction n) {
            return n.f0.choice.accept(this);
        }
        public Integer visit(Call n) { return n.f5.nodes.size(); }
    }

    private void init() {
        use = new ArrayList<LinkedHashSet<String>>(this.size);
        def = new ArrayList<>(this.size);
        in = new ArrayList<TreeSet<String>>(this.size);
        out = new ArrayList<TreeSet<String>>(this.size);
        succ = new ArrayList<>(this.size);
        in_ = null;
        out_ = null;
        for (int i = 0; i < this.size; i++) {
            in.add(new TreeSet<>());
            out.add(new TreeSet<>());
        }
    }

    private void iter() {
        this.in_ = deepCopy(this.in);
        this.out_ = deepCopy(this.out);
        ArrayList<Instruction> rev = new ArrayList<>(this.block); Collections.reverse(rev);
        for ( Instruction n : rev ) {
            Set<String> succ_in = new LinkedHashSet<>();
            Instruction[] successors = this.succ(n);
            for ( Instruction s : successors ) {
                succ_in = this.union(succ_in, this.in(s));
            }
            this.out(n, succ_in);
            TreeSet<String> o = this.out(n), d = this.def(n);
            TreeSet<String> out_not_def = this.difference(o, d);
            this.in(n, this.union(this.use(n), out_not_def));
        }
    }
    private TreeSet<String> union(Set<String> s1, Set<String> s2) {
        TreeSet<String> union = new TreeSet<>();
        union.addAll(s1);
        union.addAll(s2);
        return union;
    }
    private TreeSet<String> difference(TreeSet<String> s1, TreeSet<String> s2) {
        TreeSet<String> diff = new TreeSet<>(s1);
        diff.removeAll(s2);
        return diff;
    }
    private ArrayList<TreeSet<String>> deepCopy(ArrayList<TreeSet<String>> a) {
        ArrayList<TreeSet<String>> b = new ArrayList<>(a.size());
        for (TreeSet<String> s : a) {
            b.add(new TreeSet<>(s));
        }
        return b;
    }

    private boolean stop() {
        return in.equals(in_) && out.equals(out_);
    }

    private Instruction[] succ(Instruction n) {
        int[] arr1 = this.succ.get(this.block.indexOf(n));
        Instruction[] arr2 = new Instruction[arr1.length];
        for ( int i = 0; i < arr1.length; i++ ) {
            int idx = arr1[i];
            arr2[i] = (idx >= this.block.size()) ? null : this.block.get(idx);
        }
        return arr2;
    }

    public LinkedHashSet<String> use(Instruction n) {
        return this.use.get(this.block.indexOf(n));
    }

    public TreeSet<String> def(Instruction n) {
        int idx = this.block.indexOf(n);
        String s = this.def.get(idx);
        TreeSet<String> r = new TreeSet<>();
        if ( s != null ) { r.add(s); }
        return r;
    }

    private void in(Instruction n, Set<String> set) {
        this.in.get(this.block.indexOf(n)).addAll(set);
    }

    private void out(Instruction n, Set<String> set) {
        int idx = this.block.indexOf(n);
        this.out.get(idx).addAll(set);
    }

    private TreeSet<String> in(Instruction n) {
        TreeSet<String> r;
        int idx = this.block.indexOf(n);
        if (idx == -1) { // successor to last instruction (return statement)
            r = new TreeSet<>();
            r.add(this.ret);
        } else {
            r = this.in.get(idx);
        }
        return r;
    }

    private TreeSet<String> out(Instruction n) {
        return this.out.get(this.block.indexOf(n));
    }

    public void visit(Block n) {
        this.ret = n.f2.f0.tokenImage;
        this.block = new ArrayList<>(n.f0.nodes.size());
        for (Node i : n.f0.nodes) {
            i.accept(this);
        }
        for (Instruction i : this.block) {
            succ.add(findSuccessor(i));
        }
    }

    class LabelVisitor extends GJNoArguDepthFirst<String> {
        public String visit(Label n) {
            return n.f0.tokenImage;
        }
        public String visit(LabelWithColon n) {
            return n.f0.accept(this);
        }
        public String visit(Goto n) {
            return n.f1.accept(this);
        }
        public String visit(IfGoto n) {
            return n.f3.accept(this);
        }
        public String visit(Instruction n) {
            return n.f0.choice.accept(this);
        }
    }

    LabelVisitor labelID = new LabelVisitor();

    private int findLabel(String label) {
        for (int i = 0; i < this.block.size(); i++) {
            Instruction inst = this.block.get(i);
            if (inst.f0.which == LabelWithColon && label.equals(labelID.visit(inst))) {
                return i;
            }
        }
        return -1;
    }

    private int[] findSuccessor(Instruction inst) {
        int[] successors;
        if (inst.f0.which == IfGoto) {
            successors = new int[2];
            String l = labelID.visit(inst);
            successors[1] = findLabel(l);
        } else {
            successors = new int[1];
        }
        if (inst.f0.which == Goto) {
            String l = labelID.visit(inst);
            successors[0] = findLabel(l);
        } else {
            successors[0] = this.block.indexOf(inst) + 1;
        }
        return successors;
    }

    public void visit(Instruction n) {
        HashSet<String> set;
        block.add(n);
        def.add(defID.visit(n));
        use.add(new LinkedHashSet<>(useIDs.visit(n)));
    }
}
