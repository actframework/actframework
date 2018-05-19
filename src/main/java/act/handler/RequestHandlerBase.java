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
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;

import java.lang.annotation.Annotation;
import javax.enterprise.context.ApplicationScoped;

public abstract class RequestHandlerBase extends $.F1<ActionContext, Void> implements RequestHandler {

    protected Logger logger = LogManager.get(getClass());

    private boolean destroyed;
    private boolean sessionFree;
    private boolean requireContextResolving;
    private boolean express;

    public RequestHandlerBase() {
        this.express = this instanceof ExpressHandler;
        this.sessionFree = false;
        this.requireContextResolving = true;
    }

    @Override
    public final Void apply(ActionContext context) throws NotAppliedException, $.Break {
        handle(context);
        return null;
    }

    public RequestHandlerBase setExpress() {
        this.express = true;
        return this;
    }

    @Override
    public boolean express(ActionContext context) {
        return express;
    }

    /**
     * By default an {@link #express(ActionContext) express} handler will
     * skip result commit events triggering.
     *
     * @param context the action context.
     * @return result of {@link #express(ActionContext)}
     */
    @Override
    public boolean skipEvents(ActionContext context) {
        return express;
    }

    @Override
    public final Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
    }

    @Override
    public boolean supportPartialPath() {
        return false;
    }

    @Override
    public boolean requireResolveContext() {
        return requireContextResolving;
    }

    public RequestHandlerBase noContextResoving() {
        requireContextResolving = false;
        return this;
    }

    public RequestHandler realHandler() {
        return this;
    }

    public RequestHandlerBase setSessionFree() {
        this.sessionFree = true;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    @Override
    public boolean sessionFree() {
        return sessionFree;
    }

    @Override
    public CORS.Spec corsSpec() {
        return CORS.Spec.DUMB;
    }

    @Override
    public CSRF.Spec csrfSpec() {
        return CSRF.Spec.DUMB;
    }

    @Override
    public String contentSecurityPolicy() {
        return null;
    }

    @Override
    public boolean disableContentSecurityPolicy() {
        return false;
    }

    @Override
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        releaseResources();
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {}

    public static RequestHandlerBase wrap(final SimpleRequestHandler handler) {
        return new RequestHandlerBase() {
            @Override
            public void handle(ActionContext context) {
                handler.handle(context);
            }

            @Override
            public void prepareAuthentication(ActionContext context) {
            }

            @Override
            public String toString() {
                return handler.toString();
            }
        }.setSessionFree().noContextResoving();
    }
}
