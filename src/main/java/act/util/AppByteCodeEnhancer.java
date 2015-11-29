package act.util;

import act.app.App;
import act.asm.ClassVisitor;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.E;

public abstract class AppByteCodeEnhancer<T extends AppByteCodeEnhancer> extends AsmByteCodeEnhancer<T> {
    protected static Logger logger = LogManager.get(App.class);
    protected App app;

    protected AppByteCodeEnhancer($.Predicate<String> targetClassPredicate) {
        super(targetClassPredicate);
    }

    protected AppByteCodeEnhancer($.Predicate<String> targetClassPredicate, ClassVisitor cv) {
        super(targetClassPredicate, cv);
    }

    public AppByteCodeEnhancer app(App app) {
        E.NPE(app);
        this.app = app;
        return this;
    }

}
