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
import org.osgl.mvc.result.Forbidden;

import java.util.List;

import static act.util.ActError.Util.errorMessage;
import static act.util.ActError.Util.loadSourceInfo;
import static org.osgl.http.H.Status.FORBIDDEN;

public class ActForbidden extends Forbidden implements ActError {

    private SourceInfo sourceInfo;

    public ActForbidden() {
        super(errorMessage(FORBIDDEN));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActForbidden.class);
        }
    }

    public ActForbidden(String message, Object... args) {
        super(errorMessage(FORBIDDEN, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActForbidden.class);
        }
    }

    public ActForbidden(Throwable cause, String message, Object ... args) {
        super(cause, errorMessage(FORBIDDEN, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActForbidden.class);
        }
    }

    public ActForbidden(Throwable cause) {
        super(cause, errorMessage(FORBIDDEN));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActForbidden.class);
        }
    }

    public ActForbidden(int code) {
        super(code, errorMessage(FORBIDDEN));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActForbidden.class);
        }
    }

    public ActForbidden(int code, String message, Object... args) {
        super(code, errorMessage(FORBIDDEN, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActForbidden.class);
        }
    }

    public ActForbidden(int code, Throwable cause, String message, Object ... args) {
        super(code, cause, errorMessage(FORBIDDEN, message, args));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActForbidden.class);
        }
    }

    public ActForbidden(int code, Throwable cause) {
        super(code, cause, errorMessage(FORBIDDEN));
        if (Act.isDev()) {
            sourceInfo = loadSourceInfo(ActForbidden.class);
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

    public static Forbidden create(int code) {
        return Act.isDev() ? new ActForbidden(code) : Forbidden.of(code);
    }

    public static Forbidden create(int code, String msg, Object... args) {
        return Act.isDev() ? new ActForbidden(code, msg, args) : Forbidden.of(code, msg, args);
    }

    public static Forbidden create(int code, Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActForbidden(code, cause, msg, args) : Forbidden.of(code, cause, msg, args);
    }

    public static Forbidden create(int code, Throwable cause) {
        return Act.isDev() ? new ActForbidden(code, cause) : Forbidden.of(code, cause);
    }
}
