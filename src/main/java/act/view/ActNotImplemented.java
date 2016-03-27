package act.view;

import act.Act;
import act.app.SourceInfo;
import act.util.ActError;
import org.osgl.mvc.result.NotImplemented;

import java.util.List;

public class ActNotImplemented extends NotImplemented implements ActError {

    private SourceInfo sourceInfo;

    public ActNotImplemented() {
        super();
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActNotImplemented(String message, Object... args) {
        super(message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActNotImplemented(Throwable cause, String message, Object ... args) {
        super(cause, message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActNotImplemented(Throwable cause) {
        super(cause);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    private void loadSourceInfo() {
        doFillInStackTrace();
        Throwable cause = getCause();
        sourceInfo = Util.loadSourceInfo(null == cause ? getStackTrace() : cause.getStackTrace(), ActNotImplemented.class);
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

    public static NotImplemented create() {
        return Act.isDev() ? new ActNotImplemented() : NotImplemented.INSTANCE;
    }

    public static NotImplemented create(String msg, Object... args) {
        return Act.isDev() ? new ActNotImplemented(msg, args) : new NotImplemented(msg, args);
    }

    public static NotImplemented create(Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActNotImplemented(cause, msg, args) : new NotImplemented(cause, msg, args);
    }

    public static NotImplemented create(Throwable cause) {
        return Act.isDev() ? new ActNotImplemented(cause) : new NotImplemented(cause);
    }
}
