package act.event.bytecode;

import act.app.App;
import act.event.SimpleEventListener;
import org.osgl.$;
import org.osgl.util.E;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

class ReflectedSimpleEventListener implements SimpleEventListener {

    private transient Method method;
    private transient volatile Object host;

    private String className;
    private String methodName;
    private List<String> paramTypes;
    private boolean isStatic;

    ReflectedSimpleEventListener(String className, String methodName, List<String> paramTypes) {
        this.className = $.notNull(className);
        this.methodName = $.notNull(methodName);
        this.paramTypes = $.notNull(paramTypes);
    }

    @Override
    public void invoke(Object... args) {
        try {
            int paramNo = paramTypes.size();
            int argsNo = args.length;
            Object[] realArgs = args;
            if (paramNo != argsNo) {
                realArgs = new Object[paramNo];
                System.arraycopy(args, 0, realArgs, 0, Math.min(paramNo, argsNo));
            }
            method().invoke(host(), realArgs);
        } catch (IllegalAccessException e) {
            throw E.unexpected(e);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            throw E.unexpected(t, "Error executing event listener method %s.%s", method.getDeclaringClass().getName(), method.getName());
        }
    }


    private Object host() {
        if (isStatic) {
            return null;
        } else {
            if (null == host) {
                synchronized (this) {
                    if (null == host) {
                        App app = App.instance();
                        host = app.newInstance($.classForName(className, app.classLoader()));
                    }
                }
            }
            return host;
        }
    }

    private Method method() {
        App app = App.instance();
        ClassLoader cl = app.classLoader();
        Class hostClass = c4n(className, cl);
        int paramCnt = paramTypes.size();
        Class[] paramTypeArray = new Class[paramCnt];
        for (int i = 0; i < paramCnt; ++i) {
            paramTypeArray[i] = c4n(paramTypes.get(i), cl);
        }
        try {
            return hostClass.getDeclaredMethod(methodName, paramTypeArray);
        } catch (NoSuchMethodException e) {
            throw E.unexpected(e);
        }
    }

    private Class c4n(String className, ClassLoader cl) {
        return $.classForName(className, cl);
    }

}
