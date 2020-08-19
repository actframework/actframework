package act.view;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static act.util.ActError.Util.errorMessage;
import static act.util.ActError.Util.loadSourceInfo;
import static org.osgl.http.H.Status.INTERNAL_SERVER_ERROR;

import act.Act;
import act.app.*;
import act.asm.AsmContext;
import act.asm.AsmException;
import act.exception.BindException;
import act.util.ActError;
import org.osgl.$;
import org.osgl.exception.*;
import org.osgl.http.H;
import org.osgl.mvc.annotation.ResponseStatus;
import org.osgl.mvc.result.ErrorResult;
import org.osgl.mvc.result.Result;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.*;
import javax.persistence.PersistenceException;
import javax.validation.*;

public class ActErrorResult extends ErrorResult implements ActError {

    protected SourceInfo sourceInfo;

    public ActErrorResult(H.Status status) {
        super(status, errorMessage(status));
        init();
        populateSourceInfo();
    }

    public ActErrorResult(H.Status status, String message, Object... args) {
        super(status, errorMessage(status, message, args));
        init();
        populateSourceInfo();
    }

    public ActErrorResult(H.Status status, int errorCode) {
        super(status, errorCode, errorMessage(status));
        init();
        populateSourceInfo();
    }

    public ActErrorResult(H.Status status, int errorCode, String message, Object... args) {
        super(status, errorCode, errorMessage(status, message, args));
        init();
        populateSourceInfo();
    }

    public ActErrorResult(Throwable cause) {
        super(INTERNAL_SERVER_ERROR, cause, errorMessage(INTERNAL_SERVER_ERROR));
        init();
        populateSourceInfo(cause);
    }

    private ActErrorResult(AsmException exception, boolean scanning) {
        super(INTERNAL_SERVER_ERROR, exception.getCause(), errorMsg(exception, scanning));
        init();
        populateSourceInfo(exception.context());
    }

    public ActErrorResult(H.Status status, Throwable cause) {
        super(status, cause, errorMessage(status));
        init();
        populateSourceInfo(cause);
    }

    public ActErrorResult(H.Status status, Throwable cause, String message, Object... args) {
        super(status, cause, errorMessage(status, message, args));
        init();
        populateSourceInfo(cause);
    }

    public ActErrorResult(H.Status status, int errorCode, Throwable cause, String message, Object... args) {
        super(status, errorCode, cause, errorMessage(status, message, args));
        init();
        populateSourceInfo(cause);
    }

    public ActErrorResult(H.Status status, int errorCode, Throwable cause) {
        super(status, errorCode, cause, errorMessage(status));
        init();
        populateSourceInfo(cause);
    }

    @Override
    public Throwable getCauseOrThis() {
        Throwable cause = getCause();
        return null == cause ? this : cause;
    }

    public SourceInfo sourceInfo() {
        return sourceInfo;
    }

    @Override
    public int statusCode() {
        Throwable cause = super.getCause();
        int statusCode = null == cause ? -1 : userDefinedStatusCode(cause.getClass());
        return -1 == statusCode ? super.statusCode() : statusCode;
    }

    public List<String> stackTrace() {
        return Util.stackTraceOf(this);
    }

    @Override
    public boolean isErrorSpot(String traceLine, String nextTraceLine) {
        return false;
    }

    protected void init() {
    }

    protected void populateSourceInfo(Throwable cause) {
        if (!Act.isDev()) {
            return;
        }
        sourceInfo = loadSourceInfo(cause, getClass());
    }

    private void populateSourceInfo() {
        if (!Act.isDev()) {
            return;
        }
        sourceInfo = loadSourceInfo(new RuntimeException(), getClass());
    }

    protected void populateSourceInfo(AsmContext context) {
        if (!Act.isDev()) {
            return;
        }
        this.sourceInfo = Util.loadSourceInfo(context);
    }

    private static Map<Class<? extends Throwable>, $.Function<Throwable, Result>> x = new HashMap<>();

