package act.job.bytecode;

import act.app.App;
import act.inject.DependencyInjector;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobMethodMetaInfo;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.inject.BeanSpec;
import org.osgl.util.C;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
    protected Method method;
    private List<BeanSpec> providedParams;
    private int providedParamsSize;

    public ReflectedJobInvoker(M handlerMetaInfo, App app) {
        this.cl = app.classLoader();
        this.methodInfo = handlerMetaInfo;
        this.classInfo = handlerMetaInfo.classInfo();
        this.app = app;
    }

    private void init() {
        jobClass = $.classForName(classInfo.className(), cl);
        method = methodInfo.method();
        providedParams = methodInfo.paramTypes();
        providedParamsSize = providedParams.size();
    }

    @Override
    public Object apply() throws NotAppliedException, $.Break {
        if (null == jobClass) {
            init();
        }
        Object job = jobClassInstance(app);
        return invoke(job);
    }

    private Object jobClassInstance(App app) {
        return app.getInstance(jobClass);
    }

    private Object invoke(Object jobClassInstance) {
        Object[] params = new Object[providedParamsSize];
        for (int i = 0; i < providedParamsSize; ++i) {
            params[i] = app.getInstance(providedParams.get(i).rawType());
        }
        Object result;
        if (null != methodAccess) {
            result = methodAccess.invoke(jobClassInstance, methodIndex, params);
        } else {
            try {
                result = method.invoke(jobClassInstance, params);
            } catch (InvocationTargetException e) {
                throw E.unexpected(e.getCause());
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }
        return result;
    }
}
