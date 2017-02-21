package act.job.bytecode;

import act.app.App;
import act.controller.meta.HandlerParamMetaInfo;
import act.inject.param.ParamValueLoaderManager;
import act.inject.param.ParamValueLoaderService;
import act.job.JobContext;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobMethodMetaInfo;
import act.sys.Env;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.inject.BeanSpec;
import org.osgl.util.E;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
    private boolean disabled;
    private ParamValueLoaderService paramValueLoaderService;

    public ReflectedJobInvoker(M handlerMetaInfo, App app) {
        this.cl = app.classLoader();
        this.methodInfo = handlerMetaInfo;
        this.classInfo = handlerMetaInfo.classInfo();
        this.app = app;
    }

    private void init() {
        disabled = false;
        jobClass = $.classForName(classInfo.className(), cl);
        disabled = disabled || !Env.matches(jobClass);
        method = methodInfo.method();
        disabled = disabled || !Env.matches(jobClass);
        providedParams = methodInfo.paramTypes();
        ParamValueLoaderManager paramValueLoaderManager = app.service(ParamValueLoaderManager.class);
        if (null != paramValueLoaderManager) {
            paramValueLoaderService = paramValueLoaderManager.get(JobContext.class);
        } else {
            // this job is scheduled to run before ParamValueLoaderManager initialized
        }

        if (!Modifier.isStatic(method.getModifiers())) {
            Class[] paramTypes = paramTypes();
            //constructorAccess = ConstructorAccess.get(controllerClass);
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
        if (disabled) {
            return null;
        }
        Object job = jobClassInstance(app);
        return invoke(job);
    }

    private Class[] paramTypes() {
        List<BeanSpec> paramTypes = methodInfo.paramTypes();
        int sz = null == paramTypes ? 0 : paramTypes.size();
        Class[] ca = new Class[sz];
        for (int i = 0; i < sz; ++i) {
            BeanSpec spec = methodInfo.paramTypes().get(i);
            ca[i] = spec.rawType();
        }
        return ca;
    }


    private Object jobClassInstance(App app) {
        return null != paramValueLoaderService ? paramValueLoaderService.loadHostBean(jobClass, JobContext.current())
                : app.getInstance(jobClass);
    }

    private Object invoke(Object jobClassInstance) {
        Object[] params = params();
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

    private Object[] params() {
        if (null != paramValueLoaderService) {
            return paramValueLoaderService.loadMethodParams(method, JobContext.current());
        }
        E.illegalStateIf(paramTypes().length > 0, "Cannot invoke job with parameters before app fully started");
        return new Object[0];
    }
}
