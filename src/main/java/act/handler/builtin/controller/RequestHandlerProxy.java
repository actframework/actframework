package act.handler.builtin.controller;

import act.Act;
import act.Destroyable;
import act.app.ActionContext;
import act.app.App;
import act.app.AppInterceptorManager;
import act.controller.meta.ActionMethodMetaInfo;
import act.controller.meta.CatchMethodMetaInfo;
import act.controller.meta.ControllerClassMetaInfo;
import act.controller.meta.InterceptorMethodMetaInfo;
import act.handler.RequestHandlerBase;
import act.security.CORS;
import act.security.CSRF;
import act.view.ActServerError;
import act.view.RenderAny;
import org.osgl.cache.CacheService;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.NoResult;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Pattern;

@ApplicationScoped
public final class RequestHandlerProxy extends RequestHandlerBase {

    private static Logger logger = L.get(RequestHandlerProxy.class);

    protected enum CacheStrategy {
        NO_CACHE() {
            @Override
            public Result cached(ActionContext actionContext, CacheService cache) {
                return null;
            }
        },
        SESSION_SCOPED() {
            @Override
            protected String cacheKey(ActionContext actionContext) {
                H.Session session = actionContext.session();
                return null == session ? null : super.cacheKey(actionContext, session.id());
            }
        },
        GLOBAL_SCOPED;

        public Result cached(ActionContext actionContext, CacheService cache) {
            return cache.get(cacheKey(actionContext));
        }

        protected String cacheKey(ActionContext actionContext) {
            return cacheKey(actionContext, "");
        }

        protected String cacheKey(ActionContext actionContext, String seed) {
            H.Request request = actionContext.req();
            return S.builder("urlcache:").append(seed).append(request.url()).append(request.query()).append(request.accept()).toString();
        }
    }

    private static final C.List<BeforeInterceptor> globalBeforeInterceptors = C.newList();
    private static final C.List<AfterInterceptor> globalAfterInterceptors = C.newList();
    private static final C.List<FinallyInterceptor> globalFinallyInterceptors = C.newList();
    private static final C.List<ExceptionInterceptor> globalExceptionInterceptors = C.newList();

    public static final GroupInterceptorWithResult GLOBAL_BEFORE_INTERCEPTOR = new GroupInterceptorWithResult(globalBeforeInterceptors);
    public static final GroupAfterInterceptor GLOBAL_AFTER_INTERCEPTOR = new GroupAfterInterceptor(globalAfterInterceptors);
    public static final GroupFinallyInterceptor GLOBAL_FINALLY_INTERCEPTOR = new GroupFinallyInterceptor(globalFinallyInterceptors);
    public static final GroupExceptionInterceptor GLOBAL_EXCEPTION_INTERCEPTOR = new GroupExceptionInterceptor(globalExceptionInterceptors);

    private App app;
    private AppInterceptorManager appInterceptor;
    private CacheService cache;
    private CacheStrategy cacheStrategy = CacheStrategy.NO_CACHE;
    private String controllerClassName;
    private String actionMethodName;

    private volatile ControllerAction actionHandler = null;
    private C.List<BeforeInterceptor> beforeInterceptors = C.newList();
    private C.List<AfterInterceptor> afterInterceptors = C.newList();
    private C.List<ExceptionInterceptor> exceptionInterceptors = C.newList();
    private C.List<FinallyInterceptor> finallyInterceptors = C.newList();

    private boolean sessionFree;

    final GroupInterceptorWithResult BEFORE_INTERCEPTOR = new GroupInterceptorWithResult(beforeInterceptors);
    final GroupAfterInterceptor AFTER_INTERCEPTOR = new GroupAfterInterceptor(afterInterceptors);
    final GroupFinallyInterceptor FINALLY_INTERCEPTOR = new GroupFinallyInterceptor(finallyInterceptors);
    final GroupExceptionInterceptor EXCEPTION_INTERCEPTOR = new GroupExceptionInterceptor(exceptionInterceptors);

