package act.util;

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
import act.view.ActUnauthorized;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.result.Unauthorized;

/**
 * When authentication is required but missing, response with
 * {@link org.osgl.mvc.result.Unauthorized}
 */
public class ReturnUnauthorized implements MissingAuthenticationHandler {
    private static Result R = Unauthorized.get();

    @Override
    public Result result(ActionContext context) {
        return Act.isDev() ? new ActUnauthorized() : R;
    }

    @Override
    public void handle(ActionContext context) {
        throw result(context);
    }

    static Result result() {
        return Act.isDev() ? new ActUnauthorized() : R;
    }


}
