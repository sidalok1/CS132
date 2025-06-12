package hw4utils;

import IR.token.FunctionName;
import IR.token.Identifier;
import IR.token.*;
import sparrow.*;
import sparrow.visitor.*;

import java.util.*;

public class LivenessAnalysis implements Visitor {
    public LivenessAnalysis(FunctionDecl f) {
        f.accept(this);
    }
    int size;
    ArrayList<Identifier> def;
    ArrayList<ArrayList<Identifier>> use;
    ArrayList<TreeSet<Identifier>> in;
    ArrayList<TreeSet<Identifier>> out;
    ArrayList<TreeSet<Identifier>> in_ = null;
    ArrayList<TreeSet<Identifier>> out_ = null;
    ArrayList<ArrayList<Integer>> succ;
    public TreeSet<Interval> intervals; // closest thing java collections has to sorted list
    public void visit(Program p) {
        // to be used at the level of function declarations
    }
    ArrayList<Identifier> args;
    FunctionName funcname;
    Block block;
    public void visit(FunctionDecl n) {
        this.funcname = n.functionName;
        this.args = new ArrayList<>();
        this.args.addAll(n.formalParameters);
        this.block = n.block;
        this.block.accept(this);
    }
    Identifier ret;
    ArrayList<Instruction> code;
    public void visit(Block n) {
        ret = n.return_id;
        this.size = n.instructions.size();
        this.code = new ArrayList<>(n.instructions);
        this.init();
        while ( !this.stop() ) this.iter();
        this.LinearScanIntervals();
    }
    public TreeSet<Identifier> out(int n) {
        return this.out.get(n);
    }
//    public LivenessAnalysis(FunctionDecl f) {
//        this.size = f.block.instructions.size();
//        this.init();
//        f.block.accept(this);
//        this.in.get(this.in.size()-1).add(this.ret);
//        while ( !this.stop() ) this.iter(); // liveness analysis happens here
//        all = new HashSet<>();
//        for (int i = 0; i < this.size; i++) {
//            all.addAll(union(in.get(i), out.get(i)));
//        }
//        intervals = new TreeSet<>(new IntervalComparator(IntervalComparator.Type.START));
//        intervalMap = new HashMap<>();
//        for (String s : all) {
//            boolean live;
//            int start;
//            boolean[] lifetime = new boolean[this.size+2];
//            intervalMap.put(s, lifetime);
//            start = 0;
//            live = in.get(0).contains(s);
//            lifetime[0] = live;
//            int i;
//            for (i = 0; i < this.size; i++) {
//                if (out.get(i).contains(s)) {
//                    if (!live) {
//                        live = true;
//                        start = i+1;
//                    }
//                    lifetime[i+1] = true;
//                } else {
//                    if (live) {  // live-in and out does not contain s, end of interval
//                        live = false;
//                        intervals.add(new Interval(start, i+1, s));
//                    }
//                    lifetime[i+1] = false;
//                }
//            }
//            lifetime[i + 1] = live;
//            if (live) { intervals.add(new Interval(start, i+2, s)); }
//        }
//        // maybe check here for how many saved regs needed in call
//    }
    IntervalComparator byStart = new IntervalComparator(IntervalComparator.Type.START);
    private int max(ArrayList<Integer> lst) {
        int max = lst.get(0);
        lst.forEach(i -> i = Math.max(i, max));
        return max;
    }
    private void LinearScanIntervals() {
        Set<Identifier> allIDs = init_intervals();
        for (Identifier id : allIDs) {
            int start = 0;
            int end = this.size;
            for ( int i = 0; i < this.size; i++ ) {
                if ( comp.containedIn(id, this.out.get(i)) ) {
                    end = i;
                }
            }
            for ( int i = 0; i < this.size; i++ ) {
                if ( comp.containedIn(id, this.out.get(i)) ) {
                    start = i;
                    break;
                }
            }
            if ( comp.equals(this.ret, id) ) { end = this.size; }
            intervals.add(new Interval(start, end+1, id));
        }
    }
    private void generateIntervals() {
        Set<Identifier> allIDs = init_intervals();
        boolean live;
        int start;
        for ( Identifier id : allIDs ) {
            live = false;
            start = 0;
            int i;
            for ( i = 0; i < this.size; i++ ) {
                if ( this.out.get(i).contains(id) && !live ) {
                    live = true;
                    start = i;
                } else if ( !this.out.get(i).contains(id) && live ) {
                    live = false;
                    intervals.add(new Interval(start, i, id));
                }
            }
            if ( live ) {
                if ( id == this.ret ) {
                    i++;
                }
                intervals.add(new Interval(start, i, id));
            }
        }
    }

