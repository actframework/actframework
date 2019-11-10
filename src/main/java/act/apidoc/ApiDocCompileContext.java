package act.apidoc;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import act.route.RouteSource;
import act.route.RoutedContext;

public class ApiDocCompileContext implements RoutedContext {

    private RouteSource routeSource;

    @Override
    public RouteSource routeSource() {
        return routeSource;
    }

    @Override
    public ApiDocCompileContext routeSource(RouteSource source) {
        this.routeSource = source;
        return this;
    }

    public void saveCurrent() {
        current_.set(this);
    }

    public void destroy() {
        current_.remove();
    }

    private static final ThreadLocal<ApiDocCompileContext> current_ = new ThreadLocal<>();

    public static ApiDocCompileContext current() {
        return current_.get();
    }
}
