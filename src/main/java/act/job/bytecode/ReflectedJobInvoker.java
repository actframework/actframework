package act.job.bytecode;

import act.app.App;
import act.conf.AppConfig;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobMethodMetaInfo;
import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Implement handler using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedJobInvoker<M extends JobMethodMetaInfo> extends $.F0<Object> {

    private App app;
    private ClassLoader cl;
    private JobClassMetaInfo classInfo;
    private Class<?> jobClass;
    private MethodAccess methodAccess;
    private M methodInfo;
    private int methodIndex;
    protected Method method; //

    public ReflectedJobInvoker(M handlerMetaInfo, App app) {
        this.cl = app.classLoader();
        this.methodInfo = handlerMetaInfo;
        this.classInfo = handlerMetaInfo.classInfo();
        this.app = app;
    }

    private void init() {
        jobClass = $.classForName(classInfo.className(), cl);

        Class<?>[] paramTypes = new Class[0];
        try {
            method = jobClass.getMethod(methodInfo.name(), paramTypes);
        } catch (NoSuchMethodException e) {
            throw E.unexpected(e);
        }

        if (!methodInfo.isStatic()) {
            methodAccess = MethodAccess.get(jobClass);
            methodIndex = methodAccess.getIndex(methodInfo.name(), paramTypes);
        } else {
            method.setAccessible(true);
        }
    }

    @Override
    public Object apply() throws NotAppliedException, $.Break {
        if (null == jobClass) {
            init();
        }
        Object job = jobClassInstance(app);
        Object[] params = new Object[0];
        return invoke(job, params);
    }

    private Object jobClassInstance(App app) {
        if (null == methodAccess) {
            return null;
        }
        return app.getInstance(jobClass);
    }

    private Object invoke(Object jobClassInstance, Object[] params) {
        Object result;
        if (null != methodAccess) {
            result = methodAccess.invoke(jobClassInstance, methodIndex, params);
        } else {
            try {
                result = method.invoke(null, params);
            } catch (InvocationTargetException e) {
                throw E.unexpected(e.getCause());
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }
        return result;
    }
}
