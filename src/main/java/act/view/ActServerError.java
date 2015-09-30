package act.view;

import act.Act;
import act.app.*;
import act.exception.ActException;
import act.util.ActError;
import org.osgl.exception.UnexpectedException;
import org.osgl.mvc.result.ServerError;
import org.osgl.util.C;
import org.rythmengine.exception.RythmException;

import java.util.List;

public class ActServerError extends ServerError implements ActError {

    protected SourceInfo sourceInfo;

    protected ActServerError(Throwable t, App app) {
        super(t);
        if (Act.isDev()) {
            populateSourceInfo(t, app);
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

    protected void populateSourceInfo(Throwable t, App app) {
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

    public static ActServerError of(Throwable t, App app) {
        if (t instanceof RythmException) {
            return new RythmError((RythmException) t, app);
        } else {
            return new ActServerError(t, app);
        }
    }

    public static ActServerError of(NullPointerException e, App app) {
        return new ActServerError(e, app);
    }

    public static ActServerError of(UnexpectedException e, App app) {
        return new ActServerError(e, app);
    }

    public static ActServerError of(ActException e, App app) {
        return new ActServerError(e, app);
    }

    public static ActServerError of(IllegalArgumentException e, App app) {
        return new ActServerError(e, app);
    }

    public static ActServerError of(IllegalStateException e, App app) {
        return new ActServerError(e, app);
    }

    public static ActServerError of(RythmException e, App app) {
        return new RythmError(e, app);
    }

}
