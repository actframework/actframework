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
import act.handler.builtin.AlwaysNotFound;
import act.handler.builtin.UnknownHttpMethodHandler;
import act.route.Router;
import act.security.CORS;
import act.util.LogSupportedDestroyableBase;
import org.osgl.http.H;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Process HTTP OPTIONS request
 */
public class OptionsInfoBase extends LogSupportedDestroyableBase {

    private Router router;
    private ConcurrentMap<String, RequestHandler> handlers = new ConcurrentHashMap<String, RequestHandler>();

    public OptionsInfoBase(Router router) {
        this.router = router;
    }

    public RequestHandler optionHandler(String path, ActionContext context) {
        String s = S.string(path);
        RequestHandler handler = handlers.get(s);
        if (null == handler) {
            RequestHandler newHandler = createHandler(path, context);
            handler = handlers.putIfAbsent(s, newHandler);
            if (null == handler) {
                handler = newHandler;
            } else {
                newHandler.destroy();
            }
        }
        return handler;
    }

    private RequestHandler createHandler(String path, ActionContext context) {
        if (!router.app().config().corsEnabled()) {
            return UnknownHttpMethodHandler.INSTANCE;
        }
        List<H.Method> allowMethods = new ArrayList<>();
        List<CORS.Spec> corsSpecs = new ArrayList<>();
        for (H.Method method: router.supportedHttpMethods()) {
            RequestHandler handler;
            handler = router.getInvoker(method, path, context);
            if (handler instanceof AlwaysNotFound) {
                continue;
            }
            allowMethods.add(method);
            CORS.Spec corsSpec = handler.corsSpec();
            if (corsSpec != CORS.Spec.DUMB) {
                corsSpecs.add(corsSpec);
            }
        }
        if (allowMethods.isEmpty()) {
            return AlwaysNotFound.INSTANCE;
        }
        CORS.Spec corsSpec = CORS.spec(allowMethods);
        for (CORS.Spec spec : corsSpecs) {
            corsSpec = corsSpec.chain(spec);
        }
        return new OptionsRequestHandler(corsSpec);
    }

}