    private Set<Identifier> init_intervals() {
        intervals = new TreeSet<>(byStart);
        Set<Identifier> allIDs = new TreeSet<>(comp);
        for (int i = 0; i < this.size; i++) {
            allIDs.addAll(this.in.get(i));
            allIDs.addAll(this.out.get(i));
        }
        return allIDs;
    }

    private void init() {
        use = new ArrayList<>(this.size);
        def = new ArrayList<>(this.size);
        in = new ArrayList<>(this.size);
        out = new ArrayList<>(this.size);
        succ = new ArrayList<>(this.size);
        in_ = null;
        out_ = null;
        for (int i = 0; i < this.size; i++) {
            in.add(i, new TreeSet<>(comp));
            out.add(i, new TreeSet<>(comp));
            succ.add(i, new ArrayList<>());
            use.add(i, new ArrayList<>());
            def.add(i, null);
        }
        this.code.forEach(i -> i.accept(this));
        this.mapLocs();
    }

    private void mapLocs() {
        for (Label l : labelLocs.keySet()) {
            int loc = labelLocs.get(l);
            if ( this.gotos.containsKey(l) ) {
                for ( Goto g : this.gotos.get(l) ) {
                    int idx = this.code.indexOf(g);
                    this.succ.get(idx).add(loc);
                }
            }
            if ( this.ifGotos.containsKey(l) ) {
                for ( IfGoto ig : this.ifGotos.get(l) ) {
                    int idx = this.code.indexOf(ig);
                    this.succ.get(idx).add(loc);
                }
            }
        }
    }

    IDComparator comp = new IDComparator();
    public static class IDComparator implements Comparator<Identifier> {
        public int compare(Identifier identifier, Identifier t1) {
            return identifier.toString().compareTo(t1.toString());
        }
        public boolean equals(Identifier t1, Identifier t2) {
            if ( t1 == null || t2 == null ) { return false; }
            return t1.toString().equals(t2.toString());
        }
        public boolean containedIn(Identifier t1, Collection<Identifier> t2) {
            for ( Identifier i : t2 ) {
                if ( this.equals(i, t1) ) { return true; }
            }
            return false;
        }
    }
    public TreeSet<Identifier> in(int n) {
        if ( n < this.in.size() ) {
            return this.in.get(n);
        } else {
            TreeSet<Identifier> ret = new TreeSet<>(new IDComparator());
            ret.add(this.ret);
            return ret;
        }
    }
    private void iter() {
        this.in_ = deepCopy(this.in);
        this.out_ = deepCopy(this.out);
        for (int i = this.code.size() - 1; i >= 0; i--) {
            TreeSet<Identifier> successors_in = new TreeSet<>(comp);
            ArrayList<Integer> successors = this.succ.get(i);
            for (int j : successors) {
                successors_in = this.union(successors_in, this.in(j));
            }
            this.out.set(i, this.union(this.out.get(i), successors_in));
            TreeSet<Identifier> outs = new TreeSet<>(this.out.get(i));
            Identifier define = this.def.get(i);
            if ( define != null ) {
                outs.remove(define);
            }
            TreeSet<Identifier> use = new TreeSet<>(comp); use.addAll(this.use.get(i));
            this.in.set(i, this.union(use, outs));
        }
    }
    private TreeSet<Identifier> union(TreeSet<Identifier> s1, TreeSet<Identifier> s2) {
        TreeSet<Identifier> union = new TreeSet<>(comp);
        union.addAll(s1);
        union.addAll(s2);
        return union;
    }
    private ArrayList<TreeSet<Identifier>> deepCopy(ArrayList<TreeSet<Identifier>> a) {
        ArrayList<TreeSet<Identifier>> b = new ArrayList<>(a.size());
        for (TreeSet<Identifier> s : a) {
            b.add(new TreeSet<>(s));
        }
        return b;
    }

