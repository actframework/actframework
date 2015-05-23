package org.osgl.oms.handler.builtin.controller;

import org.osgl._;
import org.osgl.cache.CacheService;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.NoResult;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.result.ServerError;
import org.osgl.oms.OMS;
import org.osgl.oms.app.App;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.controller.meta.ActionMethodMetaInfo;
import org.osgl.oms.controller.meta.CatchMethodMetaInfo;
import org.osgl.oms.controller.meta.ControllerClassMetaInfo;
import org.osgl.oms.controller.meta.InterceptorMethodMetaInfo;
import org.osgl.oms.handler.RequestHandlerBase;
import org.osgl.oms.view.RenderAny;
import org.osgl.oms.view.RenderTemplate;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.ListIterator;

public final class RequestHandlerProxy extends RequestHandlerBase {

    private static Logger logger = L.get(RequestHandlerProxy.class);

    protected static enum CacheStrategy {
        NO_CACHE() {
            @Override
            public Result cached(AppContext appContext, CacheService cache) {
                return null;
            }
        },
        SESSION_SCOPED() {
            @Override
            protected String cacheKey(AppContext appContext) {
                H.Session session = appContext.session();
                return null == session ? null : super.cacheKey(appContext, session.id());
            }
        },
        GLOBAL_SCOPED;

        public Result cached(AppContext appContext, CacheService cache) {
            return cache.get(cacheKey(appContext));
        }

        protected String cacheKey(AppContext appContext) {
            return cacheKey(appContext, "");
        }

        protected String cacheKey(AppContext appContext, String seed) {
            H.Request request = appContext.req();
            return S.builder("urlcache:").append(seed).append(request.url()).append(request.query()).toString();
        }
    }

    private static final C.List<BeforeInterceptor> globalBeforeInterceptors = C.newList();
    private static final C.List<AfterInterceptor> globalAfterInterceptors = C.newList();
    private static final C.List<FinallyInterceptor> globalFinallyInterceptors = C.newList();
    private static final C.List<ExceptionInterceptor> globalExceptionInterceptors = C.newList();

    static final GroupInterceptorWithResult GLOBAL_BEFORE_INTERCEPTOR = new GroupInterceptorWithResult(globalBeforeInterceptors);
    static final GroupAfterInterceptor GLOBAL_AFTER_INTERCEPTOR = new GroupAfterInterceptor(globalAfterInterceptors);
    static final GroupFinallyInterceptor GLOBAL_FINALLY_INTERCEPTOR = new GroupFinallyInterceptor(globalFinallyInterceptors);
    static final GroupExceptionInterceptor GLOBAL_EXCEPTION_INTERCEPTOR = new GroupExceptionInterceptor(globalExceptionInterceptors);

    private App app;
    private CacheService cache;
    private CacheStrategy cacheStrategy = CacheStrategy.NO_CACHE;
    private String controllerClassName;
    private String actionMethodName;
    private Boolean requireContextLocal = null;

    private volatile ControllerAction actionHandler = null;
    private C.List<BeforeInterceptor> beforeInterceptors = C.newList();
    private C.List<AfterInterceptor> afterInterceptors = C.newList();
    private C.List<ExceptionInterceptor> exceptionInterceptors = C.newList();
    private C.List<FinallyInterceptor> finallyInterceptors = C.newList();

    final GroupInterceptorWithResult BEFORE_INTERCEPTOR = new GroupInterceptorWithResult(beforeInterceptors);
    final GroupAfterInterceptor AFTER_INTERCEPTOR = new GroupAfterInterceptor(afterInterceptors);
    final GroupFinallyInterceptor FINALLY_INTERCEPTOR = new GroupFinallyInterceptor(finallyInterceptors);
    final GroupExceptionInterceptor EXCEPTION_INTERCEPTOR = new GroupExceptionInterceptor(exceptionInterceptors);

    public RequestHandlerProxy(String actionMethodName, App app) {
        int pos = actionMethodName.lastIndexOf('.');
        final String ERR = "Invalid controller action: %s";
        E.illegalArgumentIf(pos < 0, ERR, actionMethodName);
        controllerClassName = actionMethodName.substring(0, pos);
        E.illegalArgumentIf(S.isEmpty(controllerClassName), ERR, actionMethodName);
        this.actionMethodName = actionMethodName.substring(pos + 1);
        E.illegalArgumentIf(S.isEmpty(this.actionMethodName), ERR, actionMethodName);
        cache = app.config().cacheService("action_proxy");
        this.app = app;
    }

    public String controller() {
        return controllerClassName;
    }

    public String action() {
        return actionMethodName;
    }

