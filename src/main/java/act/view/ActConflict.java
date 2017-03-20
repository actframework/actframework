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

public class ActConflict extends Conflict implements ActError {

    private SourceInfo sourceInfo;

    public ActConflict() {
        super();
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActConflict(String message, Object... args) {
        super(message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActConflict(Throwable cause, String message, Object ... args) {
        super(cause, message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActConflict(Throwable cause) {
        super(cause);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    private void loadSourceInfo() {
        doFillInStackTrace();
        Throwable cause = getCause();
        sourceInfo = Util.loadSourceInfo(null == cause ? getStackTrace() : cause.getStackTrace(), ActConflict.class);
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
}