    static {
        $.Function<Throwable, Result> unsupported = new $.Transformer<Throwable, Result>() {
            @Override
            public Result transform(Throwable throwable) {
                return ActNotImplemented.create(throwable);
            }
        };
        x.put(ToBeImplemented.class, new $.Transformer<Throwable, Result>() {
            @Override
            public Result transform(Throwable throwable) {
                return ActToBeImplemented.create();
            }
        });
        x.put(UnsupportedException.class, unsupported);
        x.put(UnsupportedOperationException.class, unsupported);
        x.put(IllegalStateException.class, new $.Transformer<Throwable, Result>() {
            @Override
            public Result transform(Throwable throwable) {
                return ActConflict.create(throwable);
            }
        });
        x.put(ResourceNotFoundException.class, new $.Transformer<Throwable, Result>() {
            @Override
            public Result transform(Throwable throwable) {
                return ActNotFound.create(throwable);
            }
        });
        x.put(AccessDeniedException.class, new $.Transformer<Throwable, Result>() {
            @Override
            public Result transform(Throwable throwable) {
                return ActForbidden.create(throwable);
            }
        });
        $.Transformer<Throwable, Result> badRequest = new $.Transformer<Throwable, Result>() {
            @Override
            public Result transform(Throwable throwable) {
                return ActBadRequest.create(throwable);
            }
        };
//        x.put(IllegalArgumentException.class, badRequest);
//        x.put(InvalidRangeException.class, badRequest);
//        x.put(IndexOutOfBoundsException.class, badRequest);
        x.put(ValidationException.class, badRequest);
        x.put(BindException.class, badRequest);
    }

    private static Map<Class, Integer> userDefinedStatus;

    public static void classInit(App app) {
        userDefinedStatus = app.createMap();
    }

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
        }
        if (t instanceof PersistenceException && null != t.getCause()) {
            t = t.getCause();
        }
        if (t instanceof ConstraintViolationException) {
            ConstraintViolationException e = (ConstraintViolationException) t;
            Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
            Map<String, ConstraintViolation> lookup = new HashMap<>();
            for (ConstraintViolation v : violations) {
                lookup.put(S.pathConcat(v.getRootBeanClass().getSimpleName(), '.', v.getPropertyPath().toString()), v);
            }
            S.Buffer buf = S.buffer();
            ActionContext.buildViolationMessage(lookup, buf, ";");
            String msg = buf.toString();
            return ActBadRequest.create(msg);
        } else if (t instanceof org.rythmengine.exception.RythmException) {
            return Act.isDev() ? new RythmTemplateException((org.rythmengine.exception.RythmException) t) : ErrorResult.of(INTERNAL_SERVER_ERROR);
        } else if (t instanceof AsmException) {
            return new ActErrorResult((AsmException) t, true);
        }
        $.Function<Throwable, Result> transformer = transformerOf(t);
        return null == transformer ? new ActErrorResult(t) : transformer.apply(t);
    }

    public static ActErrorResult scanningError(AsmException exception) {
        return new ActErrorResult(exception, true);
    }

    public static ActErrorResult enhancingError(AsmException exception) {
        return new ActErrorResult(exception, false);
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

    public static Result ofStatus(int statusCode) {
        return of(H.Status.of(statusCode));
    }

    @Deprecated
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
            case 501:
                return ActNotImplemented.create();
            default:
                if (Act.isDev()) {
                    return new ActErrorResult(new RuntimeException());
                } else {
                    return new ErrorResult(H.Status.of(statusCode));
                }
        }
    }

    public static ErrorResult of(H.Status status) {
        E.illegalArgumentIf(!status.isClientError() && !status.isServerError());
        if (Act.isDev()) {
            return new ActErrorResult(status);
        } else {
            return ErrorResult.of(status);
        }
    }

    public static ErrorResult of(H.Status status, String message, Object... args) {
        E.illegalArgumentIf(!status.isClientError() && !status.isServerError());
        if (Act.isDev()) {
            return new ActErrorResult(status, message, args);
        } else {
            return ErrorResult.of(status, message, args);
        }
    }

    public static ErrorResult of(H.Status status, int errorCode) {
        E.illegalArgumentIf(!status.isClientError() && !status.isServerError());
        if (Act.isDev()) {
            return new ActErrorResult(status, errorCode);
        } else {
            return ErrorResult.of(status, errorCode);
        }
    }

    public static ErrorResult of(H.Status status, int errorCode, String message, Object... args) {
        E.illegalArgumentIf(!status.isClientError() && !status.isServerError());
        if (Act.isDev()) {
            return new ActErrorResult(status, errorCode, message, args);
        } else {
            return ErrorResult.of(status, errorCode, message, args);
        }
    }

    public static Throwable rootCauseOf(Throwable t) {
        if (null == t) {
            return null;
        }
        Throwable cause;
        for (; ; ) {
            cause = t.getCause();
            if (null != cause) {
                t = cause;
            } else {
                break;
            }
        }
        return t;
    }


    private static String errorMsg(AsmException exception, boolean scanning) {
        String userMsg = exception.getLocalizedMessage();
        return (S.blank(userMsg)) ? S.concat("Error ", scanning ? "scanning" : "enhancing", " bytecode at ", exception.context().toString()) : userMsg;
    }
}
