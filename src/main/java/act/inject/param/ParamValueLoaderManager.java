package act.inject.param;

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
import act.app.App;
import act.app.AppServiceBase;
import act.cli.CliContext;
import act.job.JobContext;
import act.util.ActContext;
import act.ws.WebSocketContext;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Manage {@link ParamValueLoaderService} by context type
 */
@ApplicationScoped
public class ParamValueLoaderManager  extends AppServiceBase<ParamValueLoaderManager> {

    private final Map<Class<? extends ActContext>, ParamValueLoaderService> loaderServices = new HashMap<Class<? extends ActContext>, ParamValueLoaderService>();

    @Inject
    public ParamValueLoaderManager(App app) {
        super(app);
        loaderServices.put(ActionContext.class, new ActionContextParamLoader(app));
        loaderServices.put(CliContext.class, new CliContextParamLoader(app));
        loaderServices.put(JobContext.class, new JobContextParamLoader(app));
        loaderServices.put(WebSocketContext.class, new WebSocketContextParamLoader(app));
    }

    public <T extends ParamValueLoaderService> T  get(Class<? extends ActContext> contextClass) {
        return (T) loaderServices.get(contextClass);
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.destroyAll(loaderServices.values(), ApplicationScoped.class);
    }
}