    @Override
    public void handle(AppContext context) {
        Result result = cacheStrategy.cached(context, cache);
        try {
            if (null != result) {
                onResult(result, context);
                return;
            }
            ensureAgentsReady();
            ensureContextLocal(context);
            saveActionPath(context);
            result = handleBefore(context);
            if (null == result) {
                result = _handle(context);
            }
            Result afterResult = handleAfter(result, context);
            if (null != afterResult) {
                result = afterResult;
            }
            if (null == result) {
                result = new NoResult();
            }
            onResult(result, context);
        } catch (Exception e) {
            logger.error(e, "Error handling request");
            result = handleException(e, context);
            if (null == result) {
                result = new ServerError(e);
            }
            try {
                onResult(result, context);
            } catch (Exception e2) {
                logger.error(e2, "error rendering exception handle  result");
                onResult(new ServerError(), context);
            }
        } finally {
            handleFinally(context);
        }
    }

    protected final void useSessionCache() {
        cacheStrategy = CacheStrategy.SESSION_SCOPED;
    }

    protected final void useGlobalCache() {
        cacheStrategy = CacheStrategy.GLOBAL_SCOPED;
    }

    protected final void registerBeforeInterceptor(BeforeInterceptor interceptor) {
        insertInterceptor(beforeInterceptors, interceptor);
    }

    protected final void registerAfterInterceptor(AfterInterceptor interceptor) {
        insertInterceptor(afterInterceptors, interceptor);
    }

    protected final void registerExceptionInterceptor(ExceptionInterceptor interceptor) {
        insertInterceptor(exceptionInterceptors, interceptor);
    }

    protected final void registerFinallyInterceptor(FinallyInterceptor interceptor) {
        insertInterceptor(finallyInterceptors, interceptor);
    }

    private void onResult(Result result, AppContext context) {
        try {
            context.dissolve();
            if (result instanceof RenderAny) {
                RenderAny any = (RenderAny) result;
                any.apply(context);
            } else {
                H.Request req = context.req();
                H.Response resp = context.resp();
                result.apply(req, resp);
            }
        } finally {
            context.destroy();
        }
    }

    private void ensureAgentsReady() {
        if (null == actionHandler) {
            synchronized (this) {
                if (null == actionHandler) {
                    generateHandlers();
                }
            }
        }
    }

    private void ensureContextLocal(AppContext context) {
        if (requireContextLocal) {
            context.saveLocal();
        }
    }

    // could be used by View to resolve default path to template
    private void saveActionPath(AppContext context) {
        StringBuilder sb = S.builder(controllerClassName).append(".").append(actionMethodName);
        String path = sb.toString();
        context.actionPath(path);
    }

    private void generateHandlers() {
        ControllerClassMetaInfo ctrlInfo = app.classLoader().controllerClassMetaInfo(controllerClassName);
        ActionMethodMetaInfo actionInfo = ctrlInfo.action(actionMethodName);
        OMS.Mode mode = OMS.mode();
        actionHandler = mode.createRequestHandler(actionInfo, app);
        requireContextLocal = false;
        if (actionInfo.appContextInjection().injectVia().isLocal()) {
            requireContextLocal = true;
        }
        App app = this.app;
        for (InterceptorMethodMetaInfo info : ctrlInfo.beforeInterceptors()) {
            beforeInterceptors.add(mode.createBeforeInterceptor(info, app));
            if (info.appContextInjection().injectVia().isLocal()) {
                requireContextLocal = true;
            }
        }
        for (InterceptorMethodMetaInfo info : ctrlInfo.afterInterceptors()) {
            afterInterceptors.add(mode.createAfterInterceptor(info, app));
            if (info.appContextInjection().injectVia().isLocal()) {
                requireContextLocal = true;
            }
        }
        for (CatchMethodMetaInfo info : ctrlInfo.exceptionInterceptors()) {
            exceptionInterceptors.add(mode.createExceptionInterceptor(info, app));
            if (info.appContextInjection().injectVia().isLocal()) {
                requireContextLocal = true;
            }
        }
        for (InterceptorMethodMetaInfo info : ctrlInfo.finallyInterceptors()) {
            finallyInterceptors.add(mode.createFinallyInterceptor(info, app));
            if (info.appContextInjection().injectVia().isLocal()) {
                requireContextLocal = true;
            }
        }
    }

    private Result handleBefore(AppContext appContext) {
        Result r = GLOBAL_BEFORE_INTERCEPTOR.apply(appContext);
        if (null == r) {
            r = BEFORE_INTERCEPTOR.apply(appContext);
        }
        return r;
    }

    private Result _handle(AppContext appContext) {
        try {
            return actionHandler.handle(appContext);
        } catch (Result r) {
            return r;
        }
    }

