package hw4utils;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.EnumSet;
public enum Reg {
    a2, a3, a4, a5, a6, a7,
    s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11,
    t0, t1, t2, t3, t4, t5,
    STACK;

    public static final EnumSet<Reg>
//        tempset = new TreeSet<>(Arrays.asList(new Reg[]{t0, t1, t2, t3, t4, t5})),
//        argset =  new TreeSet<>(Arrays.asList(new Reg[]{a2, a3, a4, a5, a6, a7})),
//        saveset = new TreeSet<>(Arrays.asList(new Reg[]{s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11})),
//        reserve = new TreeSet<>(Arrays.asList(new Reg[]{t0, t1}));
        tempset =   EnumSet.of(t0, t1, t2, t3, t4, t5),
        argset =    EnumSet.of(a2, a3, a4, a5, a6, a7),
        saveset =   EnumSet.of(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11),
        reserve =   EnumSet.of(t0, t1);

    public static final int args = argset.size(), saved = saveset.size(), temp = tempset.size(), res = reserve.size();
    public static final int size = args + saved + temp + res;

    public static Reg arg(int i) {
        ArrayList<Reg> argarray = new ArrayList<>(argset);
        return (i >= 0 && i < 6) ? argarray.get(i) : STACK;
    }
    public static Reg save(int i) {
        ArrayList<Reg> savearray = new ArrayList<>(saveset);
        return savearray.get(i);
    }
    public static Reg temp(int i) {
        ArrayList<Reg> temparray = new ArrayList<>(tempset);
        return temparray.get(i);
    }
    public static Reg reserve(int i) {
        ArrayList<Reg> reservearray = new ArrayList<>(reserve);
        return reservearray.get(i);
    }
}
