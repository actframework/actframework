package act.util;

import act.asm.ClassVisitor;
import org.osgl._;
import act.app.App;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.E;

public abstract class AppByteCodeEnhancer<T extends AppByteCodeEnhancer> extends AsmByteCodeEnhancer<T> {
    protected static Logger logger = L.get(App.class);
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
