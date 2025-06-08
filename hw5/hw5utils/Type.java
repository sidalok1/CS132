package hw5utils;

public enum Type {
    ADD, SUB, MUL, SLT,
    ADDI,
    LI, LW, SW, LA,
    ECALL, JALR, BNEZ, RET, CALL, JAL,
    LABEL;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
