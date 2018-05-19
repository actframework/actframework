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
import act.security.CORS;
import act.security.CSRF;
import org.osgl.util.E;

/**
 * Base class to implement handler delegation chain
 */
public class DelegateRequestHandler extends RequestHandlerBase {
    protected RequestHandler handler_;
    private RequestHandler realHandler;
    protected DelegateRequestHandler(RequestHandler handler) {
        E.NPE(handler);
        this.handler_ = handler;
        this.realHandler = handler instanceof RequestHandlerBase ? ((RequestHandlerBase) handler).realHandler() : handler;
    }

    @Override
    public boolean express(ActionContext context) {
        return handler_.express(context);
    }

    @Override
    public boolean skipEvents(ActionContext context) {
        return handler_.skipEvents(context);
    }

    @Override
    public void handle(ActionContext context) {
        handler_.handle(context);
    }

    @Override
    public boolean requireResolveContext() {
        return handler_.requireResolveContext();
    }

    @Override
    public boolean supportPartialPath() {
        return handler_.supportPartialPath();
    }

    protected RequestHandler handler() {
        return handler_;
    }

    @Override
    public CORS.Spec corsSpec() {
        return handler_.corsSpec();
    }

    @Override
    public CSRF.Spec csrfSpec() {
        return handler_.csrfSpec();
    }

    @Override
    public String contentSecurityPolicy() {
        return handler_.contentSecurityPolicy();
    }

    @Override
    public boolean disableContentSecurityPolicy() {
        return handler_.disableContentSecurityPolicy();
    }

    @Override
    public boolean sessionFree() {
        return handler_.sessionFree();
    }

    @Override
    public void prepareAuthentication(ActionContext context) {
        handler_.prepareAuthentication(context);
    }

    @Override
    public RequestHandler realHandler() {
        return realHandler;
    }

    @Override
    protected void releaseResources() {
        handler_.destroy();
    }

    @Override
    public String toString() {
        return handler().toString();
    }
}
