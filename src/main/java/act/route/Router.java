package act.route;

import act.ActComponent;
import act.Destroyable;
import act.app.ActAppException;
import act.app.ActionContext;
import act.app.App;
import act.app.AppServiceBase;
import act.cli.tree.TreeNode;
import act.conf.AppConfig;
import act.controller.ParamNames;
import act.handler.*;
import act.handler.builtin.Echo;
import act.handler.builtin.Redirect;
import act.handler.builtin.StaticFileGetter;
import act.handler.builtin.UnknownHttpMethodHandler;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.http.util.Path;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Result;
import org.osgl.util.*;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

@ActComponent
public class Router extends AppServiceBase<Router> {

    private static final NotFound NOT_FOUND = NotFound.INSTANCE;
    private static final H.Method[] targetMethods = new H.Method[]{
            H.Method.GET, H.Method.POST, H.Method.DELETE, H.Method.PUT};
    private static final Logger logger = L.get(Router.class);

    Node _GET = Node.newRoot("GET");
    Node _PUT = Node.newRoot("PUT");
    Node _POST = Node.newRoot("POST");
    Node _DEL = Node.newRoot("DELETE");

    private Map<String, RequestHandlerResolver> resolvers = C.newMap();

    private RequestHandlerResolver handlerLookup;
    private C.Set<String> actionNames = C.newSet();
    private AppConfig appConfig;
    private String portId;

    private void initControllerLookup(RequestHandlerResolver lookup) {
        if (null == lookup) {
            lookup = new RequestHandlerResolverBase() {
                @Override
                public RequestHandler resolve(CharSequence payload, App app) {
                    return new RequestHandlerProxy(payload.toString(), app);
                }
            };
        }
        handlerLookup = lookup;
    }

    public Router(App app) {
        this(null, app, null);
    }

    public Router(App app, String portId) {
        this(null, app, portId);
    }

    public Router(RequestHandlerResolver handlerLookup, App app) {
        this(handlerLookup, app, null);
    }

    public Router(RequestHandlerResolver handlerLookup, App app, String portId) {
        super(app);
        initControllerLookup(handlerLookup);
        this.appConfig = app.config();
        this.portId = portId;
    }

    @Override
    protected void releaseResources() {
        _GET.destroy();
        _DEL.destroy();
        _POST.destroy();
        _PUT.destroy();
        handlerLookup.destroy();
        actionNames.clear();
        appConfig = null;
    }

    public String portId() {
        return portId;
    }

    // --- routing ---
    public RequestHandler getInvoker(H.Method method, CharSequence path, ActionContext context) {
        if (Arrays.binarySearch(targetMethods, method) < 0) {
            return UnknownHttpMethodHandler.INSTANCE;
        }
        context.router(this);
        Node node = search(method, Path.tokenizer(Unsafe.bufOf(path)), context);
        return getInvokerFrom(node);
    }

    public RequestHandler getInvoker(H.Method method, List<CharSequence> path, ActionContext context) {
        context.router(this);
        Node node = search(method, path, context);
        return getInvokerFrom(node);
    }

    private RequestHandler getInvokerFrom(Node node) {
        if (null == node) {
            throw notFound();
        }
        RequestHandler handler = node.handler;
        if (null == handler) {
            if (null != node.dynamicChild) {
                node = node.dynamicChild;
                if (null != node.pattern) {
                    if (node.pattern.matcher("").matches()) {
                        return node.handler;
                    } else {
                        throw notFound();
                    }
                } else {
                    return node.handler;
                }
            }
            throw notFound();
        }
        return handler;
    }

    // --- route building ---
    static enum ConflictResolver {
        /**
         * Overwrite existing route
         */
        OVERWRITE,

        /**
         * Overwrite and log warn message
         */
        OVERWRITE_WARN,

        /**
         * Skip the new route
         */
        SKIP,

        /**
         * Report error and exit app
         */
        EXIT
    }

    public void addMapping(H.Method method, CharSequence path, CharSequence action) {
        addMapping(method, path, resolveActionHandler(action), RouteSource.ROUTE_TABLE);
    }

    public void addMapping(H.Method method, CharSequence path, CharSequence action, RouteSource source) {
        addMapping(method, path, resolveActionHandler(action), source);
    }

