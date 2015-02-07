package org.osgl.oms.util;

import org.osgl._;
import org.osgl.oms.asm.ClassVisitor;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.asm.Opcodes;
import org.osgl.util.E;

/**
 * Base class for all bytecode visitor, either detector or enhancer
 */
public class BytecodeVisitor extends ClassVisitor implements Opcodes {
    protected BytecodeVisitor(ClassVisitor cv) {
        super(ASM5, cv);
    }

    protected BytecodeVisitor() {
        super(ASM5);
    }

    private BytecodeVisitor setDownstream(ClassVisitor cv) {
        E.illegalStateIf(null != this.cv);
        this.cv = cv;
        return this;
    }

    public static ClassVisitor chain(ClassWriter cw, BytecodeVisitor v0, BytecodeVisitor ... visitors) {
        v0.setDownstream(cw);
        int len = visitors.length;
        if (0 == len) {
            return v0;
        } else {
            visitors[len - 1].setDownstream(v0);
        }
        for (int i = len - 2; i >= 0; --i) {
            visitors[i].setDownstream(visitors[i + 1]);
        }
        return visitors[0];
    }

    public static ClassVisitor chain(ClassWriter cw, Class<? extends BytecodeVisitor> c0, Class<? extends BytecodeVisitor> ... cs) {
        BytecodeVisitor v0 = _.newInstance(c0);
        v0.setDownstream(cw);
        int len = cs.length;
        if (0 == len) {
            return v0;
        }
        BytecodeVisitor vi = _.newInstance(cs[len - 1]);
        vi.setDownstream(v0);
        for (int i = len - 2; i >= 0; --i) {
            BytecodeVisitor tmp = _.newInstance(cs[i]);
            tmp.setDownstream(vi);
            vi = tmp;
        }
        return vi;
    }
}
