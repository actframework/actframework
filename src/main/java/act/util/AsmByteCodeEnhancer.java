package act.util;

import act.Act;
import act.asm.ClassVisitor;
import act.asm.Opcodes;
import act.plugin.Plugin;
import org.osgl._;
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
        Act.enhancerManager().register(this);
    }

    public T clone() {
        try {
            return (T) super.clone();
        } catch (CloneNotSupportedException e) {
            throw E.unexpected(e);
        }
    }
}
