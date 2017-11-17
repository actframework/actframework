package act.util;

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

import act.app.ActionContext;
import org.osgl.mvc.result.Result;

/**
 * How the framework should respond to request missing authentication
 * while it is required or a request failure to pass CSRF checking
 */
public interface MissingAuthenticationHandler {
    /**
     * The result to be thrown out when authentication is missing.
     *
     * This method is deprecated. Please use {@link #handle(ActionContext)}
     * instead.
     */
    @Deprecated
    Result result(ActionContext context);

    /**
     * Throw out a {@link Result} when authentication is missing.
     *
     * @param context
     *      The action context
     */
    void handle(ActionContext context);
}
