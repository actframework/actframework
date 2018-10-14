package act.handler.builtin.controller;

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

import act.Destroyable;
import act.app.ActionContext;
import act.security.CORS;
import act.util.Ordered;
import act.util.Prioritised;
import org.osgl.mvc.result.Result;

public interface ExceptionInterceptorInvoker extends Prioritised, Destroyable, Ordered {
    Result handle(Exception e, ActionContext actionContext) throws Exception;
    void accept(ActionHandlerInvoker.Visitor visitor);
    CORS.Spec corsSpec();
    boolean sessionFree();
    boolean express();
    boolean skipEvents();
}
