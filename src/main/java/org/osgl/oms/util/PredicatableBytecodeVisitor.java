package org.osgl.oms.util;

import org.osgl.oms.asm.ClassVisitor;

public abstract class PredicatableBytecodeVisitor extends BytecodeVisitor {
    protected PredicatableBytecodeVisitor(ClassVisitor cv) {
        super(cv);
    }

    protected PredicatableBytecodeVisitor() {
        super();
    }

    public abstract boolean isTargetClass(String className);
}
