package org.osgl.oms.util;

import org.osgl._;
import org.osgl.oms.app.App;
import org.osgl.oms.asm.ClassVisitor;
import org.osgl.util.E;

public abstract class AppBytecodeEnhancer<T extends AppBytecodeEnhancer> extends AsmBytecodeEnhancer<T> {
    protected App app;

    protected AppBytecodeEnhancer(_.Predicate<String> targetClassPredicate) {
        super(targetClassPredicate);
    }

    protected AppBytecodeEnhancer(_.Predicate<String> targetClassPredicate, ClassVisitor cv) {
        super(targetClassPredicate, cv);
    }

    public AppBytecodeEnhancer app(App app) {
        E.NPE(app);
        this.app = app;
        return this;
    }

}
