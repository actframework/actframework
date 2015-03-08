package org.osgl.oms.util;

import org.osgl.oms.asm.ClassVisitor;
import org.osgl.oms.asm.Type;

public abstract class PredictableBytecodeVisitor extends BytecodeVisitor {
    protected PredictableBytecodeVisitor(ClassVisitor cv) {
        super(cv);
    }

    protected PredictableBytecodeVisitor() {
        super();
    }

    public abstract boolean isTargetClass(String className);

    protected static boolean isConstructor(String methodName) {
        return methodName.contains("<init>");
    }

    protected static boolean isPublic(int access) {
        return (ACC_PUBLIC & access) > 0;
    }

    protected static boolean isAbstract(int access) {
        return (ACC_ABSTRACT & access) > 0;
    }

}
