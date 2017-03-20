package act.handler;

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

/**
 * A ｀SimpleRequestHandler` is treated as a {@link RequestHandler} with the following
 * default properties:
 *
 * * {@link RequestHandler#express()}: `true`
 * * {@link RequestHandler#corsSpec()}: {@link act.security.CORS.Spec#DUMB}
 * * {@link RequestHandler#csrfSpec()}: {@link act.security.CSRF.Spec#DUMB}
 * * {@link RequestHandler#sessionFree()}: `true`
 * * {@link RequestHandler#requireResolveContext()}: `false`
 * * {@link RequestHandler#supportPartialPath()}: `false`
 */
public interface SimpleRequestHandler {
    /**
     * Invoke handler upon an action context
     *
     * @param context the context data
     */
    void handle(ActionContext context);
}