    private Result handleAfter(Result result, AppContext appContext) {
        result = AFTER_INTERCEPTOR.apply(result, appContext);
        result = GLOBAL_AFTER_INTERCEPTOR.apply(result, appContext);
        return result;
    }

    private void handleFinally(AppContext appContext) {
        FINALLY_INTERCEPTOR.apply(appContext);
        GLOBAL_FINALLY_INTERCEPTOR.apply(appContext);
    }

    private Result handleException(Exception ex, AppContext appContext) {
        Result r = EXCEPTION_INTERCEPTOR.apply(ex, appContext);
        if (null == r) {
            r = GLOBAL_EXCEPTION_INTERCEPTOR.apply(ex, appContext);
        }
        return r;
    }

    private ActionMethodMetaInfo lookupAction() {
        ControllerClassMetaInfo ctrl = app.classLoader().controllerClassMetaInfo(controllerClassName);
        return ctrl.action(actionMethodName);
    }

    @Override
    public String toString() {
        return S.fmt("%s.%s", controllerClassName, actionMethodName);
    }

    public static void registerGlobalInterceptor(BeforeInterceptor interceptor) {
        insertInterceptor(globalBeforeInterceptors, interceptor);
    }

    public static void registerGlobalInterceptor(AfterInterceptor interceptor) {
        insertInterceptor(globalAfterInterceptors, interceptor);
    }

    public static void registerGlobalInterceptor(FinallyInterceptor interceptor) {
        insertInterceptor(globalFinallyInterceptors, interceptor);
    }

    public static void registerGlobalInterceptor(ExceptionInterceptor interceptor) {
        insertInterceptor(globalExceptionInterceptors, interceptor);
    }

    private static <T extends Handler> void insertInterceptor(C.List<T> list, T i) {
        int sz = list.size();
        if (0 == sz) {
            list.add(i);
        }
        ListIterator<T> itr = list.listIterator();
        while (itr.hasNext()) {
            T t = itr.next();
            int n = i.compareTo(t);
            if (n < 0) {
                itr.add(i);
                return;
            } else if (n == 0) {
                if (i.equals(t)) {
                    // already exists
                    return;
                } else {
                    itr.add(i);
                    return;
                }
            }
        }
        list.add(i);
    }

    private static class GroupInterceptorWithResult extends _.F1<AppContext, Result> {
        C.List<? extends ActionHandler> interceptors;

        GroupInterceptorWithResult(C.List<? extends ActionHandler> interceptors) {
            this.interceptors = interceptors;
        }

        @Override
        public Result apply(AppContext appContext) throws NotAppliedException, _.Break {
            try {
                if (interceptors.isEmpty()) return null;
                for (ActionHandler i : interceptors) {
                    Result r = i.handle(appContext);
                    if (null != r) {
                        return r;
                    }
                }
                return null;
            } catch (Result r) {
                return r;
            }
        }
    }

    private static class GroupAfterInterceptor extends _.F2<Result, AppContext, Result> {
        C.List<? extends AfterInterceptor> interceptors;

        GroupAfterInterceptor(C.List<? extends AfterInterceptor> interceptors) {
            this.interceptors = interceptors;
        }

        @Override
        public Result apply(Result result, AppContext appContext) throws NotAppliedException, _.Break {
            for (AfterInterceptor i : interceptors) {
                result = i.handle(result, appContext);
            }
            return result;
        }
    }

    private static class GroupFinallyInterceptor extends _.F1<AppContext, Void> {
        C.List<? extends FinallyInterceptor> interceptors;

        GroupFinallyInterceptor(C.List<FinallyInterceptor> interceptors) {
            this.interceptors = interceptors;
        }

        @Override
        public Void apply(AppContext appContext) throws NotAppliedException, _.Break {
            if (interceptors.isEmpty()) return null;
            for (FinallyInterceptor i : interceptors) {
                i.handle(appContext);
            }
            return null;
        }
    }

    private static class GroupExceptionInterceptor extends _.F2<Exception, AppContext, Result> {
        C.List<? extends ExceptionInterceptor> interceptors;

        GroupExceptionInterceptor(C.List<? extends ExceptionInterceptor> interceptors) {
            this.interceptors = interceptors;
        }

        @Override
        public Result apply(Exception e, AppContext appContext) throws NotAppliedException, _.Break {
            try {
                if (interceptors.isEmpty()) return null;
                for (ExceptionInterceptor i : interceptors) {
                    Result r = i.handle(e, appContext);
                    if (null != r) {
                        return r;
                    }
                }
                return null;
            } catch (Result r) {
                return r;
            }
        }
    }
}
