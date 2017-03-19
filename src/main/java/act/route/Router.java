package act.route;

import act.Act;
import act.Destroyable;
import act.app.ActionContext;
import act.app.App;
import act.app.AppServiceBase;
import act.cli.tree.TreeNode;
import act.conf.AppConfig;
import act.controller.ParamNames;
import act.handler.*;
import act.handler.builtin.*;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.util.ActContext;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.http.util.Path;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

public class Router extends AppServiceBase<Router> {

    private static final H.Method[] targetMethods = new H.Method[]{
            H.Method.GET, H.Method.POST, H.Method.DELETE, H.Method.PUT, H.Method.PATCH};
    private static final Logger logger = L.get(Router.class);

    Node _GET = Node.newRoot("GET");
    Node _PUT = Node.newRoot("PUT");
    Node _POST = Node.newRoot("POST");
    Node _DEL = Node.newRoot("DELETE");
    Node _PATCH = Node.newRoot("PATCH");

    private Map<String, RequestHandlerResolver> resolvers = C.newMap();

    private RequestHandlerResolver handlerLookup;
    // map action context to url context
    // for example `act.` -> `/~`
    private Map<CharSequence, String> urlContexts = new HashMap<>();
    private Set<String> actionNames = new HashSet<>();
    private AppConfig appConfig;
    private String portId;
    private int port;
    private OptionsInfoBase optionHandlerFactory;

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
        if (S.notBlank(portId)) {
            this.port = appConfig.namedPort(portId).port();
        } else {
            this.port = appConfig.httpSecure() ? appConfig.httpExternalSecurePort() : appConfig.httpExternalPort();
        }
        this.optionHandlerFactory = new OptionsInfoBase(this);
    }

    @Override
    protected void releaseResources() {
        _GET.destroy();
        _DEL.destroy();
        _POST.destroy();
        _PUT.destroy();
        _PATCH.destroy();
        handlerLookup.destroy();
        actionNames.clear();
        appConfig = null;
    }

    public String portId() {
        return portId;
    }

    public int port() {
        return port;
    }

    // --- routing ---
    public RequestHandler getInvoker(H.Method method, CharSequence path, ActionContext context) {
        context.router(this);
        RequestHandler handler = app().blockIssueHandler();
        if (null != handler) {
            return handler;
        }
        if (method == H.Method.OPTIONS) {
            return optionHandlerFactory.optionHandler(path, context);
        }
        if (Arrays.binarySearch(targetMethods, method) < 0) {
            return UnknownHttpMethodHandler.INSTANCE;
        }
        Node node = search(method, Path.tokenizer(Unsafe.bufOf(path)), context);
        return getInvokerFrom(node);
    }

    public RequestHandler findStaticGetHandler(String url) {
        Iterator<CharSequence> path = Path.tokenizer(Unsafe.bufOf(url));
        Node node = root(H.Method.GET);
        while (null != node && path.hasNext()) {
            CharSequence nodeName = path.next();
            node = node.staticChildren.get(nodeName);
            if (null == node || node.terminateRouteSearch()) {
                break;
            }
        }
        return null == node ? null : node.handler;
    }

    private RequestHandler getInvokerFrom(Node node) {
        if (null == node) {
            return notFound();
        }
        RequestHandler handler = node.handler;
        if (null == handler) {
            if (null != node.dynamicChild) {
                node = node.dynamicChild;
                if (null != node.pattern) {
                    if (node.pattern.matcher("").matches()) {
                        if (null == node.handler) {
                            return notFound();
                        }
                        handler = node.handler;
                    } else {
                        return badRequest();
                    }
                } else if (null == node.handler) {
                    return notFound();
                } else {
                    return node.handler;
                }
            } else {
                return notFound();
            }
        }
        return handler;
    }

    // --- route building ---
    public void addContext(String actionContext, String urlContext) {
        urlContexts.put(actionContext, urlContext);
    }

    enum ConflictResolver {
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

    private CharSequence withUrlContext(CharSequence path, CharSequence action) {
        String sAction = action.toString();
        String urlContext = null;
        for (CharSequence key : urlContexts.keySet()) {
            String sKey = key.toString();
            if (sAction.startsWith(sKey)) {
                urlContext = urlContexts.get(key);
                break;
            }
        }
        return null == urlContext ? path : S.pathConcat(urlContext, '/', path.toString());
    }

    public void addMapping(H.Method method, CharSequence path, CharSequence action) {
        addMapping(method, withUrlContext(path, action), resolveActionHandler(action), RouteSource.ROUTE_TABLE);
    }

    public void addMapping(H.Method method, CharSequence path, CharSequence action, RouteSource source) {
        addMapping(method, withUrlContext(path, action), resolveActionHandler(action), source);
    }

    public void addMapping(H.Method method, CharSequence path, RequestHandler handler) {
        addMapping(method, path, handler, RouteSource.ROUTE_TABLE);
    }

    public void addMapping(H.Method method, CharSequence path, RequestHandler handler, RouteSource source) {
        Node node = _locate(method, path);
        if (null == node.handler) {
            handler = prepareReverseRoutes(handler, node);
            node.handler(handler, source);
        } else {
            RouteSource existing = node.routeSource();
            ConflictResolver resolving = source.onConflict(existing);
            switch (resolving) {
                case OVERWRITE_WARN:
                    logger.warn("\n\tOverwrite existing route \n\t\t%s\n\twith new route\n\t\t%s",
                            routeInfo(method, path, node.handler()),
                            routeInfo(method, path, handler)
                    );
                case OVERWRITE:
                    handler = prepareReverseRoutes(handler, node);
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

    private RequestHandler prepareReverseRoutes(RequestHandler handler, Node node) {
        if (handler instanceof RequestHandlerInfo) {
            RequestHandlerInfo info = (RequestHandlerInfo) handler;
            CharSequence action = info.action;
            Node root = node.root;
            root.reverseRoutes.put(action.toString(), node);
            handler = info.theHandler();
        }
        return handler;
    }

    public String reverseRoute(String action, boolean fullUrl) {
        return reverseRoute(action, C.<String, Object>map(), fullUrl);
    }

    public String reverseRoute(String action) {
        return reverseRoute(action, C.<String, Object>map());
    }

    public String reverseRoute(String action, Map<String, Object> args) {
        String fullAction = inferFullActionPath(action);
        for (H.Method m : supportedHttpMethods()) {
            String url = reverseRoute(fullAction, m, args);
            if (null != url) {
                return url;
            }
        }
        return null;
    }

    public static final $.Func0<String> DEF_ACTION_PATH_PROVIDER = new $.Func0<String>() {
        @Override
        public String apply() throws NotAppliedException, Osgl.Break {
            ActContext context = ActContext.Base.currentContext();
            E.illegalStateIf(null == context, "cannot use shortcut action path outside of a act context");
            return context.methodPath();
        }
    };

    // See https://github.com/actframework/actframework/issues/107
    public static String inferFullActionPath(String actionPath) {
        return inferFullActionPath(actionPath, DEF_ACTION_PATH_PROVIDER);
    }

    public static String inferFullActionPath(String actionPath, $.Func0<String> currentActionPathProvider) {
        String handler, controller = null;
        int pos = actionPath.indexOf(".");
        if (pos < 0) {
            handler = actionPath;
        } else {
            controller = actionPath.substring(0, pos);
            handler = actionPath.substring(pos + 1, actionPath.length());
            if (handler.indexOf(".") > 0) {
                // it's a full path, not shortcut
                return actionPath;
            }
        }
        String currentPath = currentActionPathProvider.apply();
        pos = currentPath.lastIndexOf(".");
        String currentPathWithoutHandler = currentPath.substring(0, pos);
        if (null == controller) {
            return S.concat(currentPathWithoutHandler, ".", handler);
        }
        pos = currentPathWithoutHandler.lastIndexOf(".");
        String currentPathWithoutController = currentPathWithoutHandler.substring(0, pos);
        return S.concat(currentPathWithoutController, ".", controller, ".", handler);
    }

    public String reverseRoute(String action, Map<String, Object> args, boolean fullUrl) {
        String path = reverseRoute(action, args);
        if (null == path) {
            return null;
        }
        return fullUrl ? fullUrl(path) : path;
    }

    public String reverseRoute(String action, H.Method method, Map<String, Object> args) {
        Node root = root(method);
        Node node = root.reverseRoutes.get(action);
        if (null == node) {
            return null;
        }
        C.List<String> elements = C.newList();
        args = new HashMap<>(args);
        while (root != node) {
            if (node.isDynamic()) {
                String s = node.varName.toString();
                Object v = args.remove(s);
                if (null != v) {
                    elements.add(S.string(v));
                } else {
                    elements.add(S.concat("{", s, "}"));
                }
            } else {
                elements.add(node.name.toString());
            }
            node = node.parent;
        }
        S.Buffer sb = S.newBuffer();
        Iterator<String> itr = elements.reverseIterator();
        while (itr.hasNext()) {
            sb.append("/").append(itr.next());
        }
        if (method == H.Method.GET && !args.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Object> entry : args.entrySet()) {
                Object v = entry.getValue();
                if (null == v) {
                    continue;
                }
                String k = entry.getKey();

                if (first) {
                    sb.append("?");
                    first = false;
                } else {
                    sb.append("&");
                }
                sb.append(k).append("=").append(Codec.encodeUrl(v.toString()));
            }
        }
        return sb.toString();
    }

    public String urlBase() {
        ActionContext context = ActionContext.current();
        if (null != context) {
            return urlBase(context);
        }
        AppConfig<?> config = Act.appConfig();

        /*
         * Note we support named port (restricted access) is running in the scope of
         * the internal network, thus assume we do not have secure http channel on top
         * of that
         */
        boolean secure = null != portId && config.httpSecure();
        String scheme = secure ? "https" : "http";

        String domain = config.host();

        if (80 == port || 443 == port) {
            return S.fmt("%s://%s", scheme, domain);
        } else {
            return S.fmt("%s://%s:%s", scheme, domain, port);
        }
    }

    public String urlBase(ActionContext context) {
        H.Request req = context.req();
        String scheme = req.secure() ? "https" : "http";
        int port = req.port();
        String domain = req.domain();
        if (80 == port || 443 == port) {
            return S.fmt("%s://%s", scheme, domain);
        } else {
            return S.fmt("%s://%s:%s", scheme, domain, port);
        }
    }

    public String fullUrl(String path, Object... args) {
        if (path.startsWith("//") || path.startsWith("http")) {
            return path;
        }
        S.Buffer sb = S.newBuffer(urlBase());
        if (!path.startsWith("/")) {
            sb.append("/");
        }
        return sb.append(S.fmt(path, args)).toString();
    }

    private static final Method M_FULL_URL = $.getMethod(Router.class, "fullUrl", String.class, Object[].class);

    public String _fullUrl(String path, Object[] args) {
        return $.invokeVirtual(this, M_FULL_URL, path, args);
    }

    boolean isMapped(H.Method method, CharSequence path) {
        return null != _search(method, path);
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
            node = node.addChild((StrBase) paths.get(i), path);
        }
        return node.addChild((StrBase) paths.get(len - 1), path);
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
        return actionNames.contains(S.concat(className, ".", methodName));
    }

    // TODO: build controllerNames set to accelerate the process
    public boolean possibleController(String className) {
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
        return C.list(info).sorted();
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

    private Node search(H.Method method, Iterator<CharSequence> path, ActionContext context) {
        Node node = root(method);
        if (node.terminateRouteSearch()) {
            S.Buffer sb = S.newBuffer();
            while (path.hasNext()) {
                sb.append('/').append(path.next());
            }
            context.param(ParamNames.PATH, sb.toString());
            return node;
        }
        while (null != node && path.hasNext()) {
            CharSequence nodeName = path.next();
            node = node.child(nodeName, context);
            if (null != node && node.terminateRouteSearch()) {
                if (!path.hasNext()) {
                    context.param(ParamNames.PATH, "");
                } else {
                    S.Buffer sb = S.newBuffer();
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

    private static class RequestHandlerInfo extends DelegateRequestHandler {
        private CharSequence action;
        protected RequestHandlerInfo(RequestHandler handler, CharSequence action) {
            super(handler);
            this.action = action;
        }
        RequestHandler theHandler() {
            return handler_;
        }
    }

    private RequestHandlerInfo resolveActionHandler(CharSequence action) {
        $.T2<String, String> t2 = splitActionStr(action);
        String directive = t2._1, payload = t2._2;

        if (S.notEmpty(directive)) {
            RequestHandlerResolver resolver = resolvers.get(directive);
            RequestHandler handler = null == resolver ?
                    BuiltInHandlerResolver.tryResolve(directive, payload, app()) :
                    resolver.resolve(payload, app());
            E.unsupportedIf(null == handler, "cannot find action handler by directive %s on payload %s", directive, payload);
            return new RequestHandlerInfo(handler, action);
        } else {
            RequestHandler handler = handlerLookup.resolve(payload, app());
            E.unsupportedIf(null == handler, "cannot find action handler: %s", action);
            actionNames.add(payload);
            return new RequestHandlerInfo(handler, action);
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
            case PATCH:
                return _PATCH;
            default:
                throw E.unexpected("HTTP Method not supported: %s", method);
        }
    }

    private static AlwaysNotFound notFound() {
        return AlwaysNotFound.INSTANCE;
    }

    private static AlwaysBadRequest badRequest() {
        return AlwaysBadRequest.INSTANCE;
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

        // used to pass a baq request result when dynamic regex matching failed
        private static final Node BADREQUEST = new Node(Integer.MIN_VALUE) {
            @Override
            boolean terminateRouteSearch() {
                return true;
            }
        };

        static {
            BADREQUEST.handler = AlwaysBadRequest.INSTANCE;
        }

        static Node newRoot(String name) {
            Node node = new Node(-1);
            node.name = S.str(name);
            return node;
        }

        private int id;
        private StrBase name;
        private Pattern pattern;
        private CharSequence varName;
        private Node root;
        private Node parent;
        private Node dynamicChild;
        private C.Map<CharSequence, Node> staticChildren = C.newMap();
        private C.Map<UrlPath, Node> dynamicAliases = C.newMap();
        private RequestHandler handler;
        private RouteSource routeSource;
        private Map<String, Node> reverseRoutes = new HashMap<>();

        private Node(int id) {
            this.id = id;
            name = FastStr.EMPTY_STR;
            root = this;
        }

        Node(StrBase name, Node parent) {
            E.NPE(name);
            this.name = name;
            this.parent = parent;
            this.id = name.hashCode();
            this.root = parent.root;
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
                    UrlPath path = new UrlPath(context.req().path());
                    CharSequence varName = dynamicChild.varName;
                    for (Map.Entry<UrlPath, Node> entry : dynamicChild.dynamicAliases.entrySet()) {
                        if (entry.getKey().equals(path)) {
                            varName = entry.getValue().varName;
                            break;
                        }
                    }
                    context.param(varName.toString(), S.urlDecode(S.string(name)));
                    return dynamicChild;
                } else {
                    return Node.BADREQUEST;
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
            StringBuilder sb = S.newBuilder(name);
            if (null != handler) {
                sb.append(" -> ").append(RouteInfo.compactHandler(handler.toString()));
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
            Destroyable.Util.destroyAll(staticChildren.values(), ApplicationScoped.class);
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

        Node addChild(StrBase<?> name, CharSequence path) {
            name = name.trim();
            Node node = childByMetaInfo(name);
            if (null != node && !node.isDynamic()) {
                return node;
            }
            Node child = new Node(name, this);
            if (child.isDynamic()) {
                if (null == dynamicChild) {
                    dynamicChild = child;
                    child.dynamicAliases.put(new UrlPath(path), child);
                } else {
                    dynamicChild.dynamicAliases.put(new UrlPath(path), child);
                }
                return dynamicChild;
            } else {
                staticChildren.put(name, child);
            }
            return child;
        }

        Node handler(RequestHandler handler, RouteSource source) {
            this.routeSource = $.notNull(source);
            this.handler = handler.requireResolveContext() ? new ContextualHandler((RequestHandlerBase)handler, this) : handler;
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
            return S.pathConcat(pPath, '/', name.toString());
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
            } else if (name.contains(":")) {
                StrBase varName = name.beforeFirst(":");
                StrBase ptn = name.afterFirst(":");
                Pattern pattern = ptn.isBlank() ? null : Pattern.compile(ptn.toString());
                return $.T2(varName, pattern);
            } else {
                return null;
            }
        }
    }

    private enum BuiltInHandlerResolver implements RequestHandlerResolver {
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
        redirectdir() {
            @Override
            public RequestHandler resolve(CharSequence payload, App app) {
                return new RedirectDir(payload.toString());
            }
        },
        file() {
            @Override
            public RequestHandler resolve(CharSequence base, App app) {
                return new StaticFileGetter(app.file(base.toString()));
            }
        },
        resource() {
            @Override
            public RequestHandler resolve(CharSequence payload, App app) {
                return new StaticResourceGetter(payload.toString());
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
        };

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

        @Override
        public Class<? extends Annotation> scope() {
            return ApplicationScoped.class;
        }
    }

    private static class ContextualHandler extends DelegateRequestHandler {
        private Set<String> pathVariables;
        protected ContextualHandler(RequestHandlerBase next, Node node) {
            super(next);
            pathVariables = new HashSet<>();
            while (node != null) {
                if (node.isDynamic()) {
                    pathVariables.add(node.varName.toString());
                }
                node = node.parent;
            }
        }
        @Override
        public void handle(ActionContext context) {
            context.attribute(ActionContext.ATTR_HANDLER, realHandler());
            context.attribute(ActionContext.ATTR_PATH_VARS, pathVariables);
            context.resolve();
            super.handle(context);
        }
    }

}
