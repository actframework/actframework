package act.xio;

import act.app.ActionContext;
import act.controller.meta.ActionMethodMetaInfo;
import act.controller.meta.ControllerClassMetaInfo;
import act.controller.meta.HandlerParamMetaInfo;
import act.handler.RequestHandlerBase;
import act.sys.Env;
import act.ws.WebSocketConnectionManager;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.util.E;

import java.lang.reflect.Method;

public abstract class WebSocketConnectionHandler extends RequestHandlerBase {

    private static final Object[] DUMP_PARAMS = new Object[0];
    protected boolean disabled;
    protected ClassLoader cl;
    protected WebSocketConnectionManager connectionManager;
    protected ActionMethodMetaInfo handler;
    protected ControllerClassMetaInfo controller;
    protected Class<?> handlerClass;
    protected Method method;
    protected MethodAccess methodAccess;
    private int methodIndex;
    protected boolean isStatic;

    public WebSocketConnectionHandler(ActionMethodMetaInfo methodInfo, WebSocketConnectionManager manager) {
        this.cl = manager.app().classLoader();
        this.connectionManager = $.notNull(manager);
        this.handler = $.notNull(methodInfo);
        this.controller = handler.classInfo();
        this.handlerClass = $.classForName(controller.className(), cl);
        this.disabled = !Env.matches(handlerClass);
        Class[] paramTypes = paramTypes(cl);
        try {
            this.method = handlerClass.getMethod(methodInfo.name(), paramTypes);
            this.disabled = this.disabled || !Env.matches(method);

        } catch (NoSuchMethodException e) {
            throw E.unexpected(e);
        }

        this.isStatic = methodInfo.isStatic();
        if (!this.isStatic) {
            //constructorAccess = ConstructorAccess.get(controllerClass);
            methodAccess = MethodAccess.get(handlerClass);
            methodIndex = methodAccess.getIndex(methodInfo.name(), paramTypes);
        } else {
            method.setAccessible(true);
        }
    }

    @Override
    public void prepareAuthentication(ActionContext context) {
    }

    protected void invoke(String message, WebSocketConnection connection) {
        if (this.isStatic) {
            $.invokeStatic(method, message, connection);
        } else {
            methodAccess.invoke(controller, methodIndex, message, connection);
        }
    }

    private Class[] paramTypes(ClassLoader cl) {
        int sz = handler.paramCount();
        Class[] ca = new Class[sz];
        for (int i = 0; i < sz; ++i) {
            HandlerParamMetaInfo param = handler.param(i);
            ca[i] = $.classForName(param.type().getClassName(), cl);
        }
        return ca;
    }


}
