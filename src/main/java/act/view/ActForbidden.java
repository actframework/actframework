package act.view;

import act.Act;
import act.app.SourceInfo;
import act.util.ActError;
import org.osgl.mvc.result.Forbidden;

import java.util.List;

public class ActForbidden extends Forbidden implements ActError {

    private SourceInfo sourceInfo;

    public ActForbidden() {
        super();
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActForbidden(String message, Object... args) {
        super(message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActForbidden(Throwable cause, String message, Object ... args) {
        super(cause, message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActForbidden(Throwable cause) {
        super(cause);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    private void loadSourceInfo() {
        doFillInStackTrace();
        Throwable cause = getCause();
        sourceInfo = Util.loadSourceInfo(null == cause ? getStackTrace() : cause.getStackTrace(), ActForbidden.class);
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

    public static Forbidden create() {
        return Act.isDev() ? new ActForbidden() : Forbidden.get();
    }

    public static Forbidden create(String msg, Object... args) {
        return Act.isDev() ? new ActForbidden(msg, args) : Forbidden.of(msg, args);
    }

    public static Forbidden create(Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActForbidden(cause, msg, args) : Forbidden.of(cause, msg, args);
    }

    public static Forbidden create(Throwable cause) {
        return Act.isDev() ? new ActForbidden(cause) : Forbidden.of(cause);
    }
}
