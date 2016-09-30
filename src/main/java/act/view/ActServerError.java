package act.view;

import act.Act;
import act.app.*;
import act.exception.BindException;
import act.util.ActError;
import org.osgl.$;
import org.osgl.exception.InvalidRangeException;
import org.osgl.exception.UnsupportedException;
import org.osgl.http.H;
import org.osgl.mvc.annotation.ResponseStatus;
import org.osgl.mvc.result.ErrorResult;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.result.ServerError;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.validation.ValidationException;
import java.util.List;
import java.util.Map;

public class ActServerError extends ServerError implements ActError {

    protected SourceInfo sourceInfo;

    protected ActServerError(Throwable t) {
        super($.notNull(t));
        if (Act.isDev()) {
            populateSourceInfo(t);
        }
    }


    @Override
    public Throwable getCauseOrThis() {
        Throwable t0 = this;
        Throwable t = t0.getCause();
        while (null != t) {
            t0 = t;
            t = t.getCause();
        }
        return t0;
    }

    public SourceInfo sourceInfo() {
        return sourceInfo;
    }

    @Override
    public int statusCode() {
        Throwable cause = super.getCause();
        int statusCode = userDefinedStatusCode(cause.getClass());
        return -1 == statusCode ? super.statusCode() : statusCode;
    }

    public List<String> stackTrace() {
        List<String> l = C.newList();
        Throwable t = getCauseOrThis();
        while (null != t) {
            StackTraceElement[] a = t.getStackTrace();
            for (StackTraceElement e : a) {
                l.add("at " + e.toString());
            }
            t = t.getCause();
            if (null != t) {
                l.add("Caused by " + t.toString());
            }
        }
        return l;
    }

    @Override
    public boolean isErrorSpot(String traceLine, String nextTraceLine) {
        return false;
    }

    protected void populateSourceInfo(Throwable t) {
        if (t instanceof SourceInfo) {
            this.sourceInfo = (SourceInfo)t;
        } else {
            DevModeClassLoader cl = (DevModeClassLoader) App.instance().classLoader();
            for (StackTraceElement stackTraceElement : t.getStackTrace()) {
                int line = stackTraceElement.getLineNumber();
                if (line <= 0) {
                    continue;
                }
                Source source = cl.source(stackTraceElement.getClassName());
                if (null == source) {
                    continue;
                }
                sourceInfo = new SourceInfoImpl(source, line);
            }
        }
    }

    private static Map<Class<? extends Throwable>, $.Function<Throwable, Result>> x = C.newMap();
    static {
        $.Function<Throwable, Result> unsupported = new $.Transformer<Throwable, Result>() {
            @Override
            public Result transform(Throwable throwable) {
                return ActNotImplemented.create(throwable);
            }
        };
        x.put(UnsupportedException.class, unsupported);
        x.put(UnsupportedOperationException.class, unsupported);
        x.put(IllegalStateException.class, new $.Transformer<Throwable, Result>() {
            @Override
            public Result transform(Throwable throwable) {
                return ActConflict.create(throwable);
            }
        });
        $.Transformer<Throwable, Result> badRequest = new $.Transformer<Throwable, Result>() {
            @Override
            public Result transform(Throwable throwable) {
                return ActBadRequest.create(throwable);
            }
        };
        x.put(IllegalArgumentException.class, badRequest);
        x.put(InvalidRangeException.class, badRequest);
        x.put(IndexOutOfBoundsException.class, badRequest);
        x.put(ValidationException.class, badRequest);
        x.put(BindException.class, badRequest);
    }

    private static Map<Class, Integer> userDefinedStatus = C.newMap();

    private static int userDefinedStatusCode(Class<? extends Throwable> exCls) {
        Integer I = userDefinedStatus.get(exCls);
        if (null == I) {
            ResponseStatus rs = exCls.getAnnotation(ResponseStatus.class);
            if (null == rs) {
                I = -1;
                userDefinedStatus.put(exCls, -1);
            } else {
                I = rs.value();
            }
        }
        return I;
    }

    public static Result of(Throwable t) {
        if (t instanceof Result) {
            return (Result) t;
        } else if (t instanceof org.rythmengine.exception.RythmException) {
            return new RythmTemplateException((org.rythmengine.exception.RythmException) t);
        } else {
            $.Function<Throwable, Result> transformer = transformerOf(t);
            return null == transformer ? new ActServerError(t) : transformer.apply(t);
        }
    }

    private static $.Function<Throwable, Result> transformerOf(Throwable t) {
        Class tc = t.getClass();
        $.Function<Throwable, Result> transformer = x.get(tc);
        if (null != transformer) {
            return transformer;
        }
        for (Class c : x.keySet()) {
            if (c.isAssignableFrom(tc)) {
                return x.get(c);
            }
        }
        return null;
    }

    public static ActServerError of(NullPointerException e) {
        return new ActServerError(e);
    }

    public static ActServerError of(org.rythmengine.exception.RythmException e) {
        return new RythmTemplateException(e);
    }

    public static Result of(int statusCode) {
        E.illegalArgumentIf(statusCode < 400);
        switch (statusCode) {
            case 400:
                return ActBadRequest.create();
            case 401:
                return ActUnauthorized.create();
            case 403:
                return ActForbidden.create();
            case 404:
                return ActNotFound.create();
            case 405:
                return ActMethodNotAllowed.create();
            case 409:
                return ActConflict.create();
            case 500:
                return new ActServerError(new RuntimeException());
            case 501:
                return ActNotImplemented.create();
            default:
                return new ErrorResult(H.Status.of(statusCode));
        }
    }

}
