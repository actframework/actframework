package org.osgl.oms.util;

import org.osgl._;
import org.osgl.oms.app.App;
import org.osgl.oms.asm.ClassVisitor;
import org.osgl.util.E;

public abstract class AppByteCodeEnhancer<T extends AppByteCodeEnhancer> extends AsmByteCodeEnhancer<T> {
    protected App app;

    protected AppByteCodeEnhancer(_.Predicate<String> targetClassPredicate) {
        super(targetClassPredicate);
    }

    protected AppByteCodeEnhancer(_.Predicate<String> targetClassPredicate, ClassVisitor cv) {
        super(targetClassPredicate, cv);
    }

    public AppByteCodeEnhancer app(App app) {
        E.NPE(app);
        this.app = app;
        return this;
    }

}
