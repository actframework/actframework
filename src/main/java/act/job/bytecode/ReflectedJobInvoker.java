package act.job.bytecode;

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

import act.app.App;
import act.inject.param.ParamValueLoaderManager;
import act.inject.param.ParamValueLoaderService;
import act.job.JobContext;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobMethodMetaInfo;
import act.sys.Env;
import act.util.ReflectedInvokerHelper;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.inject.BeanSpec;
import org.osgl.util.E;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Implement handler using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedJobInvoker<M extends JobMethodMetaInfo> extends $.F0<Object> {

    private App app;
    private JobClassMetaInfo classInfo;
    private volatile Class<?> jobClass;
    private MethodAccess methodAccess;
    private M methodInfo;
    private int methodIndex;
    protected Method method;
    private boolean disabled;
    private ParamValueLoaderService paramValueLoaderService;
    private Object singleton;
    private boolean isStatic;

    public ReflectedJobInvoker(M handlerMetaInfo, App app) {
        this.methodInfo = handlerMetaInfo;
        this.classInfo = handlerMetaInfo.classInfo();
        this.app = app;
        this.isStatic = handlerMetaInfo.isStatic();
    }

    private synchronized void init() {
        if (null != jobClass) {
            return;
        }
        disabled = false;
        jobClass = app.classForName(classInfo.className());
        disabled = disabled || !Env.matches(jobClass);
        method = methodInfo.method();
        disabled = disabled || !Env.matches(method);
        if (disabled) {
            return;
        }
        isStatic = methodInfo.isStatic();
        if (!isStatic) {
            singleton = ReflectedInvokerHelper.tryGetSingleton(jobClass, app);
        }
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
        JobContext ctx = JobContext.current();
        ctx.handlerMethod(method);
        Object job = jobClassInstance(app, ctx);
        return invoke(job, ctx);
    }

    public Method method() {
        return method;
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


    private Object jobClassInstance(App app, JobContext ctx) {
        if (isStatic) {
            return null;
        }
        if (null != singleton) {
            return singleton;
        }
        return null != paramValueLoaderService ?
                paramValueLoaderService.loadHostBean(jobClass, ctx)
                : app.getInstance(jobClass);
    }

    private Object invoke(Object jobClassInstance, JobContext ctx) {
        Object[] params = params(jobClassInstance, ctx);
        return null == methodAccess ? $.invokeStatic(method, params) : methodAccess.invoke(jobClassInstance, methodIndex, params);
    }

    private Object[] params(Object job, JobContext ctx) {
        if (null != paramValueLoaderService) {
            return paramValueLoaderService.loadMethodParams(job, method, ctx);
        }
        E.illegalStateIf(paramTypes().length > 0, "Cannot invoke job with parameters before app fully started");
        return new Object[0];
    }

}
