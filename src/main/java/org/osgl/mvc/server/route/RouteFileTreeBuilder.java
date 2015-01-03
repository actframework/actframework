package org.osgl.mvc.server.route;

import org.osgl.http.H;
import org.osgl.http.util.Path;
import org.osgl.mvc.server.action.ActionInvokerResolver;
import org.osgl.util.*;

import java.util.List;

public class RouteFileTreeBuilder implements TreeBuilder {

    private List<String> lines;
    private ActionInvokerResolver invokerResolver;

    public RouteFileTreeBuilder(List<String> lines) {
        E.NPE(lines);
        this.lines = lines;
    }

    public RouteFileTreeBuilder(String... lines) {
        E.illegalArgumentIf(lines.length == 0, "Empty route configuration file lines");
        this.lines = C.listOf(lines);
    }

    public void setInvokerResolver(ActionInvokerResolver resolver) {
        E.NPE(resolver);
        this.invokerResolver = resolver;
    }

    @Override
    public void build(Tree tree) {
        int lineNo = lines.size();
        for (int i = 0; i < lineNo; ++i) {
            String line = lines.get(i).trim();
            if (line.startsWith("#")) continue;
            if (S.empty(line)) continue;
            process(line, tree);
        }
    }

    private void process(String line, Tree tree) {
        List<CharSequence> fields = Path.tokenize(Unsafe.bufOf(line), 0, ' ', '\u0000');
        E.illegalArgumentIf(fields.size() != 3, "route configuration not recognized: %s", line);
        CharSequence method = fields.get(0), path = fields.get(1), action = fields.get(2);
        if ("*".contentEquals(method)) {
            process(path, action, tree.root(H.Method.GET));
            process(path, action, tree.root(H.Method.POST));
            process(path, action, tree.root(H.Method.PUT));
            process(path, action, tree.root(H.Method.DELETE));
        } else {
            Tree.Node node = null;
            H.Method m = H.Method.valueOfIgnoreCase(method.toString().trim());
            node = tree.root(m);
            E.illegalArgumentIf(null == node, "Http method %s not supported", method);
            process(path, action, node);
        }
    }

    private void process(CharSequence path, CharSequence action, Tree.Node node) {
        FastStr fsPath = FastStr.of(path);
        if (fsPath.trim().contentEquals("/")) {
            node.setInvoker(invokerResolver.resolve(action));
            return;
        }
        List<CharSequence> paths = Path.tokenize(fsPath.unsafeChars());
        int len = paths.size();
        for (int i = 0; i < len - 1; ++i) {
            node = node.addChild((StrBase)paths.get(i));
        }
        node = node.addChild((StrBase) paths.get(len - 1));
        node.setInvoker(invokerResolver.resolve(action));
    }
}
