package act.handler.builtin.controller;

import act.app.ActionContext;
import act.plugin.Plugin;
import act.util.CORS;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;

import java.util.List;

public abstract class ExceptionInterceptor
        extends Handler<ExceptionInterceptor>
        implements Plugin, ExceptionInterceptorInvoker {

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
        this.exClasses = C.listOf(exClasses);
    }

    public ExceptionInterceptor(int priority, List<Class<? extends Exception>> exClasses) {
        super(priority);
        this.exClasses = C.list(exClasses);
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
    public CORS.Handler corsHandler() {
        return CORS.Handler.DUMB;
    }

    protected abstract Result internalHandle(Exception e, ActionContext actionContext) throws Exception;

    @Override
    public void register() {
        RequestHandlerProxy.registerGlobalInterceptor(this);
    }
}
