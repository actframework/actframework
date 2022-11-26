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

import act.Act;
import act.app.App;
import act.app.event.SysEventId;
import org.osgl.http.H;
import org.osgl.http.util.Path;
import org.osgl.util.*;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code RouteTableRouterBuilder} take a list of route map definition line and
 * build the router. The line of a route map definition should look like:
 * <p/>
 * <pre>
 * [http-method] [url-path] [action-definition]
 * </pre>
 * <p/>
 * Where http-method could be one of the following:
 * <ul>
 * <li>GET</li>
 * <li>POST</li>
 * <li>PUT</li>
 * <li>DELETE</li>
 * </ul>
 * <p/>
 * url-path defines the incoming URL path, and it could
 * be either static or dynamic. For example,
 * <p/>
 * <pre>
 * # home
 * /
 *
 * # order list
 * /order
 *
 * # (dynamic) access to a certain order by ID
 * /order/{id}
 *
 * # (dynamic) access to a user by ID with regex spec
 * /user/{&lt;[1-9]{5}&gt;id}
 * </pre>
 * <p/>
 * action-definition could be in either built-in action
 * or controller action method.
 * <p/>
 * <p>Built-in action definition should be in a format of
 * <code>[directive]: [payload]</code>, for example
 * </p>
 * <p/>
 * <ul>
 * <li>
 * Echo - write back a text message directly
 * <pre>
 * echo: hello world!
 * </pre>
 * </li>
 * <li>
 * Redirect - send permanent redirect in the response
 * <pre>
 * redirect: http://www.google.com
 * </pre>
 * </li>
 * <li>
 * Static file directory handler - fetch files in a local directory
 * <pre>
 * staticDir: /public
 * </pre>
 * </li>
 * <li>
 * Static file locator - fetch specified file on request
 * <pre>
 * staticFile: /public/js/jquery.js
 * </pre>
 * </li>
 * </ul>
 */
public class RouteTableRouterBuilder implements RouterBuilder {

    private static AtomicInteger jobIdCounter = new AtomicInteger(0);

    public static final String ROUTES_FILE = "routes.conf";
    public static final String ROUTES_FILE2 = "route.conf";

    private List<String> lines;

    public RouteTableRouterBuilder(List<String> lines) {
        E.NPE(lines);
        this.lines = lines;
    }

    public RouteTableRouterBuilder(String... lines) {
        E.illegalArgumentIf(lines.length == 0, "Empty route configuration file lines");
        this.lines = C.listOf(lines);
    }

    @Override
    public void build(Router router) {
        int lineNo = lines.size();
        for (int i = 0; i < lineNo; ++i) {
            String line = lines.get(i).trim();
            if (line.startsWith("#")) continue;
            if (S.blank(line)) continue;
            try {
                process(line, router);
            } catch (final RuntimeException e) {
                if (Act.isDev()) {
                    final App app = router.app();
                    app.jobManager().on(SysEventId.PRE_START, "RouteTableRouterBuilder:setAppBlockIssue-" + jobIdCounter.getAndIncrement(), new Runnable() {
                        @Override
                        public void run() {
                            app.handleBlockIssue(e);
                        }
                    });
                } else {
                    throw e;
                }
            }
        }
    }

    private void process(String line, Router router) {
        Iterator<String> itr = Path.tokenizer(Unsafe.bufOf(line), 0, ' ', '\u0000');
        final String UNKNOWN = S.fmt("route configuration not recognized: %s", line);
        String method = null, path = null;
        S.Buffer action = S.newBuffer();
        if (itr.hasNext()) {
            method = itr.next();
        } else {
            E.illegalArgumentIf(true, UNKNOWN);
        }
        if (itr.hasNext()) {
            path = itr.next();
            if (path.contains("%")) {
                path = Codec.decodeUrl(path);
            }
        } else {
            E.illegalArgumentIf(true, UNKNOWN);
        }
        E.illegalArgumentIf(!itr.hasNext(), UNKNOWN);
        action.append(itr.next());
        while (itr.hasNext()) {
            action.append(" ").append(itr.next());
        }
        if ("*".contentEquals(method)) {
            for (H.Method m : Router.supportedHttpMethods()) {
                router.addMapping(m, path, action.toString(), RouteSource.ROUTE_TABLE);
            }
        } else {
            if ("context".equalsIgnoreCase(method) || "ctx".equalsIgnoreCase(method)) {
                router.addContext(action.toString(), path);
            } else {
                H.Method m = H.Method.valueOfIgnoreCase(method);
                router.addMapping(m, path, action.toString(), RouteSource.ROUTE_TABLE);
            }
        }
    }

}
