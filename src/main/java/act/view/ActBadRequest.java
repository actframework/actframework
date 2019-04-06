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

import act.Act;
import act.app.SourceInfo;
import act.util.ActError;
import org.osgl.mvc.result.BadRequest;

import java.util.List;

import static act.util.ActError.Util.errorMessage;
import static act.util.ActError.Util.loadSourceInfo;
import static org.osgl.http.H.Status.BAD_REQUEST;

public class ActBadRequest extends BadRequest implements ActError {

    private SourceInfo sourceInfo;

    public ActBadRequest() {
        super(errorMessage(BAD_REQUEST));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActBadRequest.class);
        }
    }

    public ActBadRequest(String message, Object... args) {
        super(errorMessage(BAD_REQUEST, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActBadRequest.class);
        }
    }

    public ActBadRequest(Throwable cause, String message, Object ... args) {
        super(cause, errorMessage(BAD_REQUEST, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(cause, ActBadRequest.class);
        }
    }

    public ActBadRequest(Throwable cause) {
        super(cause, errorMessage(BAD_REQUEST, cause.getMessage()));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(cause, ActBadRequest.class);
        }
    }

    public ActBadRequest(int errorCode) {
        super(errorCode, errorMessage(BAD_REQUEST));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActBadRequest.class);
        }
    }

    public ActBadRequest(int errorCode, String message, Object... args) {
        super(errorCode, errorMessage(BAD_REQUEST, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActBadRequest.class);
        }
    }

    public ActBadRequest(int errorCode, Throwable cause, String message, Object... args) {
        super(errorCode, cause, errorMessage(BAD_REQUEST, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(cause, ActBadRequest.class);
        }
    }

    public ActBadRequest(int errorCode, Throwable cause) {
        super(errorCode, cause, errorMessage(BAD_REQUEST, cause.getMessage()));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(cause, ActBadRequest.class);
        }
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
        return Util.stackTraceOf(this);
    }

    @Override
    public boolean isErrorSpot(String traceLine, String nextTraceLine) {
        return false;
    }

    public static BadRequest create() {
        return Act.isDev() ? new ActBadRequest() : BadRequest.get();
    }

    public static BadRequest create(String msg, Object... args) {
        return Act.isDev() ? new ActBadRequest(msg, args) : BadRequest.of(msg, args);
    }

    public static BadRequest create(Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActBadRequest(cause, msg, args) : BadRequest.of(cause, msg, args);
    }

    public static BadRequest create(Throwable cause) {
        return Act.isDev() ? new ActBadRequest(cause) : BadRequest.of(cause);
    }

    public static BadRequest create(int code) {
        return Act.isDev() ? new ActBadRequest(code) : BadRequest.of(code);
    }

    public static BadRequest create(int code, String msg, Object... args) {
        return Act.isDev() ? new ActBadRequest(code, msg, args) : BadRequest.of(code, msg, args);
    }

    public static BadRequest create(int code, Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActBadRequest(code, cause, msg, args) : BadRequest.of(code, cause, msg, args);
    }

    public static BadRequest create(int code, Throwable cause) {
        return Act.isDev() ? new ActBadRequest(code, cause) : BadRequest.of(code, cause);
    }

}