    public void addMapping(H.Method method, CharSequence path, RequestHandler handler, RouteSource source) {
        Node node = _locate(method, path);
        if (null == node.handler) {
            logger.debug(routeInfo(method, path, handler));
            node.handler(handler, source);
        } else {
            RouteSource existing = node.routeSource();
            ConflictResolver resolving = source.onConflict(existing);
            switch (resolving) {
                case OVERWRITE_WARN:
                    logger.warn("Overwrite existing route \n\t%s\nwith new route\n\t%s",
                            routeInfo(method, path, node.handler()),
                            routeInfo(method, path, handler)
                    );
                case OVERWRITE:
                    node.handler(handler, source);
                case SKIP:
                    break;
                case EXIT:
                    throw new DuplicateRouteMappingException(
                            new RouteInfo(method, path.toString(), node.handler()),
                            new RouteInfo(method, path.toString(), handler)
                    );
                default:
                    throw E.unsupport();
            }
        }
    }

    boolean isMapped(H.Method method, CharSequence path) {
        return null != _search(method, path);
    }

    public void addMapping(H.Method method, CharSequence path, RequestHandler handler) {
        addMapping(method, path, handler, RouteSource.ROUTE_TABLE);
    }

    private static String routeInfo(H.Method method, CharSequence path, Object handler) {
        return S.fmt("[%s %s] - > [%s]", method, path, handler);
    }

    private Node _search(H.Method method, CharSequence path) {
        Node node = root(method);
        assert node != null;
        E.unsupportedIf(null == node, "Method %s is not supported", method);
        if (path.length() == 1 && path.charAt(0) == '/') {
            return node;
        }
        String sUrl = path.toString();
        List<CharSequence> paths = Path.tokenize(Unsafe.bufOf(sUrl));
        int len = paths.size();
        for (int i = 0; i < len - 1; ++i) {
            node = node.findChild((StrBase) paths.get(i));
            if (null == node) return null;
        }
        return node.findChild((StrBase) paths.get(len - 1));
    }

    private Node _locate(H.Method method, CharSequence path) {
        Node node = root(method);
        E.unsupportedIf(null == node, "Method %s is not supported", method);
        assert null != node;
        int pathLen = path.length();
        if (0 == pathLen || (1 == pathLen && path.charAt(0) == '/')) {
            return node;
        }
        String sUrl = path.toString();
        List<CharSequence> paths = Path.tokenize(Unsafe.bufOf(sUrl));
        int len = paths.size();
        for (int i = 0; i < len - 1; ++i) {
            node = node.addChild((StrBase) paths.get(i));
        }
        return node.addChild((StrBase) paths.get(len - 1));
    }

    // --- action handler resolving

    /**
     * Register 3rd party action handler resolver with specified directive
     *
     * @param directive
     * @param resolver
     */
    public void registerRequestHandlerResolver(String directive, RequestHandlerResolver resolver) {
        resolvers.put(directive, resolver);
    }

    // -- action method sensor
    public boolean isActionMethod(String className, String methodName) {
        String action = new StringBuilder(className).append(".").append(methodName).toString();
        String controllerPackage = appConfig.controllerPackage();
        if (S.notEmpty(controllerPackage)) {
            if (action.startsWith(controllerPackage)) {
                String action2 = action.substring(controllerPackage.length() + 1);
                if (actionNames.contains(action2)) {
                    return true;
                }
            }
        }
        return actionNames.contains(action);
    }

    // TODO: build controllerNames set to accelerate the process
    public boolean possibleController(String className) {
        String controllerPackage = appConfig.controllerPackage();
        if (S.notEmpty(controllerPackage)) {
            if (className.startsWith(controllerPackage)) {
                String class2 = className.substring(controllerPackage.length() + 1);
                if (setContains(actionNames, class2)) {
                    return true;
                }
            }
        }
        return setContains(actionNames, className);
    }

    private static boolean setContains(Set<String> set, String name) {
        for (String s: set) {
            if (s.contains(name)) return true;
        }
        return false;
    }

    public void debug(PrintStream ps) {
        for (H.Method method : supportedHttpMethods()) {
            Node node = root(method);
            node.debug(method, ps);
        }
    }

    public List<RouteInfo> debug() {
        List<RouteInfo> info = C.newList();
        debug(info);
        return info;
    }

    public void debug(List<RouteInfo> routes) {
        for (H.Method method : supportedHttpMethods()) {
            Node node = root(method);
            node.debug(method, routes);
        }
    }

    public static H.Method[] supportedHttpMethods() {
        return targetMethods;
    }

