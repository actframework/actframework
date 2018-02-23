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
import org.osgl.mvc.result.NotImplemented;

import java.util.List;

import static act.util.ActError.Util.errorMessage;
import static act.util.ActError.Util.loadSourceInfo;
import static org.osgl.http.H.Status.NOT_IMPLEMENTED;

public class ActNotImplemented extends NotImplemented implements ActError {

    private SourceInfo sourceInfo;

    public ActNotImplemented() {
        super(errorMessage(NOT_IMPLEMENTED));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActNotImplemented.class);
        }
    }

    public ActNotImplemented(String message, Object... args) {
        super(errorMessage(NOT_IMPLEMENTED, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActNotImplemented.class);
        }
    }

    public ActNotImplemented(Throwable cause, String message, Object ... args) {
        super(cause, errorMessage(NOT_IMPLEMENTED, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActNotImplemented.class);
        }
    }

    public ActNotImplemented(Throwable cause) {
        super(cause, errorMessage(NOT_IMPLEMENTED));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(cause, ActNotImplemented.class);
        }
    }

    public ActNotImplemented(int code) {
        super(code, errorMessage(NOT_IMPLEMENTED));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActNotImplemented.class);
        }
    }

    public ActNotImplemented(int code, String message, Object... args) {
        super(code, errorMessage(NOT_IMPLEMENTED, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActNotImplemented.class);
        }
    }

    public ActNotImplemented(int code, Throwable cause, String message, Object ... args) {
        super(code, cause, errorMessage(NOT_IMPLEMENTED, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActNotImplemented.class);
        }
    }

    public ActNotImplemented(int code, Throwable cause) {
        super(code, cause, errorMessage(NOT_IMPLEMENTED));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(cause, ActNotImplemented.class);
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

    public static NotImplemented create() {
        return Act.isDev() ? new ActNotImplemented() : NotImplemented.get();
    }

    public static NotImplemented create(String msg, Object... args) {
        return Act.isDev() ? new ActNotImplemented(msg, args) : NotImplemented.of(msg, args);
    }

    public static NotImplemented create(Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActNotImplemented(cause, msg, args) : NotImplemented.of(cause, msg, args);
    }

    public static NotImplemented create(Throwable cause) {
        return Act.isDev() ? new ActNotImplemented(cause) : NotImplemented.of(cause);
    }

    public static NotImplemented create(int code) {
        return Act.isDev() ? new ActNotImplemented(code) : NotImplemented.of(code);
    }

    public static NotImplemented create(int code, String msg, Object... args) {
        return Act.isDev() ? new ActNotImplemented(code, msg, args) : NotImplemented.of(code, msg, args);
    }

    public static NotImplemented create(int code, Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActNotImplemented(code, cause, msg, args) : NotImplemented.of(code, cause, msg, args);
    }

    public static NotImplemented create(int code, Throwable cause) {
        return Act.isDev() ? new ActNotImplemented(code, cause) : NotImplemented.of(code, cause);
    }
}
