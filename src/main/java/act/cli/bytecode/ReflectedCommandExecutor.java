package act.cli.bytecode;

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
import act.cli.CliContext;
import act.cli.CommandExecutor;
import act.cli.ReportProgress;
import act.cli.meta.CommandMethodMetaInfo;
import act.cli.meta.CommandParamMetaInfo;
import act.inject.param.CliContextParamLoader;
import act.inject.param.ParamValueLoaderManager;
import act.job.AppJobManager;
import act.job.TrackableWorker;
import act.util.Async;
import act.util.ProgressGauge;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.util.E;

import java.lang.reflect.Method;

/**
 * Implement {@link act.cli.CommandExecutor} using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedCommandExecutor extends CommandExecutor {

    private static final Object[] DUMP_PARAMS = new Object[0];

    private CommandMethodMetaInfo methodMetaInfo;
    private App app;
    private CliContextParamLoader paramLoaderService;
    private ClassLoader cl;
    private Class[] paramTypes;
    private Class<?> commanderClass;
    private Method method;
    private MethodAccess methodAccess;
    private int commandIndex;
    private int paramCount;
    private CliContext.ParsingContext parsingContext;
    private boolean async;
    private ReportProgress reportProgress;

    public ReflectedCommandExecutor(CommandMethodMetaInfo methodMetaInfo, App app) {
        this.methodMetaInfo = $.notNull(methodMetaInfo);
        this.app = $.NPE(app);
        this.cl = app.classLoader();
        this.paramTypes = paramTypes();
        this.paramCount = methodMetaInfo.paramCount();
        this.commanderClass = $.classForName(methodMetaInfo.classInfo().className(), cl);
        try {
            this.method = commanderClass.getMethod(methodMetaInfo.methodName(), paramTypes);
            this.async = null != method.getAnnotation(Async.class);
            this.reportProgress = method.getAnnotation(ReportProgress.class);
        } catch (NoSuchMethodException e) {
            throw E.unexpected(e);
        }
        if (!methodMetaInfo.isStatic()) {
            methodAccess = MethodAccess.get(commanderClass);
            commandIndex = methodAccess.getIndex(methodMetaInfo.methodName(), paramTypes);
        } else {
            method.setAccessible(true);
        }
        this.paramLoaderService = app.service(ParamValueLoaderManager.class).get(CliContext.class);
        this.buildParsingContext();
    }

    @Override
    public Object execute(CliContext context) {
        context.attribute(CliContext.ATTR_METHOD, method);
        context.prepare(parsingContext);
        paramLoaderService.preParseOptions(method, methodMetaInfo, context);
        final Object cmd = commanderInstance(context);
        final Object[] params = params(cmd, context);
        if (async) {
            AppJobManager jobManager = context.app().jobManager();
            String jobId = jobManager.prepare(new TrackableWorker() {
                @Override
                protected void run(ProgressGauge progressGauge) {
                    invoke(cmd, params);
                }
            });
            context.setJobId(jobId);
            jobManager.now(jobId);
            if (null != reportProgress) {
                context.attribute(ReportProgress.CTX_ATTR_KEY, reportProgress);
                return context.progress();
            } else {
                return "Async job started: " + jobId;
            }
        }
        return invoke(cmd, params);
    }

    @Override
    protected void releaseResources() {
        app = null;
        cl = null;
        commandIndex = 0;
        commanderClass = null;
        method = null;
        methodAccess = null;
        paramTypes = null;
        super.releaseResources();
    }

    private Object commanderInstance(CliContext context) {
        String commander = commanderClass.getName();
        Object inst = context.__commanderInstance(commander);
        if (null == inst) {
            inst = paramLoaderService.loadHostBean(commanderClass, context);
            context.__commanderInstance(commander, inst);
        }
        return inst;
    }

    private Class<?>[] paramTypes() {
        int paramCount = methodMetaInfo.paramCount();
        Class<?>[] ca = new Class[paramCount];
        if (0 == paramCount) {
            return ca;
        }
        for (int i = 0; i < paramCount; ++i) {
            CommandParamMetaInfo param = methodMetaInfo.param(i);
            String className = param.type().getClassName();
            ca[i] = $.classForName(className, cl);
        }
        return ca;
    }

    private Object[] params(Object cmd, CliContext ctx) {
        if (0 == paramCount) {
            return DUMP_PARAMS;
        }
        return paramLoaderService.loadMethodParams(cmd, method, ctx);
    }

    private Object invoke(Object commander, Object[] params) {
        Object result;
        if (null != methodAccess) {
            result = methodAccess.invoke(commander, commandIndex, params);
        } else {
            result = $.invokeStatic(method, params);
        }
        return result;
    }

    private void buildParsingContext() {
        CliContextParamLoader loader = (CliContextParamLoader) app.service(ParamValueLoaderManager.class).get(CliContext.class);
        this.parsingContext = loader.buildParsingContext(commanderClass, method, methodMetaInfo);
    }

}
