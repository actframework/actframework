package org.osgl.oms.util;

import com.sun.org.apache.xpath.internal.compiler.OpCodes;
import org.osgl._;
import org.osgl.oms.OMS;
import org.osgl.oms.asm.ClassVisitor;
import org.osgl.oms.asm.Opcodes;
import org.osgl.oms.plugin.Plugin;
import org.osgl.oms.util.PredicatableBytecodeVisitor;

/**
 * Base class for all bytecode enhancer that using ASM lib
 */
public abstract class AsmBytecodeEnhancer extends PredicatableBytecodeVisitor
        implements Opcodes, Plugin {
    private _.Predicate<String> targetClassPredicate;

    protected AsmBytecodeEnhancer(_.Predicate<String> targetClassPredicate, ClassVisitor cv) {
        super(cv);
        this.targetClassPredicate = targetClassPredicate;
    }

    @Override
    public boolean isTargetClass(String className) {
        return (null == targetClassPredicate) || targetClassPredicate.test(className);
    }

    @Override
    public void register() {
        OMS.enhancerManager().register(this);
    }
}
