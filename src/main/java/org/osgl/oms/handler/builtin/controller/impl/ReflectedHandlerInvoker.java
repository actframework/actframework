package org.osgl.oms.handler.builtin.controller.impl;

import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl._;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.util.StringValueResolver;
import org.osgl.oms.app.App;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.controller.meta.*;
import org.osgl.oms.handler.builtin.controller.*;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;
import static org.osgl.oms.controller.Controller.Util.*;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Implement handler using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedHandlerInvoker<M extends HandlerMethodMetaInfo>
        implements ActionHandlerInvoker, AfterInterceptorInvoker, ExceptionInterceptorInvoker {

    private static _.Visitor<AppContext> STORE_APPCTX_TO_THREAD_LOCAL = new _.Visitor<AppContext>() {
        @Override
        public void visit(AppContext appContext) throws _.Break {
            appContext.saveLocal();
        }
    };

    private static Map<String, _.F2<AppContext, Object, ?>> fieldName_appCtxHandler_lookup = C.newMap();
    private ClassLoader cl;
    private ControllerClassMetaInfo controller;
    private Class<?> controllerClass;
    protected ConstructorAccess<?> constructorAccess;
    protected MethodAccess methodAccess;
    private M handler;
    protected int handlerIndex;
    private AppContextInjection ctxInjection;
    private Class[] paramTypes;
    protected Method method; //
    protected _.F2<AppContext, Object, ?> fieldAppCtxHandler;

    protected ReflectedHandlerInvoker(M handlerMetaInfo, App app) {
        this.cl = app.classLoader();
        this.handler = handlerMetaInfo;
        this.controller = handlerMetaInfo.classInfo();
        controllerClass = _.classForName(controller.className(), cl);

        this.ctxInjection = handlerMetaInfo.appContextInjection();
        if (ctxInjection.injectVia().isField()) {
            AppContextInjection.FieldAppContextInjection faci = (AppContextInjection.FieldAppContextInjection) ctxInjection;
            fieldAppCtxHandler = storeAppCtxToCtrlrField(faci.fieldName(), controllerClass);
        }

        paramTypes = paramTypes();
        if (!handlerMetaInfo.isStatic()) {
            constructorAccess = ConstructorAccess.get(controllerClass);
            methodAccess = MethodAccess.get(controllerClass);
            handlerIndex = methodAccess.getIndex(handlerMetaInfo.name(), paramTypes);
        } else {
            try {
                method = controllerClass.getMethod(handlerMetaInfo.name(), paramTypes);
            } catch (NoSuchMethodException e) {
                throw E.unexpected(e);
            }
            method.setAccessible(true);
        }
    }

    @Override
    public int priority() {
        return handler.priority();
    }

    public Result handle(AppContext appContext) {
        Object ctrl = controllerInstance(appContext);
        applyAppContext(appContext, ctrl);
        Object[] params = params(appContext, null, null);
        return invoke(appContext, ctrl, params);
    }

    @Override
    public Result handle(Result result, AppContext appContext) {
        Object ctrl = controllerInstance(appContext);
        applyAppContext(appContext, ctrl);
        Object[] params = params(appContext, result, null);
        return invoke(appContext, ctrl, params);
    }

    @Override
    public Result handle(Exception e, AppContext appContext) {
        Object ctrl = controllerInstance(appContext);
        applyAppContext(appContext, ctrl);
        Object[] params = params(appContext, null, e);
        return invoke(appContext, ctrl, params);
    }

    private Object controllerInstance(AppContext context) {
        if (null == constructorAccess) {
            return null;
        }
        String controller = controllerClass.getName();
        Object inst = context.__controllerInstance(controller);
        if (null == inst) {
            inst = constructorAccess.newInstance();
            context.__controllerInstance(controller, inst);
        }
        return inst;
    }

    private void applyAppContext(AppContext appContext, Object controller) {
        if (null != fieldAppCtxHandler) {
            fieldAppCtxHandler.apply(appContext, controller);
        }
        // ignore ContextLocal save as it's processed for one time when RequestHandlerProxy is invoked
    }

    private Result invoke(AppContext context, Object controller, Object[] params) {
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
        return inferResult(result, context);
    }

    private Object[] params(AppContext ctx, Result result, Exception exception) {
        int paramCount = handler.paramCount();
        Object[] oa = new Object[paramCount];
        if (0 == paramCount) {
            return oa;
        }
        for (int i = 0; i < paramCount; ++i) {
            ParamMetaInfo param = handler.param(i);
            Class<?> paramType = paramTypes[i];
            if (AppContext.class.equals(paramType)) {
                oa[i] = ctx;
            } else if (Result.class.isAssignableFrom(paramType)) {
                oa[i] = result;
            } else if (Exception.class.isAssignableFrom(paramType)) {
                oa[i] = exception;
            } else {
                String paramName = param.name();
                String reqVal = ctx.param(paramName);
                oa[i] = StringValueResolver.predefined(paramType).apply(reqVal);
            }
        }
        return oa;
    }

    private Class<?>[] paramTypes() {
        int paramCount = handler.paramCount();
        Class<?>[] ca = new Class[paramCount];
        if (0 == paramCount) {
            return ca;
        }
        for (int i = 0; i < paramCount; ++i) {
            ParamMetaInfo param = handler.param(i);
            String className = param.type().getClassName();
            ca[i] = _.classForName(className, cl);
        }
        return ca;
    }

    private static _.F2<AppContext, Object, ?> storeAppCtxToCtrlrField(final String fieldName, final Class<?> controllerClass) {
        String key = S.builder(controllerClass.getName()).append(".").append(fieldName).toString();
        _.F2<AppContext, Object, ?> ctxHandler = fieldName_appCtxHandler_lookup.get(key);
        if (null == ctxHandler) {
            ctxHandler = new _.F2<AppContext, Object, Void>() {
                private final FieldAccess fieldAccess = FieldAccess.get(controllerClass);
                private final int fieldIdx = getFieldIndex(fieldName, fieldAccess);

                @Override
                public Void apply(AppContext appContext, Object controllerInstance) throws _.Break {
                    fieldAccess.set(controllerInstance, fieldIdx, appContext);
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

    private static class _Before extends BeforeInterceptor {
        private ActionHandlerInvoker invoker;

        _Before(ActionHandlerInvoker invoker) {
            super(invoker.priority());
            this.invoker = invoker;
        }

        @Override
        public Result handle(AppContext appContext) {
            return invoker.handle(appContext);
        }
    }

    private static class _After extends AfterInterceptor {
        private AfterInterceptorInvoker invoker;

        _After(AfterInterceptorInvoker invoker) {
            super(invoker.priority());
            this.invoker = invoker;
        }

        @Override
        public Result handle(Result result, AppContext appContext) {
            return invoker.handle(result, appContext);
        }
    }

    private static class _Exception extends ExceptionInterceptor {
        private ExceptionInterceptorInvoker invoker;

        _Exception(ExceptionInterceptorInvoker invoker) {
            super(invoker.priority());
            this.invoker = invoker;
        }

        @Override
        public Result handle(Exception e, AppContext appContext) {
            return invoker.handle(e, appContext);
        }
    }

    private static class _Finally extends FinallyInterceptor {
        private ActionHandlerInvoker invoker;

        _Finally(ActionHandlerInvoker invoker) {
            super(invoker.priority());
            this.invoker = invoker;
        }

        @Override
        public void handle(AppContext appContext) {
            invoker.handle(appContext);
        }
    }

    private class _ExceptionHandlerInvoker extends ReflectedHandlerInvoker<CatchMethodMetaInfo> {
        protected _ExceptionHandlerInvoker(CatchMethodMetaInfo handlerMetaInfo, App app) {
            super(handlerMetaInfo, app);
        }
    }

}
