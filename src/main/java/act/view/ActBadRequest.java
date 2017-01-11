package act.view;

import act.Act;
import act.app.SourceInfo;
import act.util.ActError;
import org.osgl.mvc.result.BadRequest;

import java.util.List;

public class ActBadRequest extends BadRequest implements ActError {

    private SourceInfo sourceInfo;

    public ActBadRequest() {
        super();
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActBadRequest(String message, Object... args) {
        super(message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActBadRequest(Throwable cause, String message, Object ... args) {
        super(cause, message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActBadRequest(Throwable cause) {
        super(cause);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    private void loadSourceInfo() {
        doFillInStackTrace();
        Throwable cause = getCause();
        sourceInfo = Util.loadSourceInfo(null == cause ? getStackTrace() : cause.getStackTrace(), ActBadRequest.class);
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

    public static BadRequest create() {
        return Act.isDev() ? new ActBadRequest() : BadRequest.get();
    }

    public static BadRequest create(String msg, Object... args) {
        return Act.isDev() ? new ActBadRequest(msg, args) : BadRequest.get(msg, args);
    }

    public static BadRequest create(Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActBadRequest(cause, msg, args) : new BadRequest(cause, msg, args);
    }

    public static BadRequest create(Throwable cause) {
        return Act.isDev() ? new ActBadRequest(cause) : new BadRequest(cause);
    }
}
