package act.route;

import act.app.App;
import act.app.CliContext;
import act.cli.Command;
import act.cli.Optional;
import act.cli.Required;
import act.cli.tree.FilteredTreeNode;
import act.cli.tree.TreeNode;
import act.cli.tree.TreeNodeFilter;
import act.util.PropertySpec;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.inject.Inject;
import java.util.List;

/**
 * An admin interface to Act application router
 */
@SuppressWarnings("unused")
public class RouterAdmin {

    private App app;
    private CliContext context;

    @Inject
    public RouterAdmin(
            App app,
            CliContext context
    ) {
        this.app = app;
        this.context = context;
    }

    @Command(name = "act.route.list", help = "list routes")
    @PropertySpec("method,path,handler")
    public Object listRoutes(
            @Optional("list routes in tree view") boolean tree,
            @Optional("specify the port name") String name,
            @Optional("specify route filter") String q
    ) {
        final Router router = S.blank(name) ? app.router() : app.router(name);
        if (S.notBlank(q)) {
            if (q.contains(".") || q.contains("[") || q.contains("*")) {
                // already regex
            } else {
                // make it a regex
                q = ".*" + q + ".*";
            }
        }
        if (tree) {
            TreeNode root = new TreeNode() {

                @Override
                public String id() {
                    return "root";
                }

                @Override
                public String label() {
                    return "Router";
                }

                @Override
                public List<TreeNode> children() {
                    List<TreeNode> l = C.newList();
                    l.add(router._GET);
                    l.add(router._POST);
                    l.add(router._PUT);
                    l.add(router._DEL);
                    return l;
                }
            };
            return S.blank(q) ? root : new FilteredTreeNode(root, TreeNodeFilter.Common.pathMatches(q));
        } else {
            return routeInfoList(name, q);
        }
    }

    private List<RouteInfo> routeInfoList(String portName, String q) {
        final Router router = S.blank(portName) ? app.router() : app.router(portName);
        List<RouteInfo> list = router.debug();
        if (S.notBlank(q)) {
            List<RouteInfo> toBeRemoved = C.newList();
            for (RouteInfo info: list) {
                if (info.path().matches(q) || S.string(info.handler()).matches(q)) {
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

    @Command(name = "act.route.add", help = "overwrite a route entry")
    public void admin(
            @Required("specify http method") String method,
            @Required("specify path") String path,
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

}
