package act.route;

import act.app.App;
import act.cli.Command;
import act.cli.Optional;
import act.cli.tree.FilteredTreeNode;
import act.cli.tree.TreeNode;
import act.cli.tree.TreeNodeFilter;
import act.util.PropertySpec;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.inject.Inject;
import java.util.List;

/**
 * An admin interface to Act application router
 */
public class RouterAdmin {

    private App app;

    @Inject
    public RouterAdmin(App app) {
        this.app = app;
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
            List<RouteInfo> list = router.debug();
            if (S.notBlank(q)) {
                List<RouteInfo> toBeRemoved = C.newList();
                for (RouteInfo info: list) {
                    if (!info.path().matches(q)) {
                        toBeRemoved.add(info);
                    }
                }
                list = C.list(list).without(toBeRemoved);
            }
            return list;
        }
    }

}
