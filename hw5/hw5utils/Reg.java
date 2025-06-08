package hw5utils;

public enum Reg {
    zero,
    ra, sp, fp,
    t0, t1, t2, t3, t4, t5, t6,
    a0, a1, a2, a3, a4, a5, a6, a7,
    s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11;

    private static boolean debug = true;

    public static Reg reg(IR.token.Register r) {
        switch (r.toString()) {
            case "ra": return ra;
            case "sp": return sp;
            case "fp": return fp;
            case "t0": return t0;
            case "t1": return t1;
            case "t2": return t2;
            case "t3": return t3;
            case "t4": return t4;
            case "t5": return t5;
            case "t6": return t6;
            case "s1": return s1;
            case "s2": return s2;
            case "s3": return s3;
            case "s4": return s4;
            case "s5": return s5;
            case "s6": return s6;
            case "s7": return s7;
            case "s8": return s8;
            case "s9": return s9;
            case "s10": return s10;
            case "s11": return s11;
            case "a0": return a0;
            case "a1": return a1;
            case "a2": return a2;
            case "a3": return a3;
            case "a4": return a4;
            case "a5": return a5;
            case "a6": return a6;
            case "a7": return a7;
            default: return zero;
        }
    }

    @Override
    public String toString() {
        if (debug) {
            if (this.equals(zero)) return "x0";
            return this.name();
        }
        switch (this) {
            case zero:
                return "x0";
            case ra:
                return "x1";
            case sp:
                return "x2";
            case fp:
                return "x8";
            case t0:
                return "x5";
            case t1:
                return "x6";
            case t2:
                return "x7";
            case t3:
                return "x28";
            case t4:
                return "x29";
            case t5:
                return "x30";
            case t6:
                return "x31";
            case a0:
                return "x10";
            case a1:
                return "x11";
            case a2:
                return "x12";
            case a3:
                return "x13";
            case a4:
                return "x14";
            case a5:
                return "x15";
            case a6:
                return "x16";
            case a7:
                return "x17";
            case s1:
                return "x9";
            case s2:
                return "x18";
            case s3:
                return "x19";
            case s4:
                return "x20";
            case s5:
                return "x21";
            case s6:
                return "x22";
            case s7:
                return "x23";
            case s8:
                return "x24";
            case s9:
                return "x25";
            case s10:
                return "x26";
            case s11:
                return "x27";
            default:
                return null;
        }
    }
}