    private boolean stop() {
        return in.equals(in_) && out.equals(out_);
    }

    public void visit(Add n) {
        int idx = this.code.indexOf(n);
        this.def.set(idx, n.lhs);
        this.use.get(idx).addAll(Arrays.asList(n.arg1, n.arg2));
        this.succ.get(idx).add(idx+1);
    }
    public void visit(Alloc n) {
        int idx = this.code.indexOf(n);
        this.def.set(idx, n.lhs);
        this.use.get(idx).add(n.size);
        this.succ.get(idx).add(idx+1);
    }
    public void visit(Call n) {
        int idx = this.code.indexOf(n);
        this.def.set(idx, n.lhs);
        ArrayList<Identifier> u = this.use.get(idx);
        u.add(n.callee);
        u.addAll(n.args);
        this.succ.get(idx).add(idx+1);
    }
    public void visit(ErrorMessage n) {
        int idx = this.code.indexOf(n);
        this.succ.get(idx).add(idx+1);
    }
    private static class LabelComparator implements Comparator<Label> {
        public int compare(Label o1, Label o2) {
            return o1.toString().compareTo(o2.toString());
        }
    }
    LabelComparator labelcomp = new LabelComparator();
    TreeMap<Label,ArrayList<Goto>> gotos = new TreeMap<>(labelcomp);
    public void visit(Goto n) {
        //nothind used or defined
        this.gotos.computeIfAbsent(n.label, k -> new ArrayList<>()).add(n);
    }
    TreeMap<Label, ArrayList<IfGoto>> ifGotos = new TreeMap<>(labelcomp);

    public void visit(IfGoto n) {
        int idx = this.code.indexOf(n);
        this.use.get(idx).add(n.condition);
        this.ifGotos.computeIfAbsent(n.label, k -> new ArrayList<>()).add(n);
        this.succ.get(idx).add(idx+1);
    }
    TreeMap<Label, Integer> labelLocs = new TreeMap<>(labelcomp);
    public void visit(LabelInstr n) {
        int idx = this.code.indexOf(n);
        this.labelLocs.put(n.label, idx);
        this.succ.get(idx).add(idx+1);
    }
    public void visit(LessThan n) {
        int idx = this.code.indexOf(n);
        this.def.set(idx, n.lhs);
        this.use.get(idx).addAll(Arrays.asList(n.arg1, n.arg2));
        this.succ.get(idx).add(idx+1);
    }
    public void visit(Load n) {
        int idx = this.code.indexOf(n);
        this.def.set(idx, n.lhs);
        this.use.get(idx).add(n.base);
        this.succ.get(idx).add(idx+1);
    }
    public void visit(Move_Id_FuncName n) {
        int idx = this.code.indexOf(n);
        this.def.set(idx, n.lhs);
        this.succ.get(idx).add(idx+1);
    }
    public void visit(Move_Id_Id n) {
        int idx = this.code.indexOf(n);
        this.def.set(idx, n.lhs);
        this.use.get(idx).add(n.rhs);
        this.succ.get(idx).add(idx+1);
    }
    public void visit(Move_Id_Integer n) {
        int idx = this.code.indexOf(n);
        this.def.set(idx, n.lhs);
        this.succ.get(idx).add(idx+1);
    }
    public void visit(Multiply n) {
        int idx = this.code.indexOf(n);
        this.def.set(idx, n.lhs);
        this.use.get(idx).addAll(Arrays.asList(n.arg1, n.arg2));
        this.succ.get(idx).add(idx+1);
    }
    public void visit(Print n) {
        int idx = this.code.indexOf(n);
        this.use.get(idx).add(n.content);
        this.succ.get(idx).add(idx+1);
    }
    public void visit(Store n) {
        int idx = this.code.indexOf(n);
        this.use.get(idx).addAll(Arrays.asList(n.base, n.rhs));
        this.succ.get(idx).add(idx+1);
    }
    public void visit(Subtract n) {
        int idx = this.code.indexOf(n);
        this.def.set(idx, n.lhs);
        this.use.get(idx).addAll(Arrays.asList(n.arg1, n.arg2));
        this.succ.get(idx).add(idx+1);
    }


}
