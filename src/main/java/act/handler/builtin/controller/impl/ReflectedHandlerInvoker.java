package act.handler.builtin.controller.impl;

import act.Act;
import act.ActComponent;
import act.app.ActionContext;
import act.app.App;
import act.app.AppClassLoader;
import act.app.data.BinderManager;
import act.app.data.StringValueResolverManager;
import act.asm.Type;
import act.controller.ActionMethodParamAnnotationHandler;
import act.controller.Controller;
import act.controller.meta.*;
import act.data.AutoBinder;
import act.inject.DependencyInjector;
import act.inject.param.ParamValueLoaderManager;
import act.exception.BindException;
import act.handler.builtin.controller.*;
import act.util.DestroyableBase;
import act.util.GeneralAnnoInfo;
import act.view.Template;
import act.view.TemplatePathResolver;
import com.alibaba.fastjson.JSON;
import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.result.BadRequest;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.util.Binder;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implement handler using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedHandlerInvoker<M extends HandlerMethodMetaInfo> extends DestroyableBase
        implements ActionHandlerInvoker, AfterInterceptorInvoker, ExceptionInterceptorInvoker {

    private static Map<String, $.F2<ActionContext, Object, ?>> fieldName_appCtxHandler_lookup = C.newMap();
    private ClassLoader cl;
    private ControllerClassMetaInfo controller;
    private Class<?> controllerClass;
    private AutoBinder autoBinder;
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
    private ParamValueLoaderManager paramValueLoaderManager;
    private DependencyInjector<?> injector;

    protected ReflectedHandlerInvoker(M handlerMetaInfo, App app) {
        this.cl = app.classLoader();
        this.handler = handlerMetaInfo;
        this.controller = handlerMetaInfo.classInfo();
        this.controllerClass = $.classForName(controller.className(), cl);
        this.paramValueLoaderManager = app.service(ParamValueLoaderManager.class);
        this.injector = app.injector();

        this.ctxInjection = handlerMetaInfo.appContextInjection();
        if (ctxInjection.injectVia().isField()) {
            ActContextInjection.FieldActContextInjection faci = (ActContextInjection.FieldActContextInjection) ctxInjection;
            fieldAppCtxHandler = storeAppCtxToCtrlrField(faci.fieldName(), controllerClass);
        }

        this.autoBinder = new AutoBinder(app);

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
            for (ActionMethodParamAnnotationHandler annotationHandler : availableHandlers) {
                Set<Class<? extends Annotation>> listenTo = annotationHandler.listenTo();
                for (int i = 0, j = paramTypes.length; i < j; ++i) {
                    HandlerParamMetaInfo paramMetaInfo = handlerMetaInfo.param(i);
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

    public interface ReflectedHandlerInvokerVisitor extends Visitor, $.Func2<Class<?>, Method, Void> {
    }

    @Override
    public void accept(Visitor visitor) {
        ReflectedHandlerInvokerVisitor rv = (ReflectedHandlerInvokerVisitor) visitor;
        rv.apply(controllerClass, method);
    }

    public Result handle(ActionContext actionContext) throws Exception {
        Object ctrl = controllerInstance(actionContext);
        applyAppContext(actionContext, ctrl);
        Object[] params = params2(actionContext);
        return invoke(handler, actionContext, ctrl, params);
    }

    @Override
    public Result handle(Result result, ActionContext actionContext) throws Exception {
        Object ctrl = controllerInstance(actionContext);
        applyAppContext(actionContext, ctrl);
        Object[] params = params2(actionContext);
        return invoke(handler, actionContext, ctrl, params);
    }

    @Override
    public Result handle(Exception e, ActionContext actionContext) throws Exception {
        Object ctrl = handler.isStatic() ? null : controllerInstance(actionContext);
        applyAppContext(actionContext, ctrl);
        Object[] params = params2(actionContext);
        return invoke(handler, actionContext, ctrl, params);
    }

    private Object controllerInstance(ActionContext context) {
        String controllerName = controllerClass.getName();
        Object inst = context.__controllerInstance(controllerName);
        if (null == inst) {
            inst = paramValueLoaderManager.loadHostBean(controllerClass, context, injector);
            context.__controllerInstance(controllerName, inst);
        }
        return inst;
    }

    private void applyAppContext(ActionContext actionContext, Object controller) {
        if (null != fieldAppCtxHandler) {
            fieldAppCtxHandler.apply(actionContext, controller);
        }
        // ignore ContextLocal save as it's processed for one time when RequestHandlerProxy is invoked
    }

    private Result invoke(M handlerMetaInfo, ActionContext context, Object controller, Object[] params) throws Exception {
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
            }
        }
        if (null == result && handler.hasReturn()) {
            return NotFound.INSTANCE;
        }
        boolean hasTemplate = checkTemplate(context);
        return Controller.Util.inferResult(handlerMetaInfo, result, context, hasTemplate);
    }

    private synchronized boolean checkTemplate(ActionContext context) {
        if (!context.state().isHandling()) {
            // we don't check template on interceptors
            return false;
        }
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

    private Object[] params2(ActionContext context) {
        return paramValueLoaderManager.loadMethodParams(method, context, injector);
    }

    private $.T2<Class[], Class[]> paramTypes() {
        int paramCount = handler.paramCount();
        Class[] ca = new Class[paramCount];
        Class[] ca2 = new Class[paramCount];
        if (0 == paramCount) {
            return $.T2(ca, ca2);
        }
        for (int i = 0; i < paramCount; ++i) {
            HandlerParamMetaInfo param = handler.param(i);
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
                private Field field = getCtxField(fieldName);

                @Override
                public Void apply(ActionContext actionContext, Object controllerInstance) throws $.Break {
                    if (fieldIdx >= 0) {
                        fieldAccess.set(controllerInstance, fieldIdx, actionContext);
                    } else {
                        try {
                            field.set(controllerInstance, actionContext);
                        } catch (IllegalAccessException e) {
                            throw E.unexpected(e);
                        }
                    }
                    return null;
                }

                private int getFieldIndex(String fieldName, FieldAccess fieldAccess) {
                    try {
                        return fieldAccess.getIndex(fieldName);
                    } catch (Exception e) {
                        return -1;
                    }
                }

                private Field getCtxField(String fieldName) {
                    if (fieldIdx < 0) {
                        Class c = controllerClass;
                        while (!Object.class.equals(c)) {
                            try {
                                Field f = c.getDeclaredField(fieldName);
                                f.setAccessible(true);
                                return f;
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                        throw E.unexpected("Cannot find field %s in controller class %s", fieldName, controllerClass);
                    }
                    return null;
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
        return new _Exception(new ReflectedHandlerInvoker(meta, app), meta);
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
        public Result handle(ActionContext actionContext) throws Exception {
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
        public Result handle(Result result, ActionContext actionContext) throws Exception {
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

        _Exception(ExceptionInterceptorInvoker invoker, CatchMethodMetaInfo metaInfo) {
            super(invoker.priority(), exceptionClassesOf(metaInfo));
            this.invoker = invoker;
        }

        @SuppressWarnings("unchecked")
        private static List<Class<? extends Exception>> exceptionClassesOf(CatchMethodMetaInfo metaInfo) {
            List<String> classNames = metaInfo.exceptionClasses();
            List<Class<? extends Exception>> clsList = C.newSizedList(classNames.size());
            clsList = C.newSizedList(classNames.size());
            AppClassLoader cl = App.instance().classLoader();
            for (String cn : classNames) {
                clsList.add((Class) $.classForName(cn, cl));
            }
            return clsList;
        }

        @Override
        protected Result internalHandle(Exception e, ActionContext actionContext) throws Exception {
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
        public void handle(ActionContext actionContext) throws Exception {
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

}
