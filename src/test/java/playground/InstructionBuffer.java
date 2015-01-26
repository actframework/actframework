package playground;

import org.osgl.mvc.server.asm.MethodVisitor;
import org.osgl.mvc.server.asm.Opcodes;

public class InstructionBuffer implements Opcodes {
    private MethodVisitor mv;
    public InstructionBuffer(MethodVisitor mv) {
        this.mv = mv;
    }
    public void flush() {
    }
}
