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
import org.osgl.mvc.result.Conflict;

import java.util.List;

import static act.util.ActError.Util.errorMessage;
import static act.util.ActError.Util.loadSourceInfo;
import static org.osgl.http.H.Status.CONFLICT;

public class ActConflict extends Conflict implements ActError {

    private SourceInfo sourceInfo;

    public ActConflict() {
        super(errorMessage(CONFLICT));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActConflict.class);
        }
    }

    public ActConflict(String message, Object... args) {
        super(errorMessage(CONFLICT, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActConflict.class);
        }
    }

    public ActConflict(Throwable cause, String message, Object ... args) {
        super(cause, errorMessage(CONFLICT, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(cause, ActConflict.class);
        }
    }

    public ActConflict(Throwable cause) {
        super(cause, errorMessage(CONFLICT));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(cause, ActConflict.class);
        }
    }

    public ActConflict(int errorCode) {
        super(errorCode, errorMessage(CONFLICT));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActConflict.class);
        }
    }

    public ActConflict(int errorCode, String message, Object... args) {
        super(errorCode, errorMessage(CONFLICT, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActConflict.class);
        }
    }

    public ActConflict(int errorCode, Throwable cause, String message, Object... args) {
        super(errorCode, cause, errorMessage(CONFLICT, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(cause, ActConflict.class);
        }
    }

    public ActConflict(int errorCode, Throwable cause) {
        super(errorCode, cause, errorMessage(CONFLICT));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(cause, ActConflict.class);
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

    public static Conflict create() {
        return Act.isDev() ? new ActConflict() : Conflict.get();
    }

    public static Conflict create(String msg, Object... args) {
        return Act.isDev() ? new ActConflict(msg, args) : Conflict.of(msg, args);
    }

    public static Conflict create(Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActConflict(cause, msg, args) : Conflict.of(cause, msg, args);
    }

    public static Conflict create(Throwable cause) {
        return Act.isDev() ? new ActConflict(cause) : Conflict.of(cause);
    }


    public static Conflict create(int code) {
        return Act.isDev() ? new ActConflict(code) : Conflict.of(code);
    }

    public static Conflict create(int code, String msg, Object... args) {
        return Act.isDev() ? new ActConflict(code, msg, args) : Conflict.of(code, msg, args);
    }

    public static Conflict create(int code, Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActConflict(code, cause, msg, args) : Conflict.of(code, cause, msg, args);
    }

    public static Conflict create(int code, Throwable cause) {
        return Act.isDev() ? new ActConflict(code, cause) : Conflict.of(code, cause);
    }
}
