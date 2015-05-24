package act.util;

import act.asm.ClassVisitor;

public abstract class PredictableByteCodeVisitor extends ByteCodeVisitor {
    protected PredictableByteCodeVisitor(ClassVisitor cv) {
        super(cv);
    }

    protected PredictableByteCodeVisitor() {
        super();
    }

    public abstract boolean isTargetClass(String className);

}
