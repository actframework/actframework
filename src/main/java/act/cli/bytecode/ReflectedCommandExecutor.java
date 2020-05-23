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
import act.data.annotation.DateFormatPattern;
import act.data.annotation.Pattern;
import act.inject.param.CliContextParamLoader;
import act.inject.param.ParamValueLoaderManager;
import act.job.JobManager;
import act.job.TrackableWorker;
import act.util.*;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
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
    private Class[] paramTypes;
    private Class<?> commanderClass;
    private Method method;
    private MethodAccess methodAccess;
    private int commandIndex;
    private int paramCount;
    private boolean isStatic;
    private CliContext.ParsingContext parsingContext;
    private boolean async;
    private ReportProgress reportProgress;
    private String dateFormatPattern;
    private Class<? extends SerializeFilter> filters[];
    private SerializerFeature features[];
    private PropertyNamingStrategy propertyNamingStrategy;
    private boolean enableCircularReferenceDetect = false;

    public ReflectedCommandExecutor(CommandMethodMetaInfo methodMetaInfo, App app) {
        this.methodMetaInfo = $.requireNotNull(methodMetaInfo);
        this.app = $.NPE(app);
        this.paramTypes = paramTypes();
        this.paramCount = methodMetaInfo.paramCount();
        this.isStatic = methodMetaInfo.isStatic();
        this.commanderClass = app.classForName(methodMetaInfo.classInfo().className());
        try {
            this.method = commanderClass.getMethod(methodMetaInfo.methodName(), paramTypes);
            this.async = null != ReflectedInvokerHelper.getAnnotation(Async.class, method);
            this.reportProgress = ReflectedInvokerHelper.getAnnotation(ReportProgress.class, method);
            FastJsonFilter filterAnno = ReflectedInvokerHelper.getAnnotation(FastJsonFilter.class, method);
            if (null != filterAnno) {
                filters = filterAnno.value();
            }
            FastJsonFeature featureAnno = ReflectedInvokerHelper.getAnnotation(FastJsonFeature.class, method);
            if (null != featureAnno) {
                features = featureAnno.value();
            }
            FastJsonPropertyNamingStrategy propertyNamingStrategyAnno = ReflectedInvokerHelper.getAnnotation(FastJsonPropertyNamingStrategy.class, method);
            if (null != propertyNamingStrategyAnno) {
                propertyNamingStrategy = propertyNamingStrategyAnno.value();
            }
        } catch (NoSuchMethodException e) {
            throw E.unexpected(e);
        }
        if (!methodMetaInfo.isStatic()) {
            methodAccess = MethodAccess.get(commanderClass);
            commandIndex = methodAccess.getIndex(methodMetaInfo.methodName(), paramTypes);
        } else {
            method.setAccessible(true);
        }
        DateFormatPattern dfp = ReflectedInvokerHelper.getAnnotation(DateFormatPattern.class, method);
        if (null != dfp) {
            this.dateFormatPattern = dfp.value();
        } else {
            Pattern pattern = ReflectedInvokerHelper.getAnnotation(Pattern.class, method);
            if (null != pattern) {
                this.dateFormatPattern = pattern.value();
            }
        }
        this.paramLoaderService = app.service(ParamValueLoaderManager.class).get(CliContext.class);
        this.enableCircularReferenceDetect = hasAnnotation(EnableCircularReferenceDetect.class);
        this.buildParsingContext();
    }

    @Override
    public Object execute(CliContext context) {
        context.handlerMethod(method);
        if (null != dateFormatPattern) {
            context.dateFormatPattern(dateFormatPattern);
        }
        context.fastjsonFeatures(features);
        context.fastjsonPropertyNamingStrategy(propertyNamingStrategy);
        context.fastjsonFilters(filters);
        context.prepare(parsingContext);
        if (enableCircularReferenceDetect) {
            context.enableCircularReferenceDetect();
        }
        paramLoaderService.preParseOptions(method, methodMetaInfo, context);
        final Object cmd = commanderInstance(context);
        final Object[] params = params(cmd, context);
        if (async) {
            final JobManager jobManager = context.app().jobManager();
            final String jobId = jobManager.randomJobId();
            jobManager.prepare(jobId, new TrackableWorker() {
                @Override
                protected void run(ProgressGauge progressGauge) {
                    Object o = invoke(cmd, params);
                    jobManager.cacheResult(jobId, o, methodMetaInfo, null);
                }
            });
            context.setJobId(jobId);
            jobManager.now(jobId);
            String message = "Async job started: " + jobId;
            if (null != reportProgress) {
                context.println(message);
                context.attribute(ReportProgress.CTX_ATTR_KEY, reportProgress);
                return context.progress();
            } else {
                return message;
            }
        }
        return invoke(cmd, params);
    }

    @Override
    protected void releaseResources() {
        app = null;
        commandIndex = 0;
        commanderClass = null;
        method = null;
        methodAccess = null;
        paramTypes = null;
        super.releaseResources();
    }

    private Object commanderInstance(CliContext context) {
        if (isStatic) {
            return null;
        }
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
            ca[i] = app.classForName(className);
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

    private boolean hasAnnotation(Class<? extends Annotation> type) {
        return method.isAnnotationPresent(type) || commanderClass.isAnnotationPresent(type);
    }

    private void buildParsingContext() {
        CliContextParamLoader loader = app.service(ParamValueLoaderManager.class).get(CliContext.class);
        this.parsingContext = loader.buildParsingContext(commanderClass, method, methodMetaInfo);
    }

}
