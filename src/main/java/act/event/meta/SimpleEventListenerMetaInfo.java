package act.event.meta;

import org.osgl.$;
import org.osgl.util.C;

import java.util.List;

public class SimpleEventListenerMetaInfo {
    private List<String> events;
    private String className;
    private String methodName;
    private List<String> paramTypes;
    private boolean async;

    public SimpleEventListenerMetaInfo(List<String> events, String className, String methodName, List<String> paramTypes, boolean async) {
        this.events = C.list(events);
        this.className = $.notNull(className);
        this.methodName = $.notNull(methodName);
        this.paramTypes = C.list(paramTypes);
        this.async = async;
    }

    public List<String> events() {
        return events;
    }

    public String className() {
        return className;
    }

    public String methodName() {
        return methodName;
    }

    public List<String> paramTypes() {
        return paramTypes;
    }

    public boolean isAsync() {
        return async;
    }
}
