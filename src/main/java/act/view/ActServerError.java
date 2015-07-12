package act.view;

import act.Act;
import act.app.*;
import org.osgl.mvc.result.ServerError;
import org.osgl.util.C;

import java.util.List;

public class ActServerError extends ServerError {

    private SourceInfo sourceInfo;

    public ActServerError(Throwable t, App app) {
        super(t);
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
            if (null != t) {
                l.add("Caused by " + t.toString());
            }
        }
        return l;
    }

}
