package playground;

import org.osgl.oms.asm.MethodVisitor;
import org.osgl.oms.asm.Opcodes;

public class InstructionBuffer implements Opcodes {
    private MethodVisitor mv;
    public InstructionBuffer(MethodVisitor mv) {
        this.mv = mv;
    }
    public void flush() {
    }
}
