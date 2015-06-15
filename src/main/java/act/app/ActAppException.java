package act.app;

import act.Act;
import act.exception.ActException;

/**
 * Application error
 */
public abstract class ActAppException extends ActException {

    public ActAppException() {
    }

    public ActAppException(String message) {
        super(message);
    }

    public ActAppException(String message, Object... args) {
        super(message, args);
    }

    public ActAppException(Throwable cause) {
        super(cause);
    }

    public ActAppException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }

    public abstract String getErrorTitle();

    public abstract String getErrorDescription();


    public static StackTraceElement getInterestingStackTraceElement(App app, Throwable cause) {
        if (!Act.isDev()) {
            return null;
        }
        AppClassLoader classLoader = app.classLoader();
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            if (stackTraceElement.getLineNumber() > 0 && classLoader.isSourceClass(stackTraceElement.getClassName())) {
                return stackTraceElement;
            }
        }
        return null;
    }
}
