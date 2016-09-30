package act.view;

import act.app.SourceInfo;

/**
 * Base class for Template error
 */
public abstract class TemplateException extends ActServerError {
    protected SourceInfo templateInfo;

    public TemplateException(Exception t) {
        super(t);
    }

    public final SourceInfo templateSourceInfo() {
        return templateInfo;
    }

    public abstract String errorMessage();

    @Override
    public synchronized Throwable getCause() {
        Throwable t0 = super.getCause();
        Throwable t = t0.getCause();
        return null == t ? t0 : t;
    }

    public Throwable getDirectCause() {
        return super.getCause();
    }

}
