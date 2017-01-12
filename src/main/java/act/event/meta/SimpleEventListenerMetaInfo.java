package act.event.meta;

import act.Act;
import act.app.App;
import act.app.event.AppEventId;
import act.inject.DependencyInjector;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.C;
import org.osgl.util.E;

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

    public SimpleEventListenerMetaInfo(
            final List<Object> events,
            final List<$.Func0> delayedEvents,
            final String className,
            final String methodName,
            final String asyncMethodName,
            final List<String> paramTypes,
            boolean async,
            boolean isStatic,
            App app
    ) {
        int eventCnt = null == events ? 0 : events.size();
        int delayedEventCnt = null == delayedEvents ? 0 : delayedEvents.size();
        E.illegalArgumentIf(eventCnt == 0 && delayedEventCnt == 0);
        E.illegalArgumentIf(eventCnt > 0 & delayedEventCnt > 0);
        this.events = events;
        this.delayedEvents = delayedEvents;
        this.className = $.notNull(className);
        this.methodName = $.notNull(methodName);
        this.asyncMethodName = asyncMethodName;

        this.async = async;
        this.isStatic = isStatic;
        app.jobManager().on(AppEventId.DEPENDENCY_INJECTOR_PROVISIONED, new Runnable() {
            @Override
            public void run() {
                SimpleEventListenerMetaInfo.this.paramTypes = convert(paramTypes, className, methodName, $.<Method>var());
            }
        });
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

    public static List<BeanSpec> convert(List<String> paramTypes, String className, String methodName, $.Var<Method> methodHolder) {
        int sz = paramTypes.size();
        App app = Act.app();
        ClassLoader cl = app.classLoader();
        Class c = $.classForName(className, cl);
        Class[] paramClasses = new Class[sz];
        int i = 0;
        for (String s : paramTypes) {
            paramClasses[i++] = $.classForName(s, cl);
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
