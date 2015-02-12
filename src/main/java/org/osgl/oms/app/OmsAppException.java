package org.osgl.oms.app;

import org.osgl._;
import org.osgl.oms.OMS;
import org.osgl.oms.OmsException;
import org.osgl.util.E;

/**
 * Application error
 */
public abstract class OmsAppException extends OmsException {

    public OmsAppException() {
    }

    public OmsAppException(String message) {
        super(message);
    }

    public OmsAppException(String message, Object... args) {
        super(message, args);
    }

    public OmsAppException(Throwable cause) {
        super(cause);
    }

    public OmsAppException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }

    public abstract String getErrorTitle();

    public abstract String getErrorDescription();


    public static StackTraceElement getInterestingStackTraceElement(App app, Throwable cause) {
        if (!OMS.isDev()) {
            return null;
        }
        AppClassLoader classLoader = app.classLoader();
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            if (stackTraceElement.getLineNumber() > 0 && classLoader.isAppClass(stackTraceElement.getClassName())) {
                return stackTraceElement;
            }
        }
        return null;
    }
}
