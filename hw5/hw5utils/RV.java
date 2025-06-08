package hw5utils;

public class RV {
    public final Reg rd, rs1, rs2;
    public final String imm, symbol;
    public final Type type;
    private RV(Type type, Reg rd, Reg rs1, Reg rs2, String imm, String symbol) {
        this.type = type; this.rd = rd; this.rs1 = rs1; this.rs2 = rs2; this.imm = imm; this.symbol = symbol;
    }
    private static RV R(Type type, Reg rd, Reg rs1, Reg rs2) {
        return new RV(type, rd, rs1, rs2, null, null);
    }
    private static RV I(Type type, Reg rd, Reg rs1, String imm) {
        return new RV(type, rd, rs1, null, imm, null);
    }
    private static RV S(Reg rs1, Reg rs2, String imm) {
        return new RV(Type.SW, null, rs1, rs2, imm, null);
    }
    public static RV ADD(Reg rd, Reg rs1, Reg rs2) { return R(Type.ADD, rd, rs1, rs2); }
    public static RV SUB(Reg rd, Reg rs1, Reg rs2) { return R(Type.SUB, rd, rs1, rs2); }
    public static RV MUL(Reg rd, Reg rs1, Reg rs2) { return R(Type.MUL, rd, rs1, rs2); }
    public static RV SLT(Reg rd, Reg rs1, Reg rs2) { return R(Type.SLT, rd, rs1, rs2); }
    public static RV LI(Reg rd, String imm) { return I(Type.LI, rd, null, imm); }
    public static RV ADDI(Reg rd, Reg rs1, String imm) { return I(Type.ADDI, rd, rs1, imm); }
    public static RV ADDI(Reg rd, Reg rs1, int imm) { return I(Type.ADDI, rd, rs1, Integer.toString(imm)); }
    public static RV LI(Reg rd, int imm) { return I(Type.LI, rd, null, Integer.toString(imm)); }
    public static RV LW(Reg rd, Reg rs1, String imm) { return I(Type.LW, rd, rs1, imm); }
    public static RV LW(Reg rd, Reg rs1, int imm) { return I(Type.LW, rd, rs1, Integer.toString(imm)); }
    public static RV SW(Reg rs1, String imm, Reg rs2) { return S(rs2, rs1, imm); }
    public static RV SW(Reg rs1, int imm, Reg rs2) { return S(rs1, rs2, Integer.toString(imm)); }
    public static RV LA(Reg rd, String symbol) { return new RV(Type.LA, rd, null, null, null, symbol); }
    public static RV ECALL() { return new RV(Type.ECALL, null, null, null, null, null); }
    public static RV RET() { return new RV(Type.RET, null, null, null, null, null); }
    public static RV CALL(String symbol) { return new RV(Type.CALL, null, null, null, null, symbol); }
    public static RV JALR(Reg rs) { return I(Type.JALR, Reg.ra, rs, null); }
    public static RV JALR(Reg rd, Reg rs) { return I(Type.JALR, rd, rs, null); }
    public static RV JAL(Reg ra, String symbol) { return I(Type.JAL, ra, null, symbol); }
    public static RV BNEZ(Reg rs, String symbol) { return new RV(Type.BNEZ, null, rs, null, null, symbol); }
    public static RV LABEL(String label) { return new RV(Type.LABEL, null, null, null, null, label); }

    @Override
    public String toString() {
        String ret = this.type.toString() + " ", a = "", b = "", c  = "";
        switch (this.type) {
            case ADD: case SUB: case MUL: case SLT:
                a = this.rd.toString() + ", ";
                b = this.rs1.toString() + ", ";
                c = this.rs2.toString();
                break;
            case LW:
                a = this.rd.toString() + ", ";
                b = this.imm + "(";
                c = this.rs1.toString() + ")";
                break;
            case LI:
            case JAL:
                a = this.rd.toString() + ", ";
                b = this.imm;
                break;
            case LA:
                a = this.rd.toString() + ", ";
                b = this.symbol;
                break;
            case SW:
                a = this.rs1.toString() + ", ";
                b = this.imm + "(";
                c = this.rs2.toString() + ")";
                break;
            case JALR:
                a = this.rs1.toString();
                break;
            case BNEZ:
                a = this.rs1.toString() + ", ";
                b = this.symbol;
                break;
            case LABEL:
                ret = this.symbol + ":";
                break;
            case ADDI:
                a = this.rd.toString() + ", ";
                b = this.rs1.toString() + ", ";
                c = this.imm;
                break;
            case CALL:
                a = this.symbol;
                break;
            case ECALL:
            case RET:
            default:
                break;
        }
        return ret + a + b + c;
    }
}
