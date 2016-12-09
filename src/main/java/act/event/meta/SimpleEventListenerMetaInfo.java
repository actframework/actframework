package act.event.meta;

import org.osgl.$;
import org.osgl.util.C;

import java.util.List;

public class SimpleEventListenerMetaInfo {
    private List<Object> events;
    private String className;
    private String methodName;
    private String asyncMethodName;
    private List<String> paramTypes;
    private boolean async;
    private boolean isStatic;

    public SimpleEventListenerMetaInfo(List<Object> events, String className, String methodName, String asyncMethodName, List<String> paramTypes, boolean async, boolean isStatic) {
        this.events = C.list(events);
        this.className = $.notNull(className);
        this.methodName = $.notNull(methodName);
        this.asyncMethodName = asyncMethodName;
        this.paramTypes = C.list(paramTypes);
        this.async = async;
        this.isStatic = isStatic;
    }

    public List<?> events() {
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

    public List<String> paramTypes() {
        return paramTypes;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isStatic() {
        return isStatic;
    }
}
