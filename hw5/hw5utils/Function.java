package hw5utils;

import java.util.*;

public class Function {
    public final String name;
    public final List<RV> instructions = new ArrayList<>();
    public ArrayList<String> locals = new ArrayList<>();
    public ArrayList<String> params = new ArrayList<>();
    public boolean globl = false;
    public boolean start = false;
    public Function(String name) {
        this.name = name;
    }
    public Function(String name, List<RV> instructions) {
        this.name = name;
        globl = true;
        this.instructions.addAll(instructions);
    }
    public void add(RV instruction) {
        instructions.add(instruction);
    }
    public int indexOf(String name) {
        if (locals.contains(name)) {
            return locals.indexOf(name);
        } else {
            return params.indexOf(name);
        }
    }

    @Override
    public String toString() {
        ArrayList<RV> all = new ArrayList<>((instructions.size()) + 9);
        all.add(RV.LABEL(this.name));
        all.addAll(this.prologue());
        all.addAll(instructions);
        all.addAll(this.epilogue());
        StringBuilder str = new StringBuilder(all.size()*3);
        if (globl) {
            str.append(".globl ");
            str.append(this.name);
            str.append("\n");
        }
        for (RV instruction : all) {
            if (!instruction.type.equals(Type.LABEL)) {
                str.append("\t");
            }
            str.append(instruction);
            str.append("\n");
        }
        return str.toString();
    }

    public String ret = "";
    private ArrayList<RV> prologue() {
        ArrayList<RV> prologue = new ArrayList<>();
        if (!globl) {
            if (!start) {
                prologue.add(RV.SW(Reg.ra, -4, Reg.sp));
                prologue.add(RV.SW(Reg.fp, -8, Reg.sp));
                prologue.add(RV.ADDI(Reg.fp, Reg.sp, 0));
                if (!locals.isEmpty()) {
                    int numberOfLocals = locals.size();
                    prologue.add(RV.ADDI(Reg.sp, Reg.sp, (numberOfLocals+2) * -4));
                }
            } else {
                if (!locals.isEmpty()) {
                    int numberOfLocals = locals.size();
                    prologue.add(RV.ADDI(Reg.sp, Reg.sp, (numberOfLocals) * -4));
                }
            }

        }
        return prologue;
    }
    private ArrayList<RV> epilogue() {
        ArrayList<RV> epilogue = new ArrayList<>();
        int retnum = locals.indexOf(ret);
        if (!globl) {
            if (!start) {
                epilogue.add(RV.LW(Reg.a0, Reg.sp, retnum * 4));
                epilogue.add(RV.LW(Reg.ra, Reg.fp, -4));
                epilogue.add(RV.LW(Reg.fp, Reg.fp, -8));
                if (!locals.isEmpty()) {
                    int numberOfLocals = locals.size();
                    epilogue.add(RV.ADDI(Reg.sp, Reg.sp, (numberOfLocals+2) * 4));
                }
            } else {
                if (!locals.isEmpty()) {
                    int numberOfLocals = locals.size();
                    epilogue.add(RV.ADDI(Reg.sp, Reg.sp, (numberOfLocals) * 4));
                }
            }

        }
        if (!start) {
            epilogue.add(RV.RET());
        } else {
            epilogue.add(RV.LI(Reg.a0, "@exit"));
            epilogue.add(RV.ECALL());
        }
        return epilogue;
    }

}
