package act.app;

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

import act.handler.builtin.controller.AfterInterceptor;
import act.handler.builtin.controller.BeforeInterceptor;
import act.handler.builtin.controller.ExceptionInterceptor;
import act.handler.builtin.controller.FinallyInterceptor;
import act.handler.builtin.controller.RequestHandlerProxy.GroupAfterInterceptor;
import act.handler.builtin.controller.RequestHandlerProxy.GroupExceptionInterceptor;
import act.handler.builtin.controller.RequestHandlerProxy.GroupFinallyInterceptor;
import act.handler.builtin.controller.RequestHandlerProxy.GroupInterceptorWithResult;
import org.osgl.mvc.result.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static act.handler.builtin.controller.RequestHandlerProxy.insertInterceptor;

/**
 * Manage interceptors at App level
 */
public class AppInterceptorManager extends AppServiceBase<AppInterceptorManager> {
    private final List<BeforeInterceptor> beforeInterceptors = new ArrayList<>();
    private final List<AfterInterceptor> afterInterceptors = new ArrayList<>();
    private final List<ExceptionInterceptor> exceptionInterceptors = new ArrayList<>();
    private final List<FinallyInterceptor> finallyInterceptors = new ArrayList<>();

    final GroupInterceptorWithResult BEFORE_INTERCEPTOR = new GroupInterceptorWithResult(beforeInterceptors);
    final GroupAfterInterceptor AFTER_INTERCEPTOR = new GroupAfterInterceptor(afterInterceptors);
    final GroupFinallyInterceptor FINALLY_INTERCEPTOR = new GroupFinallyInterceptor(finallyInterceptors);
    final GroupExceptionInterceptor EXCEPTION_INTERCEPTOR = new GroupExceptionInterceptor(exceptionInterceptors);

    AppInterceptorManager(App app) {
        super(app);
    }

    public Result handleBefore(ActionContext actionContext) throws Exception {
        return BEFORE_INTERCEPTOR.apply(actionContext);
    }

    public Result handleAfter(Result result, ActionContext actionContext) throws Exception {
        return AFTER_INTERCEPTOR.apply(result, actionContext);
    }

    public void handleFinally(ActionContext actionContext) throws Exception {
        FINALLY_INTERCEPTOR.apply(actionContext);
    }


    public Result handleException(Exception ex, ActionContext actionContext) throws Exception {
        return EXCEPTION_INTERCEPTOR.apply(ex, actionContext);
    }

    public void registerInterceptor(BeforeInterceptor interceptor) {
        insertInterceptor(beforeInterceptors, interceptor);
    }

    public void registerInterceptor(AfterInterceptor interceptor) {
        insertInterceptor(afterInterceptors, interceptor);
    }

    public void registerInterceptor(FinallyInterceptor interceptor) {
        insertInterceptor(finallyInterceptors, interceptor);
    }

    public void registerInterceptor(ExceptionInterceptor interceptor) {
        insertInterceptor(exceptionInterceptors, interceptor);
        Collections.sort(exceptionInterceptors);
    }

    @Override
    protected void releaseResources() {
        beforeInterceptors.clear();
        afterInterceptors.clear();
        exceptionInterceptors.clear();
        finallyInterceptors.clear();
    }
}
