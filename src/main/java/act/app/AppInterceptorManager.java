package act.app;

import act.handler.RequestHandler;
import act.handler.builtin.controller.*;
import act.handler.builtin.controller.RequestHandlerProxy.GroupAfterInterceptor;
import act.handler.builtin.controller.RequestHandlerProxy.GroupExceptionInterceptor;
import act.handler.builtin.controller.RequestHandlerProxy.GroupFinallyInterceptor;
import act.handler.builtin.controller.RequestHandlerProxy.GroupInterceptorWithResult;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;

import static act.handler.builtin.controller.RequestHandlerProxy.insertInterceptor;

/**
 * Manage interceptors at App level
 */
public class AppInterceptorManager {
    private C.List<BeforeInterceptor> beforeInterceptors = C.newList();
    private C.List<AfterInterceptor> afterInterceptors = C.newList();
    private C.List<ExceptionInterceptor> exceptionInterceptors = C.newList();
    private C.List<FinallyInterceptor> finallyInterceptors = C.newList();

    final GroupInterceptorWithResult BEFORE_INTERCEPTOR = new GroupInterceptorWithResult(beforeInterceptors);
    final GroupAfterInterceptor AFTER_INTERCEPTOR = new GroupAfterInterceptor(afterInterceptors);
    final GroupFinallyInterceptor FINALLY_INTERCEPTOR = new GroupFinallyInterceptor(finallyInterceptors);
    final GroupExceptionInterceptor EXCEPTION_INTERCEPTOR = new GroupExceptionInterceptor(exceptionInterceptors);

    public Result handleBefore(AppContext appContext) {
        return BEFORE_INTERCEPTOR.apply(appContext);
    }

    public Result handleAfter(Result result, AppContext appContext) {
        return AFTER_INTERCEPTOR.apply(result, appContext);
    }

    public void handleFinally(AppContext appContext) {
        FINALLY_INTERCEPTOR.apply(appContext);
    }


    public Result handleException(Exception ex, AppContext appContext) {
        return EXCEPTION_INTERCEPTOR.apply(ex, appContext);
    }

    public void registerInterceptor(BeforeInterceptor interceptor) {
        insertInterceptor(beforeInterceptors, interceptor);
    }

    public void registerInterceptor(AfterInterceptor interceptor) {
        insertInterceptor(afterInterceptors, interceptor);
    }

    public void registerInterceptor(FinallyInterceptor interceptor) {
        insertInterceptor(finallyInterceptors, interceptor);
    }

    public void registerInterceptor(ExceptionInterceptor interceptor) {
        insertInterceptor(exceptionInterceptors, interceptor);
    }

}
