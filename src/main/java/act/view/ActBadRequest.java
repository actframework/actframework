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
import org.osgl.util.S;

import java.lang.reflect.Method;
import java.util.List;

public class ActBadRequest extends BadRequest implements ActError {

    private SourceInfo sourceInfo;

    public ActBadRequest() {
        super();
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActBadRequest(String message, Object... args) {
        super(message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActBadRequest(Method method, String message, Object... args) {
        super(null == message ? S.fmt("bad request on invoking %s.%s()", method.getDeclaringClass().getName(), method.getName()) : message);
        if (Act.isDev()) {
            loadSourceInfo(method);
        }
    }

    public ActBadRequest(Throwable cause, String message, Object ... args) {
        super(cause, message, args);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActBadRequest(Throwable cause) {
        super(cause);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    private void loadSourceInfo() {
        doFillInStackTrace();
        Throwable cause = getCause();
        sourceInfo = Util.loadSourceInfo(null == cause ? getStackTrace() : cause.getStackTrace(), ActBadRequest.class);
    }

    private void loadSourceInfo(Method method) {
        sourceInfo = Util.loadSourceInfo(method);
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
        return Act.isDev() ? new ActBadRequest(msg, args) : BadRequest.of
                (msg, args);
    }

    public static BadRequest create(Method method, String message) {
        return Act.isDev() ? new ActBadRequest(method, message) : BadRequest.get();
    }



    public static BadRequest create(Throwable cause, String msg, Object ... args) {
        return Act.isDev() ? new ActBadRequest(cause, msg, args) : new BadRequest(cause, msg, args);
    }

    public static BadRequest create(Throwable cause) {
        return Act.isDev() ? new ActBadRequest(cause) : new BadRequest(cause);
    }
}