    private Node search(H.Method method, List<CharSequence> path, ActionContext context) {
        Node node = root(method);
        int sz = path.size();
        int i = 0;
        while (null != node && i < sz) {
            CharSequence nodeName = path.get(i++);
            node = node.child(nodeName, context);
            if (null != node && node.terminateRouteSearch()) {
                if (i == sz) {
                    context.param(ParamNames.PATH, "");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int j = i; j < sz; ++j) {
                        sb.append('/').append(path.get(j));
                    }
                    context.param(ParamNames.PATH, sb.toString());
                }
                break;
            }
        }
        return node;
    }

    private Node search(H.Method method, Iterator<CharSequence> path, ActionContext context) {
        Node node = root(method);
        while (null != node && path.hasNext()) {
            CharSequence nodeName = path.next();
            node = node.child(nodeName, context);
            if (null != node && node.terminateRouteSearch()) {
                if (!path.hasNext()) {
                    context.param(ParamNames.PATH, "");
                } else {
                    StringBuilder sb = new StringBuilder();
                    while (path.hasNext()) {
                        sb.append('/').append(path.next());
                    }
                    context.param(ParamNames.PATH, sb.toString());
                }
                break;
            }
        }
        return node;
    }

    private RequestHandler resolveActionHandler(CharSequence action) {
        $.T2<String, String> t2 = splitActionStr(action);
        String directive = t2._1, payload = t2._2;

        if (S.notEmpty(directive)) {
            RequestHandlerResolver resolver = resolvers.get(action);
            RequestHandler handler = null == resolver ?
                    BuiltInHandlerResolver.tryResolve(directive, payload, app()) :
                    resolver.resolve(payload, app());
            E.unsupportedIf(null == handler, "cannot find action handler by directive: %s", directive);
            return handler;
        } else {
            RequestHandler handler = handlerLookup.resolve(payload, app());
            E.unsupportedIf(null == handler, "cannot find action handler: %s", action);
            actionNames.add(payload);
            return handler;
        }
    }

    private $.T2<String, String> splitActionStr(CharSequence action) {
        FastStr fs = FastStr.of(action);
        FastStr fs1 = fs.beforeFirst(':');
        FastStr fs2 = fs1.isEmpty() ? fs : fs.substr(fs1.length() + 1);
        return $.T2(fs1.trim().toString(), fs2.trim().toString());
    }

    private Node root(H.Method method) {
        switch (method) {
            case GET:
                return _GET;
            case POST:
                return _POST;
            case PUT:
                return _PUT;
            case DELETE:
                return _DEL;
            default:
                throw E.unexpected("HTTP Method not supported: %s", method);
        }
    }

    private static Result notFound() {
        throw NOT_FOUND;
    }

    public final class f {
        public $.Predicate<String> IS_CONTROLLER = new $.Predicate<String>() {
            @Override
            public boolean test(String s) {
                for (String action : actionNames) {
                    if (action.startsWith(s)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public final f f = new f();

    /**
     * The data structure support decision tree for
     * fast URL routing
     */
    private static class Node extends DestroyableBase implements Serializable, TreeNode {
        static Node newRoot(String name) {
            Node node = new Node(-1);
            node.name = S.str(name);
            return node;
        }

        private int id;
        private StrBase name;
        private Pattern pattern;
        private CharSequence varName;
        private Node parent;
        private Node dynamicChild;
        private C.Map<CharSequence, Node> staticChildren = C.newMap();
        private RequestHandler handler;
        private RouteSource routeSource;

        private Node(int id) {
            this.id = id;
            name = FastStr.EMPTY_STR;
        }

        Node(StrBase name, Node parent) {
            E.NPE(name);
            this.name = name;
            this.parent = parent;
            this.id = name.hashCode();
            parseDynaName(name);
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof Node) {
                Node that = (Node) obj;
                return that.id == id && that.name.equals(name);
            }
            return false;
        }

        public boolean isDynamic() {
            return null != varName;
        }

        boolean metaInfoMatches(StrBase string) {
            $.T2<StrBase, Pattern> result = _parseDynaName(string);
            if (pattern != null && result._2 != null) {
                return pattern.pattern().equals(result._2.pattern());
            } else {
                // just allow route table to use different var names
                return true;
            }
        }

        public boolean matches(CharSequence chars) {
            if (!isDynamic()) return name.contentEquals(chars);
            return (null == pattern) || pattern.matcher(chars).matches();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<TreeNode> children() {
            C.List<TreeNode> list = (C.List) C.list(staticChildren.values());
            return null == dynamicChild ? list : list.append(dynamicChild);
        }

        public Node child(CharSequence name, ActionContext context) {
            Node node = staticChildren.get(name);
            if (null == node && null != dynamicChild) {
                if (dynamicChild.matches(name)) {
                    context.param(dynamicChild.varName.toString(), S.urlDecode(S.string(name)));
                    return dynamicChild;
                }
            }
            return node;
        }

        @Override
        public String id() {
            return name.toString();
        }

        @Override
        public String label() {
            StringBuilder sb = S.builder(name);
            if (null != handler) {
                sb.append(" -> ").append(handler);
            }
            return sb.toString();
        }

        @Override
        protected void releaseResources() {
            if (null != handler) {
                handler.destroy();
            }
            if (null != dynamicChild) {
                dynamicChild.destroy();
                dynamicChild = null;
            }
            Destroyable.Util.destroyAll(staticChildren.values());
            staticChildren.clear();
        }

        Node childByMetaInfo(StrBase s) {
            Node node = staticChildren.get(s);
            if (null == node && null != dynamicChild) {
                if (dynamicChild.metaInfoMatches(s)) {
                    return dynamicChild;
                }
            }
            return node;
        }

        Node findChild(StrBase<?> name) {
            name = name.trim();
            return childByMetaInfo(name);
        }

        Node addChild(StrBase<?> name) {
            name = name.trim();
            Node node = childByMetaInfo(name);
            if (null != node && !node.isDynamic()) {
                return node;
            }
            Node child = new Node(name, this);
            if (child.isDynamic()) {
                //E.unexpectedIf(null != dynamicChild, "Cannot have more than one dynamic node in the route tree: %s", name);
                if (null == dynamicChild) {
                    dynamicChild = child;
                }
                return dynamicChild;
            } else {
                staticChildren.put(name, child);
            }
            return child;
        }

        Node handler(RequestHandler handler, RouteSource source) {
            this.routeSource = $.notNull(source);
            this.handler = handler.requireResolveContext() ? new ContextualHandler((RequestHandlerBase)handler) : handler;
            return this;
        }

        RequestHandler handler() {
            return this.handler;
        }

        RouteSource routeSource() {
            return routeSource;
        }

        boolean terminateRouteSearch() {
            return null != handler && handler.supportPartialPath();
        }

        String path() {
            if (null == parent) return "/";
            String pPath = parent.path();
            return new StringBuilder(pPath).append(pPath.endsWith("/") ? "" : "/").append(name).toString();
        }

        void debug(H.Method method, PrintStream ps) {
            if (null != handler) {
                ps.printf("%s %s %s\n", method, path(), handler);
            }
            for (Node node : staticChildren.values()) {
                node.debug(method, ps);
            }
            if (null != dynamicChild) {
                dynamicChild.debug(method, ps);
            }
        }

        void debug(H.Method method, List<RouteInfo> routes) {
            if (null != handler) {
                routes.add(new RouteInfo(method, path(), handler));
            }
            for (Node node : staticChildren.values()) {
                node.debug(method, routes);
            }
            if (null != dynamicChild) {
                dynamicChild.debug(method, routes);
            }
        }

        private void parseDynaName(StrBase name) {
            $.T2<StrBase, Pattern> result = _parseDynaName(name);
            if (null != result) {
                this.varName = result._1;
                this.pattern = result._2;
            }
        }

        private static $.T2<StrBase, Pattern> _parseDynaName(StrBase name) {
            name = name.trim();
            if (name.startsWith("{") && name.endsWith("}")) {
                StrBase s = name.afterFirst('{').beforeLast('}').trim();
                if (s.contains('<') && s.contains('>')) {
                    StrBase varName = s.afterLast('>').trim();
                    StrBase ptn = s.afterFirst('<').beforeLast('>').trim();
                    Pattern pattern = Pattern.compile(ptn.toString());
                    return $.T2(varName, pattern);
                } else {
                    return $.T2(s, null);
                }
            }
            return null;
        }
    }

    private static enum BuiltInHandlerResolver implements RequestHandlerResolver {
        echo() {
            @Override
            public RequestHandler resolve(CharSequence msg, App app) {
                return new Echo(msg.toString());
            }
        },
        redirect() {
            @Override
            public RequestHandler resolve(CharSequence payload, App app) {
                return new Redirect(payload.toString());
            }
        },
        file() {
            @Override
            public RequestHandler resolve(CharSequence base, App app) {
                return new StaticFileGetter(app.file(base.toString()));
            }
        },
        externalfile() {
            @Override
            public RequestHandler resolve(CharSequence base, App app) {
                File file = new File(base.toString());
                if (!file.canRead()) {
                    logger.warn("External file not found: %s", file.getPath());
                }
                return new StaticFileGetter(file);
            }
        }
        ;



        private static RequestHandler tryResolve(CharSequence directive, CharSequence payload, App app) {
            String s = directive.toString().toLowerCase();
            try {
                return valueOf(s).resolve(payload, app);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        @Override
        public void destroy() {
        }

        @Override
        public boolean isDestroyed() {
            return true;
        }
    }

    private static class ContextualHandler extends DelegateRequestHandler {
        protected ContextualHandler(RequestHandlerBase next) {
            super(next);
        }
        @Override
        public void handle(ActionContext context) {
            context.attribute(ActionContext.ATTR_HANDLER, realHandler());
            context.resolve();
            super.handle(context);
        }
    }

}
