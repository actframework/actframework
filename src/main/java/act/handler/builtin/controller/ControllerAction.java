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
import act.controller.CacheSupportMetaInfo;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import act.security.CORS;
import act.security.CSRF;
import act.util.MissingAuthenticationHandler;
import act.view.ActBadRequest;
import act.view.ActNotFound;
import org.osgl.mvc.result.BadRequest;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Result;

/**
 * Dispatch request to real controller action method
 */
public class ControllerAction extends ActionHandler<ControllerAction> {

    private ActionHandlerInvoker handlerInvoker;

    public ControllerAction(ActionHandlerInvoker invoker) {
        super(-1);
        this.handlerInvoker = invoker;
    }

    public CacheSupportMetaInfo cacheSupport() {
        return handlerInvoker.cacheSupport();
    }

    @Override
    public Result handle(ActionContext actionContext) throws Exception {
        return handlerInvoker.handle(actionContext);
    }

    public NotFound notFoundOnMethod(String message) {
        if (handlerInvoker instanceof ReflectedHandlerInvoker) {
            return ((ReflectedHandlerInvoker) handlerInvoker).notFoundOnMethod(message);
        }
        return ActNotFound.create();
    }

    public BadRequest badRequestOnMethod(String message) {
        if (handlerInvoker instanceof ReflectedHandlerInvoker) {
            return ((ReflectedHandlerInvoker) handlerInvoker).badRequestOnMethod(message);
        }
        return ActBadRequest.create();
    }

    @Override
    public CORS.Spec corsSpec() {
        return handlerInvoker.corsSpec();
    }

    public CSRF.Spec csrfSpec() {
        return handlerInvoker.csrfSpec();
    }

    @Override
    public boolean sessionFree() {
        return handlerInvoker.sessionFree();
    }

    public MissingAuthenticationHandler missingAuthenticationHandler() {
        return handlerInvoker.missingAuthenticationHandler();
    }

    public MissingAuthenticationHandler csrfFailureHandler() {
        return handlerInvoker.csrfFailureHandler();
    }

    @Override
    public boolean express() {
        return handlerInvoker.express();
    }

    @Override
    public void accept(Visitor visitor) {
        handlerInvoker.accept(visitor.invokerVisitor());
    }

    @Override
    protected void releaseResources() {
        handlerInvoker.destroy();
        handlerInvoker = null;
    }
}
