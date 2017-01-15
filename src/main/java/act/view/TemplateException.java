package act.view;

import act.app.*;
import org.osgl.util.S;

import java.util.List;

/**
 * Base class for Template error
 */
public abstract class TemplateException extends ActErrorResult {
    protected SourceInfo templateInfo;

    private String errorSpotTraceLine = null;

    public TemplateException(Exception t) {
        super(t);
    }

    public final SourceInfo templateSourceInfo() {
        return templateInfo;
    }

    public abstract String errorMessage();

    @Override
    public boolean isErrorSpot(String traceLine, String nextTraceLine) {
        if (null == errorSpotTraceLine) {
            errorSpotTraceLine = findErrorSpotTraceLine(stackTrace());
        }
        return S.eq(traceLine, errorSpotTraceLine);
    }

    @Override
    public synchronized Throwable getCause() {
        Throwable t0 = super.getCause();
        Throwable t = t0.getCause();
        return null == t ? t0 : t;
    }

    public Throwable getDirectCause() {
        return super.getCause();
    }

    protected String findErrorSpotTraceLine(List<String> stackTrace) {
        String spotLine = null, lastLine = null;
        for (String line: stackTrace) {
            if (line.contains("sun.reflect.NativeMethodAccessorImpl.invoke0")) {
                spotLine = lastLine;
            }
            if (isTemplateEngineInvokeLine(line)) {
                return spotLine;
            }
            lastLine = line;
        }
        return null;
    }

    protected static SourceInfo getJavaSourceInfo(Throwable cause) {
        if (null == cause) {
            return null;
        }
        cause = rootCauseOf(cause);
        DevModeClassLoader cl = (DevModeClassLoader) App.instance().classLoader();
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            int line = stackTraceElement.getLineNumber();
            if (line <= 0) {
                continue;
            }
            Source source = cl.source(stackTraceElement.getClassName());
            if (null == source) {
                continue;
            }
            return new SourceInfoImpl(source, line);
        }
        return null;
    }

    protected abstract boolean isTemplateEngineInvokeLine(String line);

}
