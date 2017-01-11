package act.view;

import act.Act;
import act.app.SourceInfo;
import act.util.ActError;
import org.osgl.mvc.result.MethodNotAllowed;

import java.util.List;

public class ActMethodNotAllowed extends MethodNotAllowed implements ActError {

    private SourceInfo sourceInfo;

    public ActMethodNotAllowed() {
        super();
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActMethodNotAllowed(String message, Object... args) {
        super(message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActMethodNotAllowed(Throwable cause, String message, Object ... args) {
        super(cause, message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActMethodNotAllowed(Throwable cause) {
        super(cause);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    private void loadSourceInfo() {
        doFillInStackTrace();
        Throwable cause = getCause();
        sourceInfo = Util.loadSourceInfo(null == cause ? getStackTrace() : cause.getStackTrace(), ActMethodNotAllowed.class);
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

    @Override
    public boolean isErrorSpot(String traceLine, String nextTraceLine) {
        return false;
    }

    public static MethodNotAllowed create() {
        return Act.isDev() ? new ActMethodNotAllowed() : MethodNotAllowed.get();
    }

    public static MethodNotAllowed create(String msg, Object... args) {
        return Act.isDev() ? new ActMethodNotAllowed(msg, args) : MethodNotAllowed.get(msg, args);
    }

    public static MethodNotAllowed create(Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActMethodNotAllowed(cause, msg, args) : new MethodNotAllowed(cause, msg, args);
    }

    public static MethodNotAllowed create(Throwable cause) {
        return Act.isDev() ? new ActMethodNotAllowed(cause) : new MethodNotAllowed(cause);
    }
}
