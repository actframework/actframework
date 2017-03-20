package act.app;

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
import act.exception.ActException;

/**
 * Application error
 */
public abstract class ActAppException extends ActException {

    public ActAppException() {
    }

    public ActAppException(String message) {
        super(message);
    }

    public ActAppException(String message, Object... args) {
        super(message, args);
    }

    public ActAppException(Throwable cause) {
        super(cause);
    }

    public ActAppException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }

    public abstract String getErrorTitle();

    public abstract String getErrorDescription();


    public static StackTraceElement getInterestingStackTraceElement(App app, Throwable cause) {
        if (!Act.isDev()) {
            return null;
        }
        AppClassLoader classLoader = app.classLoader();
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            if (stackTraceElement.getLineNumber() > 0 && classLoader.isSourceClass(stackTraceElement.getClassName())) {
                return stackTraceElement;
            }
        }
        return null;
    }
}