    @Inject
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
        this.appInterceptor = app.interceptorManager();
    }

    @Override
    protected void releaseResources() {
        _releaseResourceCollections(afterInterceptors);
        _releaseResourceCollections(beforeInterceptors);
        _releaseResourceCollections(exceptionInterceptors);
        _releaseResourceCollections(finallyInterceptors);
        if (null != actionHandler) {
            actionHandler.destroy();
            actionHandler = null;
        }
    }

    public static void releaseGlobalResources() {
        _releaseResourceCollections(globalAfterInterceptors);
        _releaseResourceCollections(globalBeforeInterceptors);
        _releaseResourceCollections(globalExceptionInterceptors);
        _releaseResourceCollections(globalFinallyInterceptors);
    }

    private static void _releaseResourceCollections(Collection<? extends Destroyable> col) {
        Destroyable.Util.destroyAll(col, null);
    }

    public String controller() {
        return controllerClassName;
    }

    public String action() {
        return actionMethodName;
    }

    @Override
    public void handle(ActionContext context) {
        Result result = cacheStrategy.cached(context, cache);
        try {
            if (null != result) {
                onResult(result, context);
                return;
            }
            ensureAgentsReady();
            saveActionPath(context);
            context.startIntercepting();
            result = handleBefore(context);
            if (null == result) {
                context.startHandling();
                result = _handle(context);
            }
            context.startIntercepting();
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
            try {
                result = handleException(e, context);
            } catch (Exception e0) {
                logger.error(e0, "Error invoking exception handler");
            }
            if (null == result) {
                result = ActServerError.of(e);
            }
            try {
                onResult(result, context);
            } catch (Exception e2) {
                logger.error(e2, "error rendering exception handle  result");
                onResult(ActServerError.of(e2), context);
            }
        } finally {
            try {
                handleFinally(context);
            } catch (Exception e) {
                logger.error(e, "Error invoking final handler");
            } finally {
                context.destroy();
            }
        }
    }

    @Override
    public boolean sessionFree() {
        ensureAgentsReady();
        return sessionFree;
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

    private void onResult(Result result, ActionContext context) {
        context.dissolve();
        try {
            if (result instanceof RenderAny) {
                RenderAny any = (RenderAny) result;
                any.apply(context);
            } else {
                H.Request req = context.req();
                H.Response resp = context.resp();
                result.apply(req, resp);
            }
        } catch (Exception e) {
            context.cacheTemplate(null);
            throw e;
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

    // could be used by View to resolve default path to template
    private void saveActionPath(ActionContext context) {
        StringBuilder sb = S.builder(controllerClassName).append(".").append(actionMethodName);
        String path = sb.toString();
        context.actionPath(path);
    }

    private boolean matches(String actionMethodName, Set<String> patterns) {
        if (patterns.contains(actionMethodName)) {
            return true;
        }
        for (String s : patterns) {
            if (Pattern.compile(s).matcher(actionMethodName).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean applied(InterceptorMethodMetaInfo interceptor) {
        Set<String> blackList = interceptor.blackList();
        if (!blackList.isEmpty()) {
            return !matches(actionMethodName, blackList);
        } else {
            Set<String> whiteList = interceptor.whiteList();
            if (!whiteList.isEmpty()) {
                return matches(actionMethodName, whiteList);
            }
            return true;
        }
    }

    private void generateHandlers() {
        ControllerClassMetaInfo ctrlInfo = app.classLoader().controllerClassMetaInfo(controllerClassName);
        ActionMethodMetaInfo actionInfo = ctrlInfo.action(actionMethodName);
        Act.Mode mode = Act.mode();
        actionHandler = mode.createRequestHandler(actionInfo, app);
        sessionFree = actionHandler.sessionFree();
        App app = this.app;
        for (InterceptorMethodMetaInfo info : ctrlInfo.beforeInterceptors()) {
            if (!applied(info)) {
                continue;
            }
            BeforeInterceptor interceptor = mode.createBeforeInterceptor(info, app);
            beforeInterceptors.add(interceptor);
            sessionFree = sessionFree && interceptor.sessionFree();
        }
        for (InterceptorMethodMetaInfo info : ctrlInfo.afterInterceptors()) {
            if (!applied(info)) {
                continue;
            }
            AfterInterceptor interceptor = mode.createAfterInterceptor(info, app);
            afterInterceptors.add(interceptor);
            sessionFree = sessionFree && interceptor.sessionFree();
        }
        for (CatchMethodMetaInfo info : ctrlInfo.exceptionInterceptors()) {
            if (!applied(info)) {
                continue;
            }
            ExceptionInterceptor interceptor = mode.createExceptionInterceptor(info, app);
            exceptionInterceptors.add(interceptor);
            sessionFree = sessionFree && interceptor.sessionFree();
        }
        Collections.sort(exceptionInterceptors);

        for (InterceptorMethodMetaInfo info : ctrlInfo.finallyInterceptors()) {
            if (!applied(info)) {
                continue;
            }
            FinallyInterceptor interceptor = mode.createFinallyInterceptor(info, app);
            finallyInterceptors.add(interceptor);
            sessionFree = sessionFree && interceptor.sessionFree();
        }

    }

    public void accept(Handler.Visitor visitor) {
        ensureAgentsReady();
        for (BeforeInterceptor i : globalBeforeInterceptors) {
            i.accept(visitor);
        }
        for (BeforeInterceptor i : beforeInterceptors) {
            i.accept(visitor);
        }
        actionHandler.accept(visitor);
        for (AfterInterceptor i : afterInterceptors) {
            i.accept(visitor);
        }
        for (AfterInterceptor i : globalAfterInterceptors) {
            i.accept(visitor);
        }
        for (FinallyInterceptor i : finallyInterceptors) {
            i.accept(visitor);
        }
        for (FinallyInterceptor i : globalFinallyInterceptors) {
            i.accept(visitor);
        }
        for (ExceptionInterceptor i : exceptionInterceptors) {
            i.accept(visitor);
        }
        for (ExceptionInterceptor i : globalExceptionInterceptors) {
            i.accept(visitor);
        }
    }

    @Override
    public CSRF.Spec csrfSpec() {
        ensureAgentsReady();
        return actionHandler.csrfSpec();
    }

    @Override
    public CORS.Spec corsSpec() {
        ensureAgentsReady();
        return actionHandler.corsSpec();
    }

    private Result handleBefore(ActionContext actionContext) throws Exception {
        Result r = GLOBAL_BEFORE_INTERCEPTOR.apply(actionContext);
        if (null == r) {
            r = appInterceptor.handleBefore(actionContext);
        }
        if (null == r) {
            r = BEFORE_INTERCEPTOR.apply(actionContext);
        }
        return r;
    }

    private Result _handle(ActionContext actionContext) throws Exception {
        try {
            return actionHandler.handle(actionContext);
        } catch (Result r) {
            return r;
        }
    }

    private Result handleAfter(Result result, ActionContext actionContext) throws Exception {
        result = AFTER_INTERCEPTOR.apply(result, actionContext);
        result = appInterceptor.handleAfter(result, actionContext);
        result = GLOBAL_AFTER_INTERCEPTOR.apply(result, actionContext);
        return result;
    }

    private void handleFinally(ActionContext actionContext) throws Exception {
        FINALLY_INTERCEPTOR.apply(actionContext);
        appInterceptor.handleFinally(actionContext);
        GLOBAL_FINALLY_INTERCEPTOR.apply(actionContext);
    }

    private Result handleException(Exception ex, ActionContext actionContext) throws Exception {
        Result r = EXCEPTION_INTERCEPTOR.apply(ex, actionContext);
        if (null == r) {
            r = appInterceptor.handleException(ex, actionContext);
        }
        if (null == r) {
            r = GLOBAL_EXCEPTION_INTERCEPTOR.apply(ex, actionContext);
        }
        return r;
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
        Collections.sort(globalExceptionInterceptors);
    }

    public static <T extends Handler> void insertInterceptor(C.List<T> list, T i) {
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

    public static class GroupInterceptorWithResult {
        private C.List<? extends ActionHandler> interceptors;

        public GroupInterceptorWithResult(C.List<? extends ActionHandler> interceptors) {
            this.interceptors = interceptors;
        }

        public Result apply(ActionContext actionContext) throws Exception {
            try {
                if (interceptors.isEmpty()) return null;
                for (ActionHandler i : interceptors) {
                    Result r = i.handle(actionContext);
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

    public static class GroupAfterInterceptor {
        private C.List<? extends AfterInterceptor> interceptors;

        public GroupAfterInterceptor(C.List<? extends AfterInterceptor> interceptors) {
            this.interceptors = interceptors;
        }

        public Result apply(Result result, ActionContext actionContext) throws Exception {
            for (AfterInterceptor i : interceptors) {
                result = i.handle(result, actionContext);
            }
            return result;
        }
    }

    public static class GroupFinallyInterceptor {
        private C.List<? extends FinallyInterceptor> interceptors;

        public GroupFinallyInterceptor(C.List<FinallyInterceptor> interceptors) {
            this.interceptors = interceptors;
        }

        public Void apply(ActionContext actionContext) throws Exception {
            if (interceptors.isEmpty()) return null;
            for (FinallyInterceptor i : interceptors) {
                i.handle(actionContext);
            }
            return null;
        }
    }

    public static class GroupExceptionInterceptor {
        private C.List<? extends ExceptionInterceptor> interceptors;

        public GroupExceptionInterceptor(C.List<? extends ExceptionInterceptor> interceptors) {
            this.interceptors = interceptors;
        }

        public Result apply(Exception e, ActionContext actionContext) throws Exception {
            try {
                if (interceptors.isEmpty()) return null;
                for (ExceptionInterceptor i : interceptors) {
                    Result r = i.handle(e, actionContext);
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
