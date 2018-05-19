package act.route;

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
import act.handler.RequestHandler;
import act.handler.RequestHandlerBase;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * Used to expose Router table for debugging purpose
 */
public class RouteInfo extends $.T3<String, String, String> implements Comparable<RouteInfo> {
    private RouteSource routeSource;
    public RouteInfo(H.Method method, String path, RequestHandler handler) {
        super(method.name(), path, handler.toString());
    }
    public RouteInfo(H.Method method, String path, RequestHandler handler, RouteSource routeSource) {
        this(method, path, handler);
        this.routeSource = $.requireNotNull(routeSource);
    }
    public String method() {
        return _1;
    }
    public String path() {
        return _2;
    }
    public String handler() {
        return _3;
    }
    public String compactHandler() {
        return compactHandler(_3);
    }

    public static String compactHandler(String handler) {
        String[] sa = handler.split("\\.");
        int len = sa.length;
        if (len == 1) {
            return handler;
        }
        S.Buffer sb = S.newBuffer();
        for (int i = 0; i < len - 2; ++i) {
            sb.append(sa[i].charAt(0)).append('.');
        }
        sb.append(sa[len - 2]).append('.').append(sa[len - 1]);
        return sb.toString();
    }

    @Override
    public String toString() {
        return null != routeSource ? S.fmt("[%s %s] -> [%s] added by %s", method(), path(), compactHandler(), routeSource.getDescription()) :
                S.fmt("[%s %s] -> [%s]", method(), path(), compactHandler());
    }

    @Override
    public int compareTo(RouteInfo routeInfo) {
        int n = path().compareTo(routeInfo.path());
        if (n != 0) {
            return n;
        }
        n = method().compareTo(routeInfo.method());
        if (n != 0) {
            return n;
        }
        return handler().compareTo(routeInfo.handler());
    }

    // used by Error template
    public static RouteInfo of(ActionContext context) {
        H.Method m = context.req().method();
        String path = context.req().url();
        RequestHandler handler = context.handler();
        if (null == handler) {
            handler = UNKNOWN_HANDLER;
        }
        return new RouteInfo(m, path, handler);
    }

    public static final RequestHandler UNKNOWN_HANDLER = new RequestHandlerBase() {
        @Override
        public void handle(ActionContext context) {
            throw E.unsupport();
        }

        @Override
        public boolean express(ActionContext context) {
            return true;
        }

        @Override
        public boolean skipEvents(ActionContext context) {
            return true;
        }

        @Override
        public String toString() {
            return "unknown";
        }

        @Override
        public boolean sessionFree() {
            return true;
        }

        @Override
        public void prepareAuthentication(ActionContext context) {
            throw E.unsupport();
        }
    };
}
