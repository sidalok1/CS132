package hw4utils;

import java.util.*;

public class RegisterFile {
    EnumMap<Reg, String> regfile = new EnumMap<>(Reg.class);
    Set<String> stack = new HashSet<>();
    int stored = 0, temps = 0, saved = 0, args = 0;
    final int size, temp, arg, save;
    Set<Reg> tempset, argset, saveset;
    public ArrayList<Reg> reservable;
    public RegisterFile(List<String> arglist, int spill) {
        setArgs(arglist);
        argset = Reg.argset;
        saveset = Reg.saveset;
        tempset = Reg.tempset;
        reservable = new ArrayList<>(Reg.reserve);
        if (spill >= 2) {
            Reg r2 = reservable.get(1);
            tempset.remove(r2);
            Reg r1 = reservable.get(0);
            tempset.remove(r1);
        } else if (spill == 1) {
            Reg r1 = reservable.get(0);
            tempset.remove(r1);
        }

        temp = tempset.size();
        arg = argset.size();
        save = saveset.size();
//        size = temp + arg + save;
        size = temp + save;
    }

    public Reg store(String id) {
        if (stored < size) {
            stored++;
            Set<Reg> from;
            if (temps < temp) {
                temps++;
                from = tempset;
            }
//            else if (args < arg) {
//                args++;
//                from = argset;
//            }
            else {
                saved++;
                from = saveset;
            }
            for (Reg r : from) {
                if (!regfile.containsKey(r)) {
                    regfile.put(r, id);
                    return r;
                }
            }
            throw new RuntimeException("Reg file full but stored !>= Reg.size");
        } else {
            stack.add(id);
            return Reg.STACK;
        }
    }

    public void store(String id, Reg r) {
        if (r.equals(Reg.STACK)) {
            stack.add(id);
        } else {
            regfile.put(r, id);
        }
    }

    public Reg storeArg(String id) {
        if (args < arg) {
            args++;
            for ( Reg r : argset) {
                if (!regfile.containsKey(r)) {
                    regfile.put(r, id);
                    return r;
                }
            }
        } else {
            stack.add(id);
            return Reg.STACK;
        }
        return null;
    }

    public Reg find(String id) {
        if (regfile.containsValue(id)) {
            ArrayList<String> regs = new ArrayList<>(regfile.values());
            int index = regs.indexOf(id);
            return Reg.values()[index];
        } else if (stack.contains(id)) {
            return Reg.STACK;
        } else {
            return null;
        }
    }
    public boolean containsReg(String id) { return regfile.containsValue(id); }
    public boolean containsStack(String id) { return stack.contains(id); }
    public boolean contains(String id) { return containsReg(id) || containsStack(id); }

    public boolean isTemp(Reg r) { return tempset.contains(r); }

    public boolean tempsAvailable() { return temps > 0; }
    public boolean argsAvailable() { return args > 0; }
    public boolean storedAvailable() { return stored > 0; }

    public Reg remove(String id) {
        Reg r = find(id);
        if (r.equals(Reg.STACK)) {
            stack.remove(id);
        } else if (regfile.containsKey(r)) {
            regfile.remove(r);
            stored--;
            if (Reg.tempset.contains(r)) { temps--; }
            else if (Reg.argset.contains(r)) { args--; }
            else { stored--; }
        }
        return r;
    }

    public void push(Reg r) {
        String id = regfile.get(r);
        regfile.remove(r);
        stack.add(id);
    }
    public void pop(String id, Reg r) {
        stack.remove(id);
        regfile.put(r, id);
    }
    public void load(String id, Reg r) {
        assert (stack.contains(id));
        regfile.put(r, id);
    }

    public void setArgs(List<String> arglist) {
        Queue<String> argqueue = new ArrayDeque<>(arglist);
        for (Reg r : Reg.argset) {
            if (!argqueue.isEmpty()) {
                String arg = argqueue.remove();
                regfile.put(r, arg);
                stored++;
                args++;
            }
        }
        while (!argqueue.isEmpty()) {
            String arg = argqueue.remove();
            stack.add(arg);
        }
    }

}
