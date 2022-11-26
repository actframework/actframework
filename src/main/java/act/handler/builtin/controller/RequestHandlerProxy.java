package act.handler.builtin.controller;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static act.app.ActionContext.contentTypeForErrorResult;
import static org.osgl.http.H.Method.GET;
import static org.osgl.http.H.Method.POST;

import act.*;
import act.app.*;
import act.app.event.SysEventId;
import act.controller.CacheSupportMetaInfo;
import act.controller.ResponseCache;
import act.controller.meta.*;
import act.handler.RequestHandlerBase;
import act.inject.util.Sorter;
import act.security.CORS;
import act.security.CSRF;
import act.util.*;
import act.view.ActBadRequest;
import act.view.ActErrorResult;
import act.view.RenderAny;
import act.xio.WebSocketConnectionHandler;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.*;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public final class RequestHandlerProxy extends RequestHandlerBase {

    public static final String CACHE_NAME = "__action_proxy__";

    private static Logger logger = L.get(RequestHandlerProxy.class);

    private static final List<BeforeInterceptor> globalBeforeInterceptors = new ArrayList<>();
    private static final List<AfterInterceptor> globalAfterInterceptors = new ArrayList<>();
    private static final List<FinallyInterceptor> globalFinallyInterceptors = new ArrayList<>();
    private static final List<ExceptionInterceptor> globalExceptionInterceptors = new ArrayList<>();
    // for @Global on classes
    private static final Set<GroupInterceptorMetaInfo> globalFreeStyleInterceptors = new HashSet<>();
    // for @Global on methods
    private static GroupInterceptorMetaInfo globalFreeStyleInterceptor = new GroupInterceptorMetaInfo();

    public static final GroupInterceptorWithResult GLOBAL_BEFORE_INTERCEPTOR = new GroupInterceptorWithResult(globalBeforeInterceptors);
    public static final GroupAfterInterceptor GLOBAL_AFTER_INTERCEPTOR = new GroupAfterInterceptor(globalAfterInterceptors);
    public static final GroupFinallyInterceptor GLOBAL_FINALLY_INTERCEPTOR = new GroupFinallyInterceptor(globalFinallyInterceptors);
    public static final GroupExceptionInterceptor GLOBAL_EXCEPTION_INTERCEPTOR = new GroupExceptionInterceptor(globalExceptionInterceptors);

    private App app;
    private AppInterceptorManager appInterceptor;
    private CacheService cache;
    private String controllerClassName;
    private String actionMethodName;
    private String actionPath;
    private Method actionMethod;
    private Set<String> cacheKeys = new HashSet<>();

    private volatile ControllerAction actionHandler = null;
    private List<BeforeInterceptor> beforeInterceptors = new ArrayList<>();
    private List<AfterInterceptor> afterInterceptors = new ArrayList<>();
    private List<ExceptionInterceptor> exceptionInterceptors = new ArrayList<>();
    private List<FinallyInterceptor> finallyInterceptors = new ArrayList<>();

    private boolean sessionFree;
    private boolean express;
    private boolean skipEvents;
    private boolean supportCache;
    private CacheSupportMetaInfo cacheSupport;
    private MissingAuthenticationHandler missingAuthenticationHandler;
    private MissingAuthenticationHandler csrfFailureHandler;

    private CacheFor.Manager cacheForManager;

    private WebSocketConnectionHandler webSocketConnectionHandler;

    final GroupInterceptorWithResult BEFORE_INTERCEPTOR = new GroupInterceptorWithResult(beforeInterceptors);
    final GroupAfterInterceptor AFTER_INTERCEPTOR = new GroupAfterInterceptor(afterInterceptors);
    final GroupFinallyInterceptor FINALLY_INTERCEPTOR = new GroupFinallyInterceptor(finallyInterceptors);
    final GroupExceptionInterceptor EXCEPTION_INTERCEPTOR = new GroupExceptionInterceptor(exceptionInterceptors);

    @Inject
    public RequestHandlerProxy(final String actionMethodName, final App app) {
        int pos = actionMethodName.lastIndexOf('.');
        final String ERR = "Invalid controller action: %s";
        E.illegalArgumentIf(pos < 0, ERR, actionMethodName);
        controllerClassName = actionMethodName.substring(0, pos);
        E.illegalArgumentIf(S.isEmpty(controllerClassName), ERR, actionMethodName);
        this.actionMethodName = actionMethodName.substring(pos + 1);
        E.illegalArgumentIf(S.isEmpty(this.actionMethodName), ERR, actionMethodName);
        this.actionPath = actionMethodName;
        if (app.classLoader() != null) {
            cache = app.config().cacheService(CACHE_NAME);
        } else {
            app.jobManager().on(SysEventId.CLASS_LOADER_INITIALIZED, "RequestHandlerProxy[" + actionMethodName + "]:setCache", new Runnable() {
                @Override
                public void run() {
                    cache = app.config().cacheService(CACHE_NAME);
                }
            });
        }
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
        cacheForManager = null;
    }

    public static void releaseGlobalResources() {
        _releaseResourceCollections(globalAfterInterceptors);
        _releaseResourceCollections(globalBeforeInterceptors);
        _releaseResourceCollections(globalExceptionInterceptors);
        _releaseResourceCollections(globalFinallyInterceptors);
        _releaseResourceCollections(globalFreeStyleInterceptors);
        globalFreeStyleInterceptor.destroy();
        // We must recreate this instance to prevent it from
        // been reused after destroyed
        globalFreeStyleInterceptor = new GroupInterceptorMetaInfo();
    }

    private static void _releaseResourceCollections(Collection<? extends Destroyable> col) {
        Destroyable.Util.destroyAll(col, null);
        col.clear();
    }

    public String controller() {
        return controllerClassName;
    }

    public String action() {
        return actionMethodName;
    }

    public ControllerAction actionHandler() {
        ensureAgentsReady();
        return actionHandler;
    }

    @Override
    public void handle(ActionContext context) {
        ensureAgentsReady();
        context.handlerMethod(actionMethod);
        if (null != webSocketConnectionHandler) {
            webSocketConnectionHandler.handle(context);
            return;
        }
        Result result = null;
        try {
            H.Method method = context.req().method();
            boolean supportCache = this.supportCache && method == GET || (cacheSupport.supportPost && method == POST);
            String cacheKey = null;
            if (supportCache) {
                context.enableCache();
                if (!this.cacheSupport.etagOnly) {
                    cacheKey = cacheSupport.cacheKey(context);
                    ResponseCache cached = this.cache.get(cacheKey);
                    if (null != cached && cached.isValid()) {
                        String etag = cached.etag();
                        if (null != etag && context.req().etagMatches(etag)) {
                            NotModified.of(etag).apply(context.req(), context.resp());
                        } else {
                            cached.applyTo(context.prepareRespForResultEvaluation());
                        }
                        return;
                    }
                }
            }
            saveActionPath(context);
            setHandlerClass(context);
            context.startIntercepting();
            result = handleBefore(context);
            if (null == result) {
                context.startHandling();
                result = _handle(context);
            }
            if (context.resp().isClosed()) {
                return;
            }
            context.startIntercepting();
            Result afterResult = handleAfter(result, context);
            if (null != afterResult) {
                result = afterResult;
            }
            if (null == result) {
                result = context.nullValueResult();
            }
            if (supportCache) {
                if (!cacheSupport.noCacheControl) {
                    S.Buffer buf = S.buffer();
                    if (cacheSupport.usePrivate) {
                        buf.append("private ");
                    }
                    if (cacheSupport.noCache || cacheSupport.etagOnly) {
                        buf.append("no-cache ");
                    }
                    buf.append("max-age=");
                    context.resp().addHeaderIfNotAdded(H.Header.Names.CACHE_CONTROL, buf.append(cacheSupport.ttl).toString());
                }
                String newEtag = S.string(context.resultHashForEtag());
                if (context.req().etagMatches(newEtag)) {
                    result = NotModified.of(newEtag);
                }
            }
            onResult(result, context);
            if (supportCache && !cacheSupport.etagOnly) {
                this.cache.put(cacheKey, context.resp(), cacheSupport.ttl);
                cacheKeys.add(cacheKey);
            }
        } catch (Exception e) {
            try {
                result = handleException(e, context);
            } catch (Exception e0) {
                logger.error(e0, "Error invoking exception handler");
            }
            if (context.resp().isClosed()) {
                logger.error(e, "Error committing result");
            } else {
                logger.error(e, "Error handling request: " + context.req().url());
                if (null == result) {
                    if (e instanceof IllegalArgumentException) {
                        String errorMsg = e.getLocalizedMessage();
                        logger.warn(e, errorMsg);
                        result = ActBadRequest.create(e, errorMsg);
                    } else {
                        H.Request req = context.req();
                        result = ActErrorResult.of(e);
                    }
                }
                try {
                    onResult(result, context);
                } catch (Exception e2) {
                    logger.error(e2, "error rendering exception handle result");
                    onResult(ActErrorResult.of(e2), context);
                }
            }
        } finally {
            try {
                handleFinally(context);
            } catch (Exception e) {
                logger.error(e, "Error invoking final handler");
            }
        }
    }

    @Override
    public boolean sessionFree() {
        ensureAgentsReady();
        return sessionFree;
    }

    @Override
    public void prepareAuthentication(ActionContext context) {
        if (null != missingAuthenticationHandler) {
            context.forceMissingAuthenticationHandler(missingAuthenticationHandler);
        }
        if (null != csrfFailureHandler) {
            context.forceCsrfCheckingFailureHandler(csrfFailureHandler);
        }
    }

    @Override
    public boolean express(ActionContext context) {
        ensureAgentsReady();
        return express;
    }

    @Override
    public boolean skipEvents(ActionContext context) {
        ensureAgentsReady();
        return skipEvents;
    }

    public void resetCache() {
        if (supportCache) {
            for (String cacheKey : cacheKeys) {
                cache.evict(cacheKey);
            }
        }
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
        boolean isRenderAny = false;
        try {
            context.applyResultHashToEtag();
            if (result instanceof RenderAny) {
                RenderAny any = (RenderAny) result;
                isRenderAny = true;
                any.apply(context);
            } else {
                H.Request req = context.req();
                ActResponse<?> resp = context.prepareRespForResultEvaluation();
                if (result instanceof ErrorResult) {
                    // see https://github.com/actframework/actframework/issues/1034
                    H.Format fmt = contentTypeForErrorResult(req);
                    req.accept(fmt);
                    resp.contentType(fmt);
                }
                result.apply(req, resp);
            }
        } catch (RuntimeException e) {
            context.cacheTemplate(null);
            throw e;
        } finally {
            if (isRenderAny) {
                RenderAny.clearThreadLocals();
            }
        }
    }

    private void ensureAgentsReady() {
        if (null == actionHandler) {
            synchronized (this) {
                if (null == actionHandler) {
                    try {
                        generateHandlers();
                    } catch (RuntimeException e) {
                        app.handleBlockIssue(e);
                    }
                }
            }
        }
    }

    // could be used by View to resolve default path to template
    private void saveActionPath(ActionContext context) {
        context.actionPath(actionPath);
    }

    // in case interceptor class is super type of action handler class
    private void setHandlerClass(ActionContext context) {
        context.handlerClass(actionHandler.invoker().invokeMethod().getDeclaringClass());
    }

    private boolean matches(Set<String> patterns) {
        if (patterns.contains(actionMethodName) || patterns.contains(actionPath)) {
            return true;
        }
        for (String s : patterns) {
            if (Pattern.compile(s).matcher(actionPath).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean applied(InterceptorMethodMetaInfo interceptor) {
        Set<String> blackList = interceptor.blackList();
        if (!blackList.isEmpty()) {
            return !matches(blackList);
        } else {
            Set<String> whiteList = interceptor.whiteList();
            if (!whiteList.isEmpty()) {
                return matches(whiteList);
            }
            return true;
        }
    }

    private ActionMethodMetaInfo findActionInfoFromParent(ControllerClassMetaInfo ctrlInfo, String methodName) {
        ActionMethodMetaInfo actionInfo;
        ControllerClassMetaInfo parent = ctrlInfo.parent(true);
        if (null == parent) {
            throw new UnexpectedException("Cannot find action method meta info: %s", actionPath);
        }
        while (true) {
            actionInfo = parent.action(methodName);
            if (null != actionInfo) {
                break;
            }
            parent = parent.parent(true);
            if (null == parent) {
                break;
            }
        }
        return new ActionMethodMetaInfo($.requireNotNull(actionInfo), ctrlInfo);
    }

    private WebSocketConnectionHandler tryGenerateWebSocketConnectionHandler(ActionMethodMetaInfo methodInfo) {
        WebSocketConnectionHandler wsHandler = Act.network().createWebSocketConnectionHandler(methodInfo);
        return null == wsHandler || !wsHandler.isWsHandler() ? null : wsHandler;
    }

    private void generateHandlers() {
        ControllerClassMetaInfo ctrlInfo = app.classLoader().controllerClassMetaInfo(controllerClassName);
        ActionMethodMetaInfo actionInfo = ctrlInfo.action(actionMethodName);
        if (null == actionInfo) {
            actionInfo = findActionInfoFromParent(ctrlInfo, actionMethodName);
        }
        webSocketConnectionHandler = tryGenerateWebSocketConnectionHandler(actionInfo);
//        if (null != webSocketConnectionHandler) {
//            return;
//        }
        Act.Mode mode = Act.mode();
        actionHandler = mode.createRequestHandler(actionInfo, app);
        actionMethod = actionHandler.invoker().invokeMethod();
        sessionFree = actionHandler.sessionFree();
        missingAuthenticationHandler = actionHandler.missingAuthenticationHandler();
        csrfFailureHandler = actionHandler.csrfFailureHandler();
        express = actionHandler.express();
        skipEvents = actionHandler.skipEvents();
        cacheSupport = actionHandler.cacheSupport();
        supportCache = cacheSupport.enabled;

        App app = this.app;
        if (supportCache) {
            cache = app.cache();
            cacheForManager = app.getInstance(CacheFor.Manager.class);
            cacheForManager.register(actionPath, this);
            String cacheForId = cacheSupport.id;
            if (S.notBlank(cacheForId)) {
                cacheForManager.register(cacheForId, this);
            }
        }

        GroupInterceptorMetaInfo interceptorMetaInfo = new GroupInterceptorMetaInfo(actionInfo.interceptors());
        interceptorMetaInfo.mergeFrom(globalFreeStyleInterceptor);
        for (GroupInterceptorMetaInfo freeStyleInterceptor : globalFreeStyleInterceptors) {
            interceptorMetaInfo.mergeFrom(freeStyleInterceptor);
        }

        for (InterceptorMethodMetaInfo info : interceptorMetaInfo.beforeList()) {
            if (!applied(info)) {
                continue;
            }
            BeforeInterceptor interceptor = mode.createBeforeInterceptor(info, app);
            insertInterceptor(beforeInterceptors, interceptor);
            sessionFree = sessionFree && interceptor.sessionFree();
            express = express && interceptor.express();
        }
        for (InterceptorMethodMetaInfo info : interceptorMetaInfo.afterList()) {
            if (!applied(info)) {
                continue;
            }
            AfterInterceptor interceptor = mode.createAfterInterceptor(info, app);
            insertInterceptor(afterInterceptors, interceptor);
            sessionFree = sessionFree && interceptor.sessionFree();
            express = express && interceptor.express();
        }
        for (CatchMethodMetaInfo info : interceptorMetaInfo.catchList()) {
            if (!applied(info)) {
                continue;
            }
            ExceptionInterceptor interceptor = mode.createExceptionInterceptor(info, app);
            insertInterceptor(exceptionInterceptors, interceptor);
            sessionFree = sessionFree && interceptor.sessionFree();
            express = express && interceptor.express();
        }
        Collections.sort(exceptionInterceptors);

        for (InterceptorMethodMetaInfo info : interceptorMetaInfo.finallyList()) {
            if (!applied(info)) {
                continue;
            }
            FinallyInterceptor interceptor = mode.createFinallyInterceptor(info, app);
            insertInterceptor(finallyInterceptors, interceptor);
            sessionFree = sessionFree && interceptor.sessionFree();
            express = express && interceptor.express();
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
    public String contentSecurityPolicy() {
        ensureAgentsReady();
        return actionHandler.contentSecurityPolicy();
    }

    @Override
    public boolean disableContentSecurityPolicy() {
        ensureAgentsReady();
        return actionHandler.disableContentSecurityPolicy();
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
        return actionPath;
    }

    public static void registerGlobalInterceptor(GroupInterceptorMetaInfo freeStyleInterceptor) {
        globalFreeStyleInterceptors.add(freeStyleInterceptor);
    }

    public static void registerGlobalInterceptor(InterceptorMethodMetaInfo interceptor, InterceptorType type) {
        globalFreeStyleInterceptor.add(interceptor, type);
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

    @AnnotatedClassFinder(value = Global.class, callOn = SysEventId.PRE_START)
    @SuppressWarnings("unused")
    public static void registerGlobalInterceptors(Class<?> interceptorClass) {
        App app = Act.app();
        if (BeforeInterceptor.class.isAssignableFrom(interceptorClass)) {
            BeforeInterceptor interceptor = (BeforeInterceptor) app.getInstance(interceptorClass);
            registerGlobalInterceptor(interceptor);
        } else if (AfterInterceptor.class.isAssignableFrom(interceptorClass)) {
            AfterInterceptor interceptor = (AfterInterceptor) app.getInstance(interceptorClass);
            registerGlobalInterceptor(interceptor);
        } else if (ExceptionInterceptor.class.isAssignableFrom(interceptorClass)) {
            ExceptionInterceptor interceptor = (ExceptionInterceptor) app.getInstance(interceptorClass);
            registerGlobalInterceptor(interceptor);
        } else if (FinallyInterceptor.class.isAssignableFrom(interceptorClass)) {
            FinallyInterceptor interceptor = (FinallyInterceptor) app.getInstance(interceptorClass);
            registerGlobalInterceptor(interceptor);
        } else {
            // check if this is a free style interceptor
            ControllerClassMetaInfo metaInfo = app.classLoader().controllerClassMetaInfo(interceptorClass.getName());
            if (null != metaInfo) {
                registerGlobalInterceptor(metaInfo.interceptors());
            }
        }
    }

    public static <T extends Handler> void insertInterceptor(List<T> list, T i) {
        if (list.contains(i)) {
            return;
        }
        list.add(i);
        Collections.sort(list, Sorter.COMPARATOR);
    }

    public static class GroupInterceptorWithResult {
        private List<? extends ActionHandler> interceptors;

        public GroupInterceptorWithResult(List<? extends ActionHandler> interceptors) {
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
        private List<? extends AfterInterceptor> interceptors;

        public GroupAfterInterceptor(List<? extends AfterInterceptor> interceptors) {
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
        private List<? extends FinallyInterceptor> interceptors;

        public GroupFinallyInterceptor(List<FinallyInterceptor> interceptors) {
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
        private List<? extends ExceptionInterceptor> interceptors;

        public GroupExceptionInterceptor(List<? extends ExceptionInterceptor> interceptors) {
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
