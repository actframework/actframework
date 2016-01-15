package act.route;

import act.app.App;
import act.cli.Command;
import act.cli.Optional;
import act.cli.tree.TreeNode;
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
            @Optional("specify the port name") String name
    ) {
        final Router router = S.blank(name) ? app.router() : app.router(name);
        if (tree) {
            return new TreeNode() {
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
        } else {
            return router.debug();
        }
    }

}
