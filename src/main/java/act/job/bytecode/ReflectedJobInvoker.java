package act.job.bytecode;

import act.app.App;
import act.conf.AppConfig;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobMethodMetaInfo;
import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Implement handler using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedJobInvoker<M extends JobMethodMetaInfo> extends $.F0<Object> {

    private static Map<String, $.F2<App, Object, ?>> fieldName_appHandler_lookup = C.newMap();
    private static Map<String, $.F2<AppConfig, Object, ?>> fieldName_appConfigHandler_lookup = C.newMap();
    private App app;
    private ClassLoader cl;
    private JobClassMetaInfo classInfo;
    private Class<?> jobClass;
    protected MethodAccess methodAccess;
    private M methodInfo;
    protected int methodIndex;
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
        if (!methodInfo.isStatic()) {
            methodAccess = MethodAccess.get(jobClass);
            methodIndex = methodAccess.getIndex(methodInfo.name(), paramTypes);
        } else {
            try {
                method = jobClass.getMethod(methodInfo.name(), paramTypes);
            } catch (NoSuchMethodException e) {
                throw E.unexpected(e);
            }
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
        return app.newInstance(jobClass);
    }

    private Object invoke(Object jobClassInstance, Object[] params) {
        Object result;
        if (null != methodAccess) {
            result = methodAccess.invoke(jobClassInstance, methodIndex, params);
        } else {
            try {
                result = method.invoke(null, params);
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }
        return result;
    }

    private static <T> $.F2<T, Object, ?> injectField(final String fieldName, final Class<?> controllerClass, final Map<String, $.F2<T, Object, ?>> cache) {
        if (S.blank(fieldName)) return null;
        String key = S.builder(controllerClass.getName()).append(".").append(fieldName).toString();
        $.F2<T, Object, ?> injector = cache.get(key);
        if (null == injector) {
            injector = new $.F2<T, Object, Void>() {
                private FieldAccess fieldAccess = FieldAccess.get(controllerClass);
                private int fieldIdx = getFieldIndex(fieldName, fieldAccess);

                @Override
                public Void apply(T injectTarget, Object controllerInstance) throws $.Break {
                    fieldAccess.set(controllerInstance, fieldIdx, injectTarget);
                    return null;
                }

                private int getFieldIndex(String fieldName, FieldAccess fieldAccess) {
                    return fieldAccess.getIndex(fieldName);
                }
            };
            cache.put(key, injector);
        }
        return injector;
    }
}
