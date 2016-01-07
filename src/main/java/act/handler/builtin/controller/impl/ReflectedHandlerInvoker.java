package act.handler.builtin.controller.impl;

import act.Act;
import act.ActComponent;
import act.app.ActionContext;
import act.app.App;
import act.app.data.BinderManager;
import act.app.data.StringValueResolverManager;
import act.asm.Type;
import act.controller.ActionMethodParamAnnotationHandler;
import act.controller.Controller;
import act.controller.meta.*;
import act.exception.BindException;
import act.handler.builtin.controller.*;
import act.util.DestroyableBase;
import act.util.GeneralAnnoInfo;
import act.view.Template;
import act.view.TemplatePathResolver;
import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.StringValueResolver;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implement handler using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedHandlerInvoker<M extends HandlerMethodMetaInfo> extends DestroyableBase
        implements ActionHandlerInvoker, AfterInterceptorInvoker, ExceptionInterceptorInvoker {

    private static $.Visitor<ActionContext> STORE_APPCTX_TO_THREAD_LOCAL = new $.Visitor<ActionContext>() {
        @Override
        public void visit(ActionContext actionContext) throws $.Break {
            //actionContext.saveLocal();
        }
    };

    private static Map<String, $.F2<ActionContext, Object, ?>> fieldName_appCtxHandler_lookup = C.newMap();
    private App app;
    private ClassLoader cl;
    private ControllerClassMetaInfo controller;
    private Class<?> controllerClass;
    protected MethodAccess methodAccess;
    private M handler;
    protected int handlerIndex;
    private ActContextInjection ctxInjection;
    private Map<H.Format, Boolean> templateCache = C.newMap();
    private Class[] paramTypes;
    private Class[] paramComponentTypes; // in case there are generic container type in paramTypes
    private Map<Integer, List<ActionMethodParamAnnotationHandler>> paramAnnoHandlers = null;
    protected Method method; //
    protected $.F2<ActionContext, Object, ?> fieldAppCtxHandler;

    protected ReflectedHandlerInvoker(M handlerMetaInfo, App app) {
        this.app = app;
        this.cl = app.classLoader();
        this.handler = handlerMetaInfo;
        this.controller = handlerMetaInfo.classInfo();
        controllerClass = $.classForName(controller.className(), cl);

        this.ctxInjection = handlerMetaInfo.appContextInjection();
        if (ctxInjection.injectVia().isField()) {
            ActContextInjection.FieldActContextInjection faci = (ActContextInjection.FieldActContextInjection) ctxInjection;
            fieldAppCtxHandler = storeAppCtxToCtrlrField(faci.fieldName(), controllerClass);
        }

        $.T2<Class[], Class[]> t2 = paramTypes();
        paramTypes = t2._1;
        paramComponentTypes = t2._2;
        try {
            method = controllerClass.getMethod(handlerMetaInfo.name(), paramTypes);
        } catch (NoSuchMethodException e) {
            throw E.unexpected(e);
        }
        if (!handlerMetaInfo.isStatic()) {
            //constructorAccess = ConstructorAccess.get(controllerClass);
            methodAccess = MethodAccess.get(controllerClass);
            handlerIndex = methodAccess.getIndex(handlerMetaInfo.name(), paramTypes);
        } else {
            method.setAccessible(true);
        }
        paramAnnoHandlers = C.newMap();
        if (paramTypes.length > 0) {
            ClassLoader classLoader = cl;
            List<ActionMethodParamAnnotationHandler> availableHandlers = Act.pluginManager().pluginList(ActionMethodParamAnnotationHandler.class);
            for (ActionMethodParamAnnotationHandler annotationHandler: availableHandlers) {
                Set<Class<? extends Annotation>> listenTo = annotationHandler.listenTo();
                for (int i = 0,j = paramTypes.length; i < j; ++i) {
                    ParamMetaInfo paramMetaInfo = handlerMetaInfo.param(i);
                    List<GeneralAnnoInfo> annoInfoList = paramMetaInfo.generalAnnoInfoList();
                    for (GeneralAnnoInfo annoInfo : annoInfoList) {
                        Class<? extends Annotation> annoClass = annoInfo.annotationClass(classLoader);
                        if (listenTo.contains(annoClass)) {
                            List<ActionMethodParamAnnotationHandler> handlerList = paramAnnoHandlers.get(i);
                            if (null == handlerList) {
                                handlerList = C.newList();
                                paramAnnoHandlers.put(i, handlerList);
                            }
                            handlerList.add(annotationHandler);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void releaseResources() {
        app = null;
        cl = null;
        controller = null;
        controllerClass = null;
        method = null;
        methodAccess = null;
        handler.destroy();
        handler = null;
        paramTypes = null;
        fieldAppCtxHandler = null;
        fieldName_appCtxHandler_lookup.clear();
        ctxInjection = null;
        super.releaseResources();
    }

    @Override
    public int priority() {
        return handler.priority();
    }

    public interface ReflectedHandlerInvokerVisitor extends Visitor, $.Func2<Class<?>, Method, Void> {}

    @Override
    public void accept(Visitor visitor) {
        ReflectedHandlerInvokerVisitor rv = (ReflectedHandlerInvokerVisitor)visitor;
        rv.apply(controllerClass, method);
    }

    public Result handle(ActionContext actionContext) {
        Object ctrl = controllerInstance(actionContext);
        applyAppContext(actionContext, ctrl);
        Object[] params = params(actionContext, null, null);
        return invoke(handler, actionContext, ctrl, params);
    }

    @Override
    public Result handle(Result result, ActionContext actionContext) {
        Object ctrl = controllerInstance(actionContext);
        applyAppContext(actionContext, ctrl);
        Object[] params = params(actionContext, result, null);
        return invoke(handler, actionContext, ctrl, params);
    }

    @Override
    public Result handle(Exception e, ActionContext actionContext) {
        Object ctrl = controllerInstance(actionContext);
        applyAppContext(actionContext, ctrl);
        Object[] params = params(actionContext, null, e);
        return invoke(handler, actionContext, ctrl, params);
    }

    private Object controllerInstance(ActionContext context) {
        String controller = controllerClass.getName();
        Object inst = context.__controllerInstance(controller);
        if (null == inst) {
            inst = context.newInstance(controllerClass);
            context.__controllerInstance(controller, inst);
        }
        return inst;
    }

    private void applyAppContext(ActionContext actionContext, Object controller) {
        if (null != fieldAppCtxHandler) {
            fieldAppCtxHandler.apply(actionContext, controller);
        }
        // ignore ContextLocal save as it's processed for one time when RequestHandlerProxy is invoked
    }

    private Result invoke(M handlerMetaInfo, ActionContext context, Object controller, Object[] params) {
        Object result;
        if (null != methodAccess) {
            try {
                result = methodAccess.invoke(controller, handlerIndex, params);
            } catch (Result r) {
                return r;
            }
        } else {
            try {
                result = method.invoke(null, params);
            } catch (Result r) {
                return r;
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }
        boolean hasTemplate = checkTemplate(context);
        return Controller.Util.inferResult(handlerMetaInfo, result, context, hasTemplate);
    }

    private synchronized boolean checkTemplate(ActionContext context) {
        H.Format fmt = context.accept();
        Boolean B = templateCache.get(fmt);
        if (null == B || Act.isDev()) {
            if (!TemplatePathResolver.isAcceptFormatSupported(fmt)) {
                B = false;
            } else {
                Template t = Act.viewManager().load(context);
                B = t != null;
            }
            templateCache.put(fmt, B);
        }
        return B;
    }

    private Object[] params(ActionContext ctx, Result result, Exception exception) {
        int paramCount = handler.paramCount();
        Object[] oa = new Object[paramCount];
        if (0 == paramCount) {
            return oa;
        }
        StringValueResolverManager resolverManager = app.resolverManager();
        BinderManager binderManager = app.binderManager();

        for (int i = 0; i < paramCount; ++i) {
            ParamMetaInfo param = handler.param(i);
            Class<?> paramType = paramTypes[i];
            Class<?> paramComponentType = paramComponentTypes[i];
            if (ActionContext.class.equals(paramType)) {
                oa[i] = ctx;
            } else if (Result.class.isAssignableFrom(paramType)) {
                oa[i] = result;
            } else if (Exception.class.isAssignableFrom(paramType)) {
                oa[i] = exception;
            } else {
                try {
                    BindAnnoInfo bindInfo = param.bindAnnoInfo();
                    Binder<?> binder = null;
                    String bindName = param.bindName();
                    if (null != bindInfo) {
                        binder = bindInfo.binder(ctx);
                        bindName = bindInfo.model();
                    } else {
                        Type type = param.type();
                        if (null != type) {
                            binder = binderManager.binder(paramType, $.classForName(type.getClassName(), cl), param);
                            if (null == binder) {
                                binder = binderManager.binder(param);
                            }
                        }
                    }
                    if (null != binder) {
                        oa[i] = binder.resolve(bindName, ctx);
                    } else {
                        StringValueResolver resolver = null;
                        if (param.resolverDefined()) {
                            resolver = param.resolver(app);
                        }
                        String reqVal = ctx.paramVal(bindName);
                        if (null == reqVal) {
                            Object o = ctx.tryParseJson(bindName, paramType, paramComponentType);
                            if (null != o) {
                                oa[i] = o;
                                continue;
                            }
                            o = param.defVal(paramType);
                            if (null != o) {
                                oa[i] = o;
                                continue;
                            }
                        }
                        if (null == resolver) {
                            oa[i] = resolverManager.resolve(reqVal, paramType);
                        } else {
                            oa[i] = resolver.resolve(reqVal);
                        }
                        List<ActionMethodParamAnnotationHandler> annotationHandlers = paramAnnoHandlers.get(i);
                        if (null != annotationHandlers) {
                            String paraName = param.name();
                            Object val = oa[i];
                            for (ActionMethodParamAnnotationHandler annotationHandler : annotationHandlers) {
                                for (Annotation annotation : paramAnnotationList(i)) {
                                    annotationHandler.handle(paraName, val, annotation, ctx);
                                }
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    throw new BindException(e);
                }
            }
        }
        return oa;
    }

    private List<Annotation> paramAnnotationList(int paramIndex) {
        ParamMetaInfo paramMetaInfo = handler.param(paramIndex);
        List<Annotation> retVal = C.newList();
        List<GeneralAnnoInfo> infoList = paramMetaInfo.generalAnnoInfoList();
        if (null == infoList) {
            return retVal;
        }
        for (GeneralAnnoInfo annoInfo : infoList) {
            retVal.add(annoInfo.toAnnotation());
        }
        return retVal;
    }

    private $.T2<Class[], Class[]> paramTypes() {
        int paramCount = handler.paramCount();
        Class[] ca = new Class[paramCount];
        Class[] ca2 = new Class[paramCount];
        if (0 == paramCount) {
            return $.T2(ca, ca2);
        }
        for (int i = 0; i < paramCount; ++i) {
            ParamMetaInfo param = handler.param(i);
            String className = param.type().getClassName();
            ca[i] = $.classForName(className, cl);
            Type componentType = param.componentType();
            if (null == componentType) {
                ca2[i] = null;
            } else {
                ca2[i] = $.classForName(componentType.getClassName(), cl);
            }
        }
        return $.T2(ca, ca2);
    }

    private static $.F2<ActionContext, Object, ?> storeAppCtxToCtrlrField(final String fieldName, final Class<?> controllerClass) {
        String key = S.builder(controllerClass.getName()).append(".").append(fieldName).toString();
        $.F2<ActionContext, Object, ?> ctxHandler = fieldName_appCtxHandler_lookup.get(key);
        if (null == ctxHandler) {
            ctxHandler = new $.F2<ActionContext, Object, Void>() {
                private FieldAccess fieldAccess = FieldAccess.get(controllerClass);
                private int fieldIdx = getFieldIndex(fieldName, fieldAccess);

                @Override
                public Void apply(ActionContext actionContext, Object controllerInstance) throws $.Break {
                    fieldAccess.set(controllerInstance, fieldIdx, actionContext);
                    return null;
                }

                private int getFieldIndex(String fieldName, FieldAccess fieldAccess) {
                    return fieldAccess.getIndex(fieldName);
                }
            };
            fieldName_appCtxHandler_lookup.put(key, ctxHandler);
        }
        return ctxHandler;
    }

    public static ControllerAction createControllerAction(ActionMethodMetaInfo meta, App app) {
        return new ControllerAction(new ReflectedHandlerInvoker(meta, app));
    }

    public static BeforeInterceptor createBeforeInterceptor(InterceptorMethodMetaInfo meta, App app) {
        return new _Before(new ReflectedHandlerInvoker(meta, app));
    }

    public static AfterInterceptor createAfterInterceptor(InterceptorMethodMetaInfo meta, App app) {
        return new _After(new ReflectedHandlerInvoker(meta, app));
    }

    public static ExceptionInterceptor createExceptionInterceptor(CatchMethodMetaInfo meta, App app) {
        return new _Exception(new ReflectedHandlerInvoker(meta, app));
    }

    public static FinallyInterceptor createFinannyInterceptor(InterceptorMethodMetaInfo meta, App app) {
        return new _Finally(new ReflectedHandlerInvoker(meta, app));
    }

    @ActComponent
    private static class _Before extends BeforeInterceptor {
        private ActionHandlerInvoker invoker;

        _Before(ActionHandlerInvoker invoker) {
            super(invoker.priority());
            this.invoker = invoker;
        }

        @Override
        public Result handle(ActionContext actionContext) {
            return invoker.handle(actionContext);
        }

        @Override
        public void accept(Visitor visitor) {
            invoker.accept(visitor.invokerVisitor());
        }

        @Override
        protected void releaseResources() {
            invoker.destroy();
            invoker = null;
        }
    }

    @ActComponent
    private static class _After extends AfterInterceptor {
        private AfterInterceptorInvoker invoker;

        _After(AfterInterceptorInvoker invoker) {
            super(invoker.priority());
            this.invoker = invoker;
        }

        @Override
        public Result handle(Result result, ActionContext actionContext) {
            return invoker.handle(result, actionContext);
        }

        @Override
        public void accept(Visitor visitor) {
            invoker.accept(visitor.invokerVisitor());
        }

        @Override
        public void accept(ActionHandlerInvoker.Visitor visitor) {
            invoker.accept(visitor);
        }

        @Override
        protected void releaseResources() {
            invoker.destroy();
            invoker = null;
        }
    }

    @ActComponent
    private static class _Exception extends ExceptionInterceptor {
        private ExceptionInterceptorInvoker invoker;

        _Exception(ExceptionInterceptorInvoker invoker) {
            super(invoker.priority());
            this.invoker = invoker;
        }

        @Override
        public Result handle(Exception e, ActionContext actionContext) {
            return invoker.handle(e, actionContext);
        }

        @Override
        public void accept(ActionHandlerInvoker.Visitor visitor) {
            invoker.accept(visitor);
        }

        @Override
        public void accept(Visitor visitor) {
            invoker.accept(visitor.invokerVisitor());
        }

        @Override
        protected void releaseResources() {
            invoker.destroy();
            invoker = null;
        }
    }

    @ActComponent
    private static class _Finally extends FinallyInterceptor {
        private ActionHandlerInvoker invoker;

        _Finally(ActionHandlerInvoker invoker) {
            super(invoker.priority());
            this.invoker = invoker;
        }

        @Override
        public void handle(ActionContext actionContext) {
            invoker.handle(actionContext);
        }

        @Override
        public void accept(Visitor visitor) {
            invoker.accept(visitor.invokerVisitor());
        }

        @Override
        protected void releaseResources() {
            invoker.destroy();
            invoker = null;
        }
    }

    private class _ExceptionHandlerInvoker extends ReflectedHandlerInvoker<CatchMethodMetaInfo> {
        protected _ExceptionHandlerInvoker(CatchMethodMetaInfo handlerMetaInfo, App app) {
            super(handlerMetaInfo, app);
        }
    }

}
