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
import act.apidoc.ApiManager;
import act.apidoc.Endpoint;
import act.app.ActionContext;
import act.controller.Controller;
import act.handler.RequestHandler;
import act.handler.builtin.controller.ActionHandlerInvoker;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.ErrorResult;
import org.osgl.mvc.result.NotImplemented;
import org.osgl.mvc.result.Result;

public class ActToBeImplemented extends ErrorResult {

    private static final Logger logger = LogManager.get(ActToBeImplemented.class);

    private ActToBeImplemented() {
        super(H.Status.of(inferStatusCode()));
    }

    private static int inferStatusCode() {
        ActionContext context = ActionContext.current();
        return context.successStatus().code();
    }

    @Override
    protected void applyMessage(H.Request request, H.Response response) {
        ActionContext context = ActionContext.current();
        if ($.bool(context.hasTemplate())) {
            throw createNotImplemented();
        }
        Endpoint endpoint = Act.getInstance(ApiManager.class).endpoint(context.actionPath());
        if (null == endpoint) {
            logger.warn("Cannot locate endpoint for %s", context.actionPath());
            throw createNotImplemented();
        }
        RequestHandler handler = context.handler();
        if (handler instanceof RequestHandlerProxy) {
            RequestHandlerProxy proxy = $.cast(handler);
            ActionHandlerInvoker invoker =  proxy.actionHandler().invoker();
            if (invoker instanceof ReflectedHandlerInvoker) {
                ReflectedHandlerInvoker rfi = $.cast(invoker);
                Result result = Controller.Util.inferResult(rfi.handlerMetaInfo(), endpoint.returnSampleObject, context, false);
                result.apply(request, response);
                return;
            }
        }
        throw createNotImplemented();
    }

    public static ErrorResult create() {
        return Act.isDev() ? new ActToBeImplemented() : createNotImplemented();
    }

    private static ErrorResult createNotImplemented() {
        return ActNotImplemented.create("To be implemented");
    }

}
