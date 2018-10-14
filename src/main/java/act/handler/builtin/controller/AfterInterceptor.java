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
import act.plugin.Plugin;
import org.osgl.mvc.result.Result;

/**
 * Intercept request handling before calling to action method
 */
public abstract class AfterInterceptor
        extends Handler<AfterInterceptor>
        implements Plugin, AfterInterceptorInvoker {

    protected AfterInterceptor(Integer priority) {
        super(priority);
    }

    /**
     * Sub class should implement this method to do further processing on the
     * result and return the processed result, or return a completely new result
     *
     * @param result     the generated result by either {@link BeforeInterceptor} or
     *                   controller action handler method
     * @param actionContext
     * @return the new result been processed
     */
    @Override
    public Result handle(Result result, ActionContext actionContext) throws Exception {
        return result;
    }

    @Override
    public void register() {
        RequestHandlerProxy.registerGlobalInterceptor(this);
    }
}
