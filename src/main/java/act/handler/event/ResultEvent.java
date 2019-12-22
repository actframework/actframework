package act.handler.event;

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
import act.event.ActEvent;
import act.event.SystemEvent;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.mvc.result.ErrorResult;
import org.osgl.mvc.result.Result;

public abstract class ResultEvent extends ActEvent<Result> implements SystemEvent {

    private final H.Request req;
    private final H.Response resp;


    public ResultEvent(Result result, H.Request req, H.Response resp) {
        super(result);
        this.req = $.requireNotNull(req);
        this.resp = $.requireNotNull(resp);
    }

    public Result result() {
        return source();
    }

    public H.Request request() {
        return req;
    }

    public H.Response response() {
        return resp;
    }

    public static final $.Func3<Result, H.Request<?>, H.Response<?>, Void> BEFORE_COMMIT_HANDLER =
            new $.F3<Result, H.Request<?>, H.Response<?>, Void>() {
                @Override
                public Void apply(Result result, H.Request<?> request, H.Response<?> response) throws NotAppliedException, $.Break {
                    ActionContext context = request.context();
                    try {
                        context.applyCorsSpec().applyContentSecurityPolicy().applyContentType(result);
                        if (!context.skipEvents()) {
                            context.app().eventBus().emit(new BeforeResultCommit(result, request, response));
                        }
                    } catch (RuntimeException e) {
                        if (Act.isProd()) {
                            throw e;
                        }
                        if (!(result instanceof ErrorResult)) {
                            throw e;
                        }
                        // it might happens in rare case
                        // refer https://github.com/actframework/actframework/issues/1264
                        // however we don't need to raise it or log it as it
                        // is already logged in somewhere else
                        // we just want to ensure the error page can be generated.
                    }
                    return null;
                }
            };


    public static final $.Func3<Result, H.Request<?>, H.Response<?>, Void> AFTER_COMMIT_HANDLER =
            new $.F3<Result, H.Request<?>, H.Response<?>, Void>() {
                @Override
                public Void apply(Result result, H.Request<?> request, H.Response<?> response) throws NotAppliedException, $.Break {
                    ActionContext context = request.context();
                    context.logAccess(response);
                    try {
                        if (!context.skipEvents()) {
                            context.app().eventBus().emit(new AfterResultCommit(result, request, response));
                        }
                    } catch (RuntimeException e) {
                        if (Act.isProd()) {
                            throw e;
                        }
                        if (!(result instanceof ErrorResult)) {
                            throw e;
                        }
                        // it might happens in rare case
                        // refer https://github.com/actframework/actframework/issues/1264
                        // however we don't need to raise it or log it as it
                        // is already logged in somewhere else
                        // we just want to ensure the error page can be generated.
                    }
                    return null;
                }
            };

}
