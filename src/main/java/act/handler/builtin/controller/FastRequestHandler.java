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

import act.app.ActionContext;
import act.handler.RequestHandler;
import act.handler.RequestHandlerBase;
import act.security.CSRF;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.E;

/**
 * For any handler that does not require the framework to lookup incoming request
 * and construct the sessions, it shall extends from this class
 */
public abstract class FastRequestHandler extends RequestHandlerBase {

    protected Logger logger = LogManager.get(getClass());

    public static final RequestHandler DUMB = new FastRequestHandler() {
        @Override
        public void handle(ActionContext context) {
        }

        @Override
        public boolean express(ActionContext context) {
            return true;
        }
    };

    @Override
    public boolean requireResolveContext() {
        return false;
    }

    @Override
    public CSRF.Spec csrfSpec() {
        return CSRF.Spec.DUMB;
    }

    @Override
    public boolean sessionFree() {
        return true;
    }

    @Override
    public void prepareAuthentication(ActionContext context) {
        throw E.unsupport();
    }
}
