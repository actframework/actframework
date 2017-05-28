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

public class ActNotImplemented extends NotImplemented implements ActError {

    private SourceInfo sourceInfo;

    public ActNotImplemented() {
        super();
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActNotImplemented(String message, Object... args) {
        super(message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActNotImplemented(Throwable cause, String message, Object ... args) {
        super(cause, message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActNotImplemented(Throwable cause) {
        super(cause);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    private void loadSourceInfo() {
        doFillInStackTrace();
        Throwable cause = getCause();
        sourceInfo = Util.loadSourceInfo(null == cause ? getStackTrace() : cause.getStackTrace(), ActNotImplemented.class);
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
}
