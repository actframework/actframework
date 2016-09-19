package act.view;

import act.app.SourceInfo;

/**
 * Base class for Template error
 */
public abstract class TemplateError extends ActServerError {
    protected SourceInfo templateInfo;

    public TemplateError(Exception t) {
        super(t);
    }

    public final SourceInfo templateSourceInfo() {
        return templateInfo;
    }

    public abstract String errorMessage();

}
