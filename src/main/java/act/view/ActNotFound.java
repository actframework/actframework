package act.view;

import act.Act;
import act.app.*;
import act.util.ActError;
import org.osgl.mvc.result.Forbidden;
import org.osgl.mvc.result.NotFound;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

public class ActNotFound extends NotFound implements ActError {

    private SourceInfo sourceInfo;
    private Throwable cause;

    public ActNotFound() {
        super();
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActNotFound(String message) {
        super(message);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActNotFound(String message, Object... args) {
        super(message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    private void loadSourceInfo() {
        doFillInStackTrace();
        DevModeClassLoader cl = (DevModeClassLoader) App.instance().classLoader();
        for (StackTraceElement stackTraceElement : getStackTrace()) {
            int line = stackTraceElement.getLineNumber();
            if (line <= 0) {
                continue;
            }
            String className = stackTraceElement.getClassName();
            if (S.eq(ActNotFound.class.getName(), className)) {
                continue;
            }
            Source source = cl.source(className);
            if (null == source) {
                continue;
            }
            sourceInfo = new SourceInfoImpl(source, line);
        }
    }

    public ActNotFound(Throwable t, App app) {
        cause = t;
        if (Act.isDev()) {
            if (t instanceof SourceInfo) {
                this.sourceInfo = (SourceInfo)t;
            } else {
                DevModeClassLoader cl = (DevModeClassLoader) app.classLoader();
                for (StackTraceElement stackTraceElement : t.getStackTrace()) {
                    int line = stackTraceElement.getLineNumber();
                    if (line <= 0) {
                        continue;
                    }
                    Source source = cl.source(stackTraceElement.getClassName());
                    if (null == source) {
                        continue;
                    }
                    sourceInfo = new SourceInfoImpl(source, line);
                }
            }
        }
    }

    @Override
    public synchronized Throwable getCause() {
        return null != cause ? cause : this;
    }


    public SourceInfo sourceInfo() {
        return sourceInfo;
    }

    public List<String> stackTrace() {
        List<String> l = C.newList();
        Throwable t = getCause();
        while (null != t) {
            StackTraceElement[] a = t.getStackTrace();
            for (StackTraceElement e : a) {
                l.add("at " + e.toString());
            }
            t = t.getCause();
            if (t == this) {
                break;
            }
            if (null != t) {
                l.add("Caused by " + t.toString());
            }
        }
        return l;
    }
}
