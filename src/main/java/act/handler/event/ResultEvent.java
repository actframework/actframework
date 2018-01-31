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

import act.app.ActionContext;
import act.event.ActEvent;
import act.event.SystemEvent;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.mvc.result.Result;

public abstract class ResultEvent extends ActEvent<Result> implements SystemEvent {

    private final H.Request req;
    private final H.Response resp;


    public ResultEvent(Result result, H.Request req, H.Response resp) {
        super(result);
        this.req = $.notNull(req);
        this.resp = $.notNull(resp);
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
                public Void apply(Result result, H.Request<?> request, H.Response<?> response) throws NotAppliedException, Osgl.Break {
                    ActionContext context = request.context();
                    context.applyCorsSpec().applyContentSecurityPolicy().applyContentType(result);
                    context.app().eventBus().emit(new BeforeResultCommit(result, request, response));
                    return null;
                }
            };


    public static final $.Func3<Result, H.Request<?>, H.Response<?>, Void> AFTER_COMMIT_HANDLER =
            new $.F3<Result, H.Request<?>, H.Response<?>, Void>() {
                @Override
                public Void apply(Result result, H.Request<?> request, H.Response<?> response) throws NotAppliedException, Osgl.Break {
                    ActionContext context = request.context();
                    context.logAccess(response);
                    context.app().eventBus().emit(new AfterResultCommit(result, request, response));
                    return null;
                }
            };

}
