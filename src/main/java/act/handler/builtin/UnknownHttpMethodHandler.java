package act.handler.builtin;

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
import act.app.ActionContext;
import act.conf.AppConfig;
import act.handler.ExpressHandler;
import act.handler.UnknownHttpMethodProcessor;
import act.handler.builtin.controller.FastRequestHandler;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.view.ActErrorResult;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.Result;

import java.io.Serializable;

public class UnknownHttpMethodHandler extends FastRequestHandler implements Serializable {

    private static Logger logger = LogManager.get(UnknownHttpMethodHandler.class);
    public static final UnknownHttpMethodHandler INSTANCE = new UnknownHttpMethodHandler();

    private volatile UnknownHttpMethodProcessor configured;

    @Override
    public void handle(ActionContext context) {
        H.Method method = context.req().method();
        Result result = configured(context.config()).handle(method);
        try {
            result = RequestHandlerProxy.GLOBAL_AFTER_INTERCEPTOR.apply(result, context);
        } catch (Exception e) {
            logger.error(e, "Error calling global after interceptor");
            result = ActErrorResult.of(e);
        }
        result.apply(context.req(), context.prepareRespForResultEvaluation());
    }

    @Override
    public String toString() {
        return "unknown http method handler: " + configured(Act.appConfig()).getClass().getSimpleName();
    }

    @Override
    public boolean express(ActionContext context) {
        return configured(null) instanceof ExpressHandler;
    }

    @Override
    public boolean skipEvents(ActionContext context) {
        return true;
    }

    private UnknownHttpMethodProcessor configured(AppConfig config) {
        if (null == configured) {
            synchronized (this) {
                if (null == configured) {
                    if (null == config) {
                        config = Act.appConfig();
                    }
                    configured = config.unknownHttpMethodProcessor();
                }
            }
        }
        return configured;
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
