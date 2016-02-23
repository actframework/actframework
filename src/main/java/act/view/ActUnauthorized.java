package act.view;

import act.Act;
import act.app.SourceInfo;
import act.util.ActError;
import org.osgl.mvc.result.Unauthorized;

import java.util.List;

public class ActUnauthorized extends Unauthorized implements ActError {

    private SourceInfo sourceInfo;

    public ActUnauthorized() {
        super();
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActUnauthorized(String realm) {
        super(realm);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActUnauthorized(String realm, boolean digest) {
        super(realm, digest);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    private void loadSourceInfo() {
        doFillInStackTrace();
        Throwable cause = getCause();
        sourceInfo = Util.loadSourceInfo(null == cause ? getStackTrace() : cause.getStackTrace(), ActUnauthorized.class);
    }

    @Override
    public Throwable getCauseOrThis() {
        return this;
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

    public static Unauthorized create() {
        return Act.isDev() ? new ActUnauthorized() : Unauthorized.INSTANCE;
    }

    public static Unauthorized create(String realm) {
        return Act.isDev() ? new ActUnauthorized(realm) : new Unauthorized(realm);
    }

    public static Unauthorized create(String realm, boolean digest) {
        return Act.isDev() ? new ActUnauthorized(realm, digest) : new Unauthorized(realm, digest);
    }

}
