package act.view;

import act.Act;
import act.app.SourceInfo;
import act.util.ActError;
import org.osgl.mvc.result.NotFound;

import java.util.List;

public class ActNotFound extends NotFound implements ActError {

    private SourceInfo sourceInfo;

    public ActNotFound() {
        super();
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

    public ActNotFound(Throwable cause, String message, Object ... args) {
        super(cause, message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActNotFound(Throwable cause) {
        super(cause);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    private void loadSourceInfo() {
        doFillInStackTrace();
        Throwable cause = getCause();
        sourceInfo = Util.loadSourceInfo(null == cause ? getStackTrace() : cause.getStackTrace(), ActNotFound.class);
    }

    @Override
    public Throwable getCauseOrThis() {
        Throwable cause = super.getCause();
        return null == cause ? this : cause;
    }

    public SourceInfo sourceInfo() {
        return sourceInfo;
    }

    public List<String> stackTrace() {
        Throwable cause = getCause();
        ActError root = this;
        if (null == cause) {
            cause = this;
            root = null;
        }
        return Util.stackTraceOf(cause, root);
    }


    public static NotFound create() {
        return Act.isDev() ? new ActNotFound() : NotFound.INSTANCE;
    }

    public static NotFound create(String msg, Object... args) {
        return Act.isDev() ? new ActNotFound(msg, args) : new NotFound(msg, args);
    }

    public static NotFound create(Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActNotFound(cause, msg, args) : new NotFound(cause, msg, args);
    }

    public static NotFound create(Throwable cause) {
        return Act.isDev() ? new ActNotFound(cause) : new NotFound(cause);
    }
}
