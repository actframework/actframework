package org.osgl.oms.util;

import org.osgl._;
import org.osgl.oms.OMS;
import org.osgl.oms.asm.ClassVisitor;
import org.osgl.oms.asm.Opcodes;
import org.osgl.oms.plugin.Plugin;
import org.osgl.util.E;

/**
 * Base class for all bytecode enhancer that using ASM lib
 */
public abstract class AsmByteCodeEnhancer<T extends AsmByteCodeEnhancer> extends PredictableByteCodeVisitor
        implements Opcodes, Plugin, Cloneable {
    private _.Predicate<String> targetClassPredicate;

    protected abstract Class<T> subClass();

    protected AsmByteCodeEnhancer(_.Predicate<String> targetClassPredicate) {
        this.targetClassPredicate = targetClassPredicate;
    }

    protected AsmByteCodeEnhancer(_.Predicate<String> targetClassPredicate, ClassVisitor cv) {
        super(cv);
        this.targetClassPredicate = targetClassPredicate;
    }

    public void predicate(_.Predicate<String> predicate) {
        targetClassPredicate = predicate;
    }

    @Override
    public boolean isTargetClass(String className) {
        return (null == targetClassPredicate) || targetClassPredicate.test(className);
    }

    @Override
    public void register() {
        OMS.enhancerManager().register(this);
    }

    public T clone() {
        try {
            return (T) super.clone();
        } catch (CloneNotSupportedException e) {
            throw E.unexpected(e);
        }
    }
}
