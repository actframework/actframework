package act.apidoc;

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
import act.app.App;
import act.app.AppServiceBase;
import act.app.event.SysEventId;
import act.app.util.NamedPort;
import act.conf.AppConfig;
import act.handler.RequestHandler;
import act.handler.RequestHandlerBase;
import act.handler.builtin.ResourceGetter;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.route.Router;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;

import java.util.SortedSet;
import java.util.TreeSet;

import static act.controller.Controller.Util.renderJson;

/**
 * Keep track endpoints defined in the system
 */
public class ApiManager extends AppServiceBase<ApiManager> {

    static final Logger LOGGER = LogManager.get(ApiManager.class);

    /**
     * The {@link Endpoint} defined in the system
     */
    SortedSet<Endpoint> endpoints = new TreeSet<>();


    public ApiManager(final App app) {
        super(app);
        if (!app.config().apiDocEnabled()) {
            return;
        }
        app.jobManager().alongWith(SysEventId.POST_START, "compile-api-book", new Runnable() {
            @Override
            public void run() {
                load(app);
            }
        });
        app.router().addMapping(H.Method.GET, "/~/apidoc/endpoint", new GetEndpointsHandler(this));
        app.router().addMapping(H.Method.GET, "/~/apidoc", new ResourceGetter("asset/~act/apibook/index.html"));
    }

    @Override
    protected void releaseResources() {
        endpoints.clear();
    }

    public void load(App app) {
        Act.LOGGER.info("start compiling API book");
        Router router = app.router();
        AppConfig config = app.config();
        load(router, null, config);
        for (NamedPort port : app.config().namedPorts()) {
            router = app.router(port);
            load(router, port, config);
        }
    }

    private void load(Router router, NamedPort port, AppConfig config) {
        final int portNumber = null == port ? config.httpExternalPort() : port.port();
        router.accept(new Router.Visitor() {
            @Override
            public void visit(H.Method method, String path, RequestHandler handler) {
                if ((handler instanceof RequestHandlerProxy)) {
                    endpoints.add(new Endpoint(portNumber, method, path, handler));
                }
            }
        });
    }

    private class GetEndpointsHandler extends RequestHandlerBase {

        private ApiManager api;

        public GetEndpointsHandler(ApiManager api) {
            this.api = api;
        }

        @Override
        public void handle(ActionContext context) {
            renderJson(api.endpoints).apply(context.req(), context.prepareRespForWrite());
        }

        @Override
        public void prepareAuthentication(ActionContext context) {

        }

        @Override
        public String toString() {
            return "API doc handler";
        }
    }


}
