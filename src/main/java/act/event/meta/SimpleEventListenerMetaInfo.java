package act.event.meta;

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

import act.Act;
import act.app.App;
import act.app.event.SysEventId;
import act.inject.DependencyInjector;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SimpleEventListenerMetaInfo {
    private List<Object> events;
    private List<$.Func0> delayedEvents;
    private String className;
    private String methodName;
    private String asyncMethodName;
    private List<BeanSpec> paramTypes;
    private boolean async;
    private boolean isStatic;
    private boolean beforeAppStart;

    public SimpleEventListenerMetaInfo(
            final List<Object> events,
            final List<$.Func0> delayedEvents,
            final String className,
            final String methodName,
            final String asyncMethodName,
            final List<String> paramTypes,
            boolean async,
            boolean isStatic,
            boolean beforeAppStart,
            App app
    ) {
        int eventCnt = null == events ? 0 : events.size();
        int delayedEventCnt = null == delayedEvents ? 0 : delayedEvents.size();
        E.illegalArgumentIf(eventCnt == 0 && delayedEventCnt == 0);
        E.illegalArgumentIf(eventCnt > 0 & delayedEventCnt > 0);
        this.events = events;
        this.delayedEvents = delayedEvents;
        this.className = $.requireNotNull(className);
        this.methodName = $.requireNotNull(methodName);
        this.asyncMethodName = asyncMethodName;

        this.async = async;
        this.isStatic = isStatic;
        this.beforeAppStart = beforeAppStart;
        SysEventId hookOn = beforeAppStart ? SysEventId.DEPENDENCY_INJECTOR_LOADED : SysEventId.DEPENDENCY_INJECTOR_PROVISIONED;
        app.jobManager().on(hookOn, jobId(), new Runnable() {
            @Override
            public void run() {
                SimpleEventListenerMetaInfo.this.paramTypes = convert(paramTypes, className, methodName, $.<Method>var());
            }
        });
    }

    public String jobId() {
        S.Buffer buf = S.buffer("SimpleEventListenerByteCodeScanner:init:" )
                .append(className).append(".").append(methodName);
        if (null != asyncMethodName) {
            buf.append("|").append(asyncMethodName);
        }
        return buf.toString();
    }

    public List<?> events() {
        if (!delayedEvents.isEmpty()) {
            List list = new ArrayList();
            for ($.Func0 func : delayedEvents) {
                list.add(func.apply());
            }
            return list;
        }
        return events;
    }

    public String className() {
        return className;
    }

    public String methodName() {
        return methodName;
    }

    public String asyncMethodName() {
        return asyncMethodName;
    }

    public List<BeanSpec> paramTypes() {
        return paramTypes;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean beforeAppStart() {
        return beforeAppStart;
    }

    public static List<BeanSpec> convert(List<String> paramTypes, String className, String methodName, $.Var<Method> methodHolder) {
        int sz = paramTypes.size();
        App app = Act.app();
        Class c = app.classForName(className);
        Class[] paramClasses = new Class[sz];
        int i = 0;
        for (String s : paramTypes) {
            paramClasses[i++] = app.classForName(s);
        }
        Method method = $.getMethod(c, methodName, paramClasses);
        method.setAccessible(true);
        methodHolder.set(method);
        if (0 == sz) {
            return C.list();
        }
        List<BeanSpec> retVal = new ArrayList<>(sz);
        Type[] types = method.getGenericParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        DependencyInjector injector = app.injector();
        for (i = 0; i < types.length; ++i) {
            retVal.add(BeanSpec.of(types[i], annotations[i], null, injector));
        }
        return C.list(retVal);
    }
}
