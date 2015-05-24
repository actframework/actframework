package playground;

import act.asm.MethodVisitor;
import act.asm.Opcodes;

public class InstructionBuffer implements Opcodes {
    private MethodVisitor mv;
    public InstructionBuffer(MethodVisitor mv) {
        this.mv = mv;
    }
    public void flush() {
    }
}
