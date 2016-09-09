package act.handler.builtin.controller;

import act.app.ActionContext;
import act.plugin.Plugin;
import act.security.CORS;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.Comparator;
import java.util.List;

public abstract class ExceptionInterceptor
        extends Handler<ExceptionInterceptor>
        implements Plugin, ExceptionInterceptorInvoker, Comparable<ExceptionInterceptor> {

    private List<Class<? extends Exception>> exClasses;

    public ExceptionInterceptor() {
        this(0);
    }

    @SuppressWarnings("unchecked")
    public ExceptionInterceptor(int priority) {
        this(priority, new Class[]{});
    }

    public ExceptionInterceptor(Class<? extends Exception> ... exClasses) {
        this(0, exClasses);
    }

    public ExceptionInterceptor(int priority, Class<? extends Exception> ... exClasses) {
        super(priority);
        E.illegalArgumentIf(exClasses.length == 0);
        this.exClasses = C.listOf(exClasses).sorted(EXCEPTION_WEIGHT_COMPARATOR);
    }

    public ExceptionInterceptor(int priority, List<Class<? extends Exception>> exClasses) {
        super(priority);
        E.illegalArgumentIf(exClasses.isEmpty());
        this.exClasses = C.list(exClasses).sorted(EXCEPTION_WEIGHT_COMPARATOR);
    }

    @Override
    public Result handle(Exception e, ActionContext actionContext) throws Exception {
        for (Class<? extends Exception> c : exClasses) {
            if (c.isInstance(e)) {
                return internalHandle(e, actionContext);
            }
        }
        return null;
    }

    @Override
    public int compareTo(ExceptionInterceptor o) {
        return EXCEPTION_WEIGHT_COMPARATOR.compare(o.exClasses.get(0), exClasses.get(0));
    }

    @Override
    public CORS.Spec corsSpec() {
        return CORS.Spec.DUMB;
    }

    protected abstract Result internalHandle(Exception e, ActionContext actionContext) throws Exception;

    @Override
    public void register() {
        RequestHandlerProxy.registerGlobalInterceptor(this);
    }

    public static Comparator<Class<? extends Exception>> EXCEPTION_WEIGHT_COMPARATOR = new Comparator<Class<? extends Exception>>() {
        @Override
        public int compare(Class<? extends Exception> o1, Class<? extends Exception> o2) {
            return weight(o1) - weight(o2);
        }
    };

    public static int weight(Class c) {
        int i = 0;
        while (c != Object.class) {
            i++;
            c = c.getSuperclass();
        }
        return i;
    }
}
