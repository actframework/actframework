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

import act.app.App;
import act.cli.*;
import act.cli.tree.FilteredTreeNode;
import act.cli.tree.TreeNodeFilter;
import act.util.PropertySpec;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.List;

/**
 * An admin interface to Act application router
 */
@SuppressWarnings("unused")
public class RouterAdmin {

    private App app;
    private CliContext context;

    public RouterAdmin() {
        this.app = App.instance();
        this.context = CliContext.current();
    }

    @Command(name = "act.route.list, act.route.print, act.route, act.routes", help = "list routes")
    @PropertySpec("method,path,compactHandler")
    public Object listRoutes(
            @Optional("list routes in tree view") boolean tree,
            @Optional("specify the port name") String name,
            @Optional("specify route filter") String q
    ) {
        final Router router = S.blank(name) ? app.router() : app.router(name);
        if (tree) {
            return S.blank(q) ? router : new FilteredTreeNode(router, TreeNodeFilter.Common.pathMatches(q));
        } else {
            return routeInfoList(name, q);
        }
    }

    private List<RouteInfo> routeInfoList(String portName, String q) {
        final Router router = S.blank(portName) ? app.router() : app.router(portName);
        List<RouteInfo> list = router.debug();
        if (S.notBlank(q)) {
            List<RouteInfo> toBeRemoved = new ArrayList<>();
            for (RouteInfo info: list) {
                String handler = S.string(info.handler());
                String path = info.path();
                if (path.contains(q) || handler.contains(q) || path.matches(q) || handler.matches(q)) {
                    continue;
                }
                toBeRemoved.add(info);
            }
            list = C.list(list).without(toBeRemoved);
        }
        return list;
    }

    @Command(name = "act.route.overwrite", help = "overwrite a route entry")
    public void overwrite(
            @Required("specify http method") String method,
            @Required("specify path") String path,
            @Required("specify handler") String handler,
            @Optional("specify the port name") String name
    ) {
        final Router router = S.blank(name) ? app.router() : app.router(name);
        router.addMapping(H.Method.valueOfIgnoreCase(method), path, handler, RouteSource.ADMIN_OVERWRITE);
        context.println("route entry has been added/overwritten");
    }

    @Command(name = "act.route.add", help = "add a route entry")
    public void add(
            @Required("specify http method") String method,
            @Required("specify URL path") String path,
            @Required("specify handler") String handler,
            @Optional("specify the port name") String name
    ) {
        final Router router = S.blank(name) ? app.router() : app.router(name);
        try {
            router.addMapping(H.Method.valueOfIgnoreCase(method), path, handler, RouteSource.ADMIN_ADD);
            context.println("route entry has been added");
        } catch (DuplicateRouteMappingException e) {
            context.println("Route entry already exist");
        }
    }

    @Command(name = "act.route.echo", help = "Add a temporary echo route")
    public void echo(
            @Required("specify URL path") String path,
            @Required("specify the code to echo back") String code,
            @Optional("specify the port name") String name
    ) {
        final Router router = S.blank(name) ? app.router() : app.router(name);
        try {
            router.addMapping(H.Method.GET, path, "echo:" + code, RouteSource.ADMIN_ADD);
            context.println("route entry has been added");
        } catch (DuplicateRouteMappingException e) {
            context.println("Route entry already exist");
        }
    }

}
