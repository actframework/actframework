package org.osgl.oms.util;

import org.osgl._;
import org.osgl.oms.asm.ClassVisitor;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.asm.Opcodes;
import org.osgl.util.E;

import java.util.Iterator;
import java.util.List;

/**
 * Base class for all bytecode visitor, either detector or enhancer
 */
public class BytecodeVisitor extends ClassVisitor implements Opcodes {

    private _.Var<? extends ClassVisitor> _cv;

    protected BytecodeVisitor(ClassVisitor cv) {
        super(ASM5, cv);
    }

    protected BytecodeVisitor() {
        super(ASM5);
    }

    public BytecodeVisitor commitDownstream() {
        E.illegalStateIf(null == _cv || null != cv);
        cv = _cv.get();
        return this;
    }

    private BytecodeVisitor setDownstream(_.Var<? extends ClassVisitor> cv) {
        E.illegalStateIf(null != this.cv);
        _cv = cv;
        return this;
    }

    private BytecodeVisitor setDownstream(ClassVisitor cv) {
        E.illegalStateIf(null != this.cv);
        this.cv = cv;
        return this;
    }

    public static BytecodeVisitor chain(_.Var<ClassWriter> cw, List<? extends BytecodeVisitor> visitors) {
        if (visitors.isEmpty()) {
            return null;
        }
        Iterator<? extends BytecodeVisitor> i = visitors.iterator();
        BytecodeVisitor v = i.next();
        v.setDownstream(cw);
        while (i.hasNext()) {
            BytecodeVisitor v0 = i.next();
            v0.setDownstream(v);
            v = v0;
        }
        return v;
    }

    public static ClassVisitor chain(ClassWriter cw, BytecodeVisitor v0, BytecodeVisitor... visitors) {
        v0.setDownstream(cw);
        int len = visitors.length;
        if (0 == len) {
            return v0;
        }
        for (int i = 0; i < len - 1; ++i) {
            BytecodeVisitor v = visitors[i];
            v.setDownstream(v0);
            v0 = v;
        }
        return v0;
    }

}
