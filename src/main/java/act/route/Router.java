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
import act.Destroyable;
import act.app.*;
import act.app.event.SysEventId;
import act.cli.tree.TreeNode;
import act.conf.AppConfig;
import act.controller.ParamNames;
import act.controller.builtin.ThrottleFilter;
import act.handler.*;
import act.handler.builtin.*;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.security.CORS;
import act.security.CSRF;
import act.util.ActContext;
import act.util.DestroyableBase;
import act.ws.WebSocketConnectionListener;
import act.ws.WsEndpoint;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.http.util.Path;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.Result;
import org.osgl.util.*;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.NotNull;

public class Router extends AppHolderBase<Router> implements TreeNode {

    public static final String PORT_DEFAULT = "default";
    public static final String PORT_CLI_OVER_HTTP = "__cli__";
    public static final String PORT_ADMIN = "__admin__";
    public static final String PORT_SYS = "__sys__";

    /**
     * A visitor can be passed to the router to traverse
     * the routes
     */
    public interface Visitor {
        /**
         * Visit a route mapping in the router
         *
         * @param method
         *         the HTTP method
         * @param path
         *         the URL path
         * @param source
         *         the route source
         * @param varNames
         *         the URL path variable name list
         * @param handler
         *         the handler
         */
        void visit(H.Method method, String path, RouteSource source, Set<String> varNames, RequestHandler handler);
    }

    public static final String IGNORE_NOTATION = "...";

    private static final H.Method[] targetMethods = new H.Method[]{
            H.Method.GET, H.Method.POST, H.Method.DELETE, H.Method.PUT, H.Method.PATCH};

    private static final Logger LOGGER = LogManager.get(Router.class);

    Node _GET;
    Node _PUT;
    Node _POST;
    Node _DEL;
    Node _PATCH;

    List<Node> roots;

    private Map<String, RequestHandlerResolver> resolvers = new HashMap<>();

    private RequestHandlerResolver handlerLookup;
    // map action context to url context
    // for example `act.` -> `/~`
    private Map<String, String> urlContexts = new HashMap<>();
    private Set<String> actionNames = new HashSet<>();
    private AppConfig appConfig;
    private String portId;
    private int port;
    private OptionsInfoBase optionHandlerFactory;
    private Set<RequestHandler> requireBodyParsing = new HashSet<>();

    private static final ThreadLocal<AtomicInteger> varIdCounter = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
        }
    };

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
        _GET = Node.newRoot("GET", appConfig);
        _PUT = Node.newRoot("PUT", appConfig);
        _POST = Node.newRoot("POST", appConfig);
        _DEL = Node.newRoot("DELETE", appConfig);
        _PATCH = Node.newRoot("PATCH", appConfig);
        roots = C.list(_GET, _PUT, _POST, _DEL, _PATCH);
    }

    @Override
    protected void releaseResources() {
        for (Node root : roots) {
            root.destroy();
        }
        handlerLookup.destroy();
        actionNames.clear();
        appConfig = null;
    }

    @Override
    public String id() {
        return S.blank(portId) ? "default" : portId;
    }

    @Override
    public String label() {
        return S.concat("Router[", id(), "]");
    }

    @Override
    public List<TreeNode> children() {
        return $.cast(roots);
    }

    public String portId() {
        return portId;
    }

    public int port() {
        return port;
    }

    /**
     * Accept a {@link Visitor} to traverse route mapping in this
     * router
     *
     * @param visitor
     *         the visitor
     */
    public void accept(Visitor visitor) {
        visit(_GET, H.Method.GET, visitor);
        visit(_POST, H.Method.POST, visitor);
        visit(_PUT, H.Method.PUT, visitor);
        visit(_DEL, H.Method.DELETE, visitor);
        visit(_PATCH, H.Method.PATCH, visitor);
    }

    private void initControllerLookup(RequestHandlerResolver lookup) {
        if (null == lookup) {
            lookup = new RequestHandlerResolverBase() {
                @Override
                public RequestHandler resolve(String payload, App app) {
                    if (null != payload && payload.startsWith(WsEndpoint.PSEUDO_METHOD)) {
                        String className = S.cut(payload).afterFirst("|");
                        return createWebSocketConnectionHandler(className);
                    }
                    return new RequestHandlerProxy(payload, app);
                }
            };
        }
        handlerLookup = lookup;
    }

    private static RequestHandler createWebSocketConnectionHandler(String className) {
        if (S.notBlank(className)) {
            Class<?> type = Act.appClassForName(className);
            if (WebSocketConnectionListener.class.isAssignableFrom(type)) {
                Class<? extends WebSocketConnectionListener> listenerType = $.cast(type);
                WebSocketConnectionListener listener = Act.app().eventEmitted(SysEventId.DEPENDENCY_INJECTOR_PROVISIONED) ? Act.getInstance(listenerType) : new WebSocketConnectionListener.DelayedResolveProxy(listenerType);
                return Act.network().createWebSocketConnectionHandler(listener);
            }
        }
        return Act.network().createWebSocketConnectionHandler();
    }

    private void visit(Node node, H.Method method, Visitor visitor) {
        RequestHandler handler = node.handler;
        if (null != handler) {
            if (handler instanceof ContextualHandler) {
                handler = ((ContextualHandler) handler).realHandler();
            }
            visitor.visit(method, node.path(), node.routeSource, node.allVarNames(), handler);
        }
        for (TreeNode child : node.children()) {
            visit((Node)child, method, visitor);
        }
    }

    // Mark handler as require body parsing
    public void markRequireBodyParsing(RequestHandler handler) {
        requireBodyParsing.add(handler);
    }

    // --- routing ---
    public RequestHandler getInvoker(H.Method method, String path, ActionContext context) {
        context.router(this);
        if (method == H.Method.OPTIONS) {
            return optionHandlerFactory.optionHandler(path, context);
        }
        Node node = root(method, false);
        if (null == node) {
            return UnknownHttpMethodHandler.INSTANCE;
        }
        node = search(node, Path.tokenizer(Unsafe.bufOf(path)), context);
        RequestHandler handler = getInvokerFrom(node, context);
        RequestHandler blockIssueHandler = app().blockIssueHandler();
        if (null == blockIssueHandler || (handler instanceof FileGetter || handler instanceof ResourceGetter)) {
            return handler;
        }
        return blockIssueHandler;
    }

    public RequestHandler findStaticGetHandler(String url) {
        Iterator<String> path = Path.tokenizer(Unsafe.bufOf(url));
        Node node = root(H.Method.GET);
        while (null != node && path.hasNext()) {
            String nodeName = path.next();
            node = node.staticChildren.get(nodeName);
            if (null == node || node.terminateRouteSearch()) {
                break;
            }
        }
        return null == node ? null : node.handler;
    }

    private RequestHandler getInvokerFrom(Node node, ActionContext context) {
        if (null == node) {
            return notFound();
        }
        RequestHandler handler = node.handler;
        if (null == handler) {
            for (Node targetNode : node.dynamicChildren) {
                if (Node.MATCH_ALL.equals(targetNode.patternTrait) || targetNode.pattern.matcher("").matches()) {
                    return getInvokerFrom(targetNode, context);
                }
            }
            return notFound();
        } else {
            context.routeSource(node.routeSource);
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

    private String withUrlContext(String path, String action) {
        String sAction = action;
        String urlContext = null;
        for (String key : urlContexts.keySet()) {
            String sKey = key;
            if (sAction.startsWith(sKey)) {
                urlContext = urlContexts.get(key);
                break;
            }
        }
        return null == urlContext ? path : S.pathConcat(urlContext, '/', path);
    }

    public void addMapping(H.Method method, String path, String action) {
        addMapping(method, withUrlContext(path, action), resolveActionHandler(action), RouteSource.ROUTE_TABLE);
    }

    public void addMapping(H.Method method, String path, String action, RouteSource source) {
        addMapping(method, withUrlContext(path, action), resolveActionHandler(action), source);
    }

    public void addMapping(H.Method method, String path, RequestHandler handler) {
        addMapping(method, path, handler, RouteSource.ROUTE_TABLE);
    }

    private String evalConf(String s) {
        Object o = appConfig.getIgnoreCase(s);
        if (null == o) {
            warn("Missing configuration for path substitution: %s", s);
            return s;
        }
        return S.string(o);
    }

    private String processStringSubstitution(String s) {
        int n = s.indexOf("${");
        if (n < 0) {
            return s;
        }
        if (n == 0 && s.endsWith(")}")) {
            s = s.substring(2);
            s = s.substring(0, s.length() - 1);
            return evalConf(s);
        }
        int a = 0;
        int z = n;
        S.Buffer buf = S.buffer();
        while (true) {
            buf.append(s.substring(a, z));
            n = s.indexOf("}", z);
            a = n;
            String part = s.substring(z + 2, a);
            buf.append(evalConf(part));
            n = s.indexOf("${", a);
            if (n < 0) {
                buf.append(s.substring(a + 1));
                return buf.toString();
            }
            z = n;
        }
    }

    @SuppressWarnings("FallThrough")
    public void addMapping(final H.Method method, String path, RequestHandler handler, final RouteSource source) {
        if (isTraceEnabled()) {
            trace("R+ %s %s | %s (%s)", method, path, handler, source);
        }
        path = processStringSubstitution(path);
        if (!app().config().builtInReqHandlerEnabled()) {
            String sPath = path;
            if (sPath.startsWith("/~/")) {
                // disable built-in handlers except those might impact application behaviour
                // apibook is allowed here as it only available on dev mode
                if (!(sPath.contains("asset") || sPath.contains("i18n") || sPath.contains("job") || sPath.contains("api") || sPath.contains("ticket"))) {
                    return;
                }
            }
        }
        Node node = _locate(method, path, handler.toString());
        if (null == node.handler) {
            Set<Node> conflicts = node.conflicts();
            if (!conflicts.isEmpty()) {
                for (Node conflict : conflicts) {
                    if (null != conflict.handler) {
                        node = conflict;
                        break;
                    }
                }
            }
        }
        if (null == node.handler) {
            handler = prepareReverseRoutes(handler, node);
            node.handler(handler, source);
        } else {
            RouteSource existing = node.routeSource();
            ConflictResolver resolving = source.onConflict(existing);
            switch (resolving) {
                case OVERWRITE_WARN:
                    warn("\n\tOverwrite existing route \n\t\t%s\n\twith new route\n\t\t%s",
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
                            new RouteInfo(method, path, node.handler(), existing),
                            new RouteInfo(method, path, handler, source)
                    );
                default:
                    throw E.unsupport();
            }
        }
    }

    private RequestHandler prepareReverseRoutes(RequestHandler handler, Node node) {
        if (handler instanceof RequestHandlerInfo) {
            RequestHandlerInfo info = (RequestHandlerInfo) handler;
            String action = info.action;
            Node root = node.root;
            root.reverseRoutes.put(action, node);
            handler = info.theHandler();
        }
        return handler;
    }

    public String reverseRoute(String action, boolean fullUrl) {
        return reverseRoute(action, new HashMap<String, Object>(), fullUrl);
    }

    public String reverseRoute(String action) {
        return reverseRoute(action, new HashMap<String, Object>());
    }

    public String reverseRoute(String action, Map<String, Object> args) {
        String fullAction = inferFullActionPath(action);
        for (H.Method m : supportedHttpMethods()) {
            String url = reverseRoute(fullAction, m, args);
            if (null != url) {
                return ensureUrlContext(url);
            }
        }
        return null;
    }

    public static final $.Func0<String> DEF_ACTION_PATH_PROVIDER = new $.Func0<String>() {
        @Override
        public String apply() throws NotAppliedException, $.Break {
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
        if (actionPath.contains("/")) {
            return actionPath;
        }
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
        if (null == currentPath) {
            return actionPath;
        }
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
        int varId = 0;
        while (root != node) {
            if (node.isDynamic()) {
                Node targetNode = node;
                for (Map.Entry<String, Node> entry : node.dynamicReverseAliases.entrySet()) {
                    if (entry.getKey().equals(action)) {
                        targetNode = entry.getValue();
                        break;
                    }
                }
                String s = "";
                if (!args.isEmpty()) {
                    S.Buffer buffer = S.buffer();
                    for ($.Transformer<Map<String, Object>, String> builder : targetNode.nodeValueBuilders) {
                        buffer.append(builder.transform(args));
                    }
                    s = buffer.toString();
                    if (S.blank(s) && targetNode.varNames.size() <= varId) {
                        s = S.string(args.remove(S.string(targetNode.varNames.get(varId++))));
                    }
                }
                if (S.blank(s)) {
                    s = S.string("-");
                }
                elements.add(s);
            } else {
                elements.add(node.toString());
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
        boolean secure = null == portId && config.httpSecure();
        String scheme = secure ? "https" : "http";

        String domain = config.host();

        if (80 == port || 443 == port) {
            return S.concat(scheme, "://", domain);
        } else {
            return S.concat(scheme, "://", domain, ":", S.string(port));
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

    private String ensureUrlContext(String path) {
        String urlContext = appConfig.urlContext();
        if (null == urlContext || path.startsWith(urlContext)) {
            if ("/".equals(path)) {
                path = "";
            }
            return path;
        }
        if (!path.startsWith("/")) {
            path = S.concat("/", path);
            if (path.startsWith(urlContext)) {
                return path;
            }
        }
        if ("/".equals(path)) {
            path = "";
        }
        return S.concat(urlContext, path);
    }

    public String fullUrl(final String pathTemplate, Object... args) {
        String path = S.fmt(pathTemplate, args);
        if (path.startsWith("//") || path.startsWith("http")) {
            return path;
        }
        if (!path.contains("/") && (path.contains(".") || path.contains("("))) {
            String reversedPath = reverseRoute(path);
            path = null != reversedPath ? reversedPath : path;
        }
        S.Buffer sb = S.newBuffer(urlBase());
        path = ensureUrlContext(path);
        return sb.append(S.fmt(path, args)).toString();
    }

    /**
     * Return full URL of reverse rout of specified action
     *
     * @param action
     *         the action path
     * @param renderArgs
     *         the render arguments
     * @return the full URL as described above
     */
    public String fullUrl(String action, Map<String, Object> renderArgs) {
        return fullUrl(reverseRoute(action, renderArgs));
    }

    private static final Method M_FULL_URL = $.getMethod(Router.class, "fullUrl", String.class, Object[].class);

    public String _fullUrl(String path, Object[] args) {
        return $.invokeVirtual(this, M_FULL_URL, path, args);
    }

    boolean isMapped(H.Method method, String path) {
        return null != _search(method, path);
    }

    private static String routeInfo(H.Method method, String path, Object handler) {
        return S.fmt("[%s %s] - > [%s]", method, path, handler);
    }

    private Node _search(H.Method method, String path) {
        Node node = root(method);
        assert node != null;
        E.unsupportedIf(null == node, "Method %s is not supported", method);
        if (path.length() == 1 && path.charAt(0) == '/') {
            return node;
        }
        String sUrl = path;
        List<String> paths = Path.tokenize(Unsafe.bufOf(sUrl));
        int len = paths.size();
        for (int i = 0; i < len - 1; ++i) {
            node = node.findChild(paths.get(i));
            if (null == node) return null;
        }
        return node.findChild(paths.get(len - 1));
    }

    private Node _locate(final H.Method method, final String path, String action) {
        Node node = root(method);
        E.unsupportedIf(null == node, "Method %s is not supported", method);
        assert null != node;
        int pathLen = path.length();
        if (0 == pathLen || (1 == pathLen && path.charAt(0) == '/')) {
            return node;
        }
        String sUrl = path;
        List<String> paths = Path.tokenize(Unsafe.bufOf(sUrl));
        int len = paths.size();
        for (int i = 0; i < len - 1; ++i) {
            String part = paths.get(i);
            if (checkIgnoreRestParts(node, part)) {
                return node;
            }
            node = node.addChild(part, path, action);
        }
        String part = paths.get(len - 1);
        if (checkIgnoreRestParts(node, part)) {
            return node;
        }
        return node.addChild(part, path, action);
    }

    private boolean checkIgnoreRestParts(Node node, String nextPart) {
        boolean shouldIgnoreRests = S.eq(IGNORE_NOTATION, S.string(nextPart));
        E.invalidConfigurationIf(node.ignoreRestParts() && !shouldIgnoreRests, "Bad route configuration: parts appended to route that ends with \"...\"");
        E.invalidConfigurationIf(shouldIgnoreRests && !node.children().isEmpty(), "Bad route configuration: \"...\" appended to node that has children");
        node.ignoreRestParts(shouldIgnoreRests);
        return shouldIgnoreRests;
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
        for (String s : set) {
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
        List<RouteInfo> info = new ArrayList<>();
        debug(info);
        return C.list(info).sorted();
    }

    public void debug(List<RouteInfo> routes) {
        for (H.Method method : supportedHttpMethods()) {
            Node node = root(method);
            node.debug(method, routes, new HashSet<Node>());
        }
    }

    public static H.Method[] supportedHttpMethods() {
        return targetMethods;
    }

    private Node search(Node rootNode, Iterator<String> path, ActionContext context) {
        varIdCounter.get().set(0);
        Node node = rootNode;
        Node backup = null;
        String backupPath = null;
        List<String> pathBackup = null;
        if (node.terminateRouteSearch() && !context.urlPath().isBuiltIn()) {
            S.Buffer sb = S.buffer();
            pathBackup = new ArrayList<>();
            while (path.hasNext()) {
                String pathElement = path.next();
                pathBackup.add(pathElement);
                sb.append('/').append(pathElement);
            }
            backupPath = sb.toString();
            backup = node;
        }
        if (null != pathBackup) {
            path = pathBackup.iterator();
        }
        while (null != node && path.hasNext()) {
            String nodeName = path.next();
            node = node.child(nodeName, context);
            if (null != node) {
                if (node.terminateRouteSearch()) {
                    if (!path.hasNext()) {
                        context.param(ParamNames.PATH, "");
                    } else {
                        S.Buffer sb = S.buffer();
                        while (path.hasNext()) {
                            if (!sb.isEmpty()) {
                                sb.append('/');
                            }
                            sb.append(path.next());
                        }
                        context.param(ParamNames.PATH, sb.toString());
                    }
                    break;
                } else if (node.ignoreRestParts()) {
                    S.Buffer sb = S.buffer();
                    while (path.hasNext()) {
                        if (!sb.isEmpty()) {
                            sb.append('/');
                        }
                        sb.append(path.next());
                    }
                    context.param(ParamNames.PATH, sb.toString());
                    break;
                }
            }
        }
        if (null == node && null != backup) {
            node = backup;
            context.param(ParamNames.PATH, backupPath);
        }
        return node;
    }

    private static class RequestHandlerInfo extends DelegateRequestHandler {
        private String action;

        protected RequestHandlerInfo(RequestHandler handler, String action) {
            super(handler);
            this.action = action;
        }

        RequestHandler theHandler() {
            return handler_;
        }

        @Override
        public String toString() {
            return action.toString();
        }
    }

    private RequestHandlerInfo resolveActionHandler(String action) {
        $.T2<String, String> t2 = splitActionStr(action);
        String directive = t2._1, payload = t2._2;

        if (S.empty(directive)) {
            if (payload.contains("/")) {
                directive = "resource";
            }
        }

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

    private $.T2<String, String> splitActionStr(String action) {
        FastStr fs = FastStr.of(action);
        FastStr fs1 = fs.beforeFirst(':');
        FastStr fs2 = fs1.isEmpty() ? fs : fs.substr(fs1.length() + 1);
        return $.T2(fs1.trim().toString(), fs2.trim().toString());
    }

    private Node root(H.Method method) {
        return root(method, true);
    }

    private Node root(H.Method method, boolean reportError) {
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
                if (reportError) {
                    throw E.unexpected("HTTP Method not supported: %s", method);
                }
                return null;
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
    private static class Node extends DestroyableBase implements Serializable, TreeNode, Comparable<Node> {

        private static final S.Pair TILDE = S.pair('~', '~');

        // used to pass a baq request result when dynamic regex matching failed
        private static final Node BADREQUEST = new Node(Integer.MIN_VALUE, Act.appConfig()) {
            @Override
            boolean terminateRouteSearch() {
                return true;
            }
        };

        static {
            BADREQUEST.handler = AlwaysBadRequest.INSTANCE;
        }

        static Node newRoot(String name, AppConfig<?> config) {
            Node node = new Node(-1, config);
            node.name = name;
            return node;
        }

        static final String MATCH_ALL = "(.*?)";

        private String uid = Act.cuid();

        private int id;

        private boolean isDynamic;

        // --- for static node
        private String name;

        // --- for keyword matching node
        private Keyword keyword;

        // ignore all the rest in URL when routing
        private boolean ignoreRestParts;

        private boolean hasKeywordMatchingChild;

        // --- for dynamic node
        private Pattern pattern;
        private String patternTrait;
        private List<String> varNames = new ArrayList<>();
        // used to build the node value for reverse routing
        private List<$.Transformer<Map<String, Object>, String>> nodeValueBuilders = new ArrayList<>();

        // --- references
        private Node root;
        private Node parent;
        private transient Node conflictNode;
        private List<Node> dynamicChildren = new ArrayList<>();
        private Map<String, Node> staticChildren = new HashMap<>();
        private Map<Keyword, Node> keywordMatchingChildren = new HashMap<>();
        private Map<UrlPath, Node> dynamicAliases = new HashMap<>();
        private Map<String, Node> dynamicReverseAliases = new HashMap<>();
        private RequestHandler handler;
        private RouteSource routeSource;
        private RouterRegexMacroLookup macroLookup;
        private Map<String, Node> reverseRoutes = new HashMap<>();

        private Node(int id, AppConfig config) {
            this.id = id;
            this.macroLookup = config.routerRegexMacroLookup();
            name = "";
            root = this;
        }

        Node(Keyword keyword, Node parent) {
            this.keyword = $.requireNotNull(keyword);
            this.parent = parent;
            this.id = keyword.hashCode();
            this.root = parent.root;
            this.macroLookup = parent.macroLookup;
        }

        Node(String name, Node parent) {
            this.name = S.requireNotBlank(name);
            this.parent = parent;
            this.id = name.hashCode();
            this.root = parent.root;
            this.macroLookup = parent.macroLookup;
            parseDynaName(name);
        }

        @Override
        public String toString() {
            return id();
        }

        @Override
        public int hashCode() {
            return uid.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public int compareTo(Node o) {
            if (!o.isDynamic && !isDynamic) {
                return name.compareTo(o.name);
            }
            int myVars = varNames.size(), hisVars = o.varNames.size();
            if (myVars != hisVars) {
                return -(myVars - hisVars);
            }
            boolean fullVar = "(.*)".equals(patternTrait), hisIsFullVar = "(.*)".equals(o.patternTrait);
            if (fullVar == hisIsFullVar) {
                return name.compareTo(o.name);
            }
            return fullVar ? 1 : -1;
        }

        public boolean ignoreRestParts() {
            return ignoreRestParts;
        }

        public void ignoreRestParts(boolean ignore) {
            this.ignoreRestParts = ignore;
        }

        public boolean isDynamic() {
            return isDynamic;
        }

        public Set<Node> conflicts() {
            Set<Node> nodes = new HashSet<>();
            findOutConflictNodes(nodes);
            return nodes;
        }

        private void findOutConflictNodes(Set<Node> nodes) {
            if (null != conflictNode) {
                nodes.add(conflictNode);
            }
            if (this.root == this || this.parent == null) {
                return;
            }
            // track back to parents
            // so that we can flag thing like
            // /foo/{foo}/xyz and /foo/{bar}/xyz
            Set<Node> parentConflictNodes = new HashSet<>();
            parent.findOutConflictNodes(parentConflictNodes);
            for (Node parentConflictNode : parentConflictNodes) {
                Node staticNode = parentConflictNode.staticChildren.get(name);
                if (null != staticNode) {
                    nodes.add(staticNode);
                    continue;
                }
                for (Node dynamicNode : parentConflictNode.dynamicChildren) {
                    if (metaInfoConflict(dynamicNode.name)) {
                        nodes.add(dynamicNode);
                    }
                }
            }
        }

        boolean metaInfoMatchesExactly(String string) {
            return this.isDynamic && $.eq(string, name);
        }

        boolean metaInfoConflict(String string) {
            $.Var<String> patternTraitsVar = $.var();

            boolean isDynamic = parseDynaNameStyleA(string, null, null, patternTraitsVar);

            isDynamic = isDynamic || parseDynaNameStyleB(
                    string, null, null,
                    patternTraitsVar, null);

            return isDynamic && patternTrait.equals(patternTraitsVar.get());
        }

        public boolean matches(String chars) {
            if (!isDynamic()) return name.contentEquals(chars);
            return (null == pattern) || pattern.matcher(chars).matches();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<TreeNode> children() {
            Set<Node> set = new HashSet<>();
            set.addAll(staticChildren.values());
            set.addAll(dynamicChildren);
            List<TreeNode> list = new ArrayList<>();
            list.addAll(set);
            for (Node node : set) {
                for (Node alias : node.dynamicAliases.values()) {
                    if (!set.contains(alias)) {
                        list.add(alias);
                    }
                }
            }
            return list;
        }

        private Node child(String name, ActionContext context) {
            Node node = staticChildren.get(name);
            if (null != node) {
                return node;
            }
            if (hasKeywordMatchingChild) {
                node = keywordMatchingChildren.get(Keyword.of(name));
                if (null != node) {
                    return node;
                }
            }
            if (!dynamicChildren.isEmpty()) {
                UrlPath path = context.urlPath();
                for (Node targetNode : dynamicChildren) {
                    for (Map.Entry<UrlPath, Node> entry : targetNode.dynamicAliases.entrySet()) {
                        if (entry.getKey().equals(path)) {
                            targetNode = entry.getValue();
                            break;
                        }
                    }
                    if (MATCH_ALL == targetNode.patternTrait) {
                        context.urlPathParam(targetNode.varNames.get(varIdCounter.get().getAndIncrement()), name);
                        return targetNode;
                    }
                    Pattern pattern = targetNode.pattern;
                    Matcher matcher = null == pattern ? null : pattern.matcher(name);
                    if (null != matcher && matcher.matches()) {
                        if (!targetNode.nodeValueBuilders.isEmpty()) {
                            for (String varName : targetNode.varNames) {
                                String varNameStr = varName;
                                try {
                                    String varValue = matcher.group(varNameStr);
                                    if (S.notBlank(varValue)) {
                                        context.urlPathParam(varNameStr, S.string(varValue));
                                    }
                                } catch (IllegalArgumentException e) {
                                    if (e.getMessage().contains("No group with name")) {
                                        String escaped = escapeUnderscore(varNameStr);
                                        String varValue = matcher.group(escaped);
                                        if (S.notBlank(varValue)) {
                                            context.urlPathParam(varNameStr, S.string(varValue));
                                        }
                                    }
                                }
                            }
                        } else {
                            String varName = targetNode.varNames.get(varIdCounter.get().getAndIncrement());
                            context.urlPathParam(varName, S.string(name));
                        }
                        return targetNode;
                    }
                }
                return Node.BADREQUEST;
            }
            return node;
        }

        @Override
        public String id() {
            return null == name ? keyword.dashed() : name;
        }

        @Override
        public String label() {
            StringBuilder sb = S.newBuilder(null == name ? "~" + keyword.dashed() + "~" : name);
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
            Destroyable.Util.destroyAll(dynamicChildren, ApplicationScoped.class);
            Destroyable.Util.destroyAll(staticChildren.values(), ApplicationScoped.class);
            staticChildren.clear();
        }

        /**
         * Returns all URL path variable names including var name of parent/ancestors.
         * @return all URL path variable names as described above
         */
        public Set<String> allVarNames() {
            Set<String> set = new HashSet<>(varNames);
            Node cur = parent;
            while (cur != root && cur != null) {
                set.addAll(cur.varNames);
                cur = cur.parent;
            }
            return set;
        }

        Node childByMetaInfoExactMatching(String name) {
            Node node = staticChildren.get(name);
            if (null != node) {
                return node;
            }
            if (hasKeywordMatchingChild) {
                node = keywordMatchingChildren.get(Keyword.of(name));
                if (null != node) {
                    return node;
                }
            }
            if (!dynamicChildren.isEmpty()) {
                for (Node targetNode : dynamicChildren) {
                    if (targetNode.metaInfoMatchesExactly(name)) {
                        return targetNode;
                    }
                }
            }
            return node;
        }

        Node childByMetaInfoConflictMatching(String name) {
            if (!dynamicChildren.isEmpty()) {
                for (Node targetNode : dynamicChildren) {
                    if (targetNode.metaInfoConflict(name)) {
                        return targetNode;
                    }
                }
            }
            return null;
        }

        Node findChild(String name) {
            name = name.trim();
            return childByMetaInfoExactMatching(name);
        }

        Node addChild(String name, final String path, final String action) {
            name = name.trim();
            Keyword keyword = null;
            if (S.is(name).wrappedWith(TILDE)) {
                keyword = Keyword.of(S.strip(name).of(TILDE));
                Node node = keywordMatchingChildren.get(keyword);
                if (null == node) {
                    node = new Node(keyword, this);
                    keywordMatchingChildren.put(keyword, node);
                    hasKeywordMatchingChild = true;
                    staticChildren.put(keyword.javaVariable(), node);
                    staticChildren.put(keyword.hyphenated(), node);
                    staticChildren.put(keyword.underscore(), node);
                    return node;
                }
            }
            Node node = null != keyword ? keywordMatchingChildren.get(keyword) : childByMetaInfoExactMatching(name);
            if (null != node) {
                return node;
            }
            Node conflictNode = childByMetaInfoConflictMatching(name);
            Node child = new Node(name, this);
            child.conflictNode = conflictNode;
            if (child.isDynamic()) {
                boolean isAlias = false;
                for (Node targetNode : dynamicChildren) {
                    if (S.eq(targetNode.patternTrait, child.patternTrait)) {
                        targetNode.dynamicAliases.put(UrlPath.of(path), child);
                        targetNode.dynamicReverseAliases.put(action, child);
                        isAlias = true;
                        break;
                    }
                }
                if (!isAlias) {
                    child.dynamicAliases.put(UrlPath.of(path), child);
                    child.dynamicReverseAliases.put(action, child);
                    dynamicChildren.add(child);
                }
                Collections.sort(dynamicChildren);
                return child;
            } else {
                staticChildren.put(name, child);
            }
            return child;
        }

        Node handler(RequestHandler handler, RouteSource source) {
            this.routeSource = $.requireNotNull(source);
            this.handler = handler.requireResolveContext() ? new ContextualHandler((RequestHandlerBase) handler) : handler;
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
            return S.pathConcat(pPath, '/', id());
        }

        String debugPath() {
            if (null == parent) return "/";
            String pPath = parent.path();
            return S.pathConcat(pPath, '/', debugId());
        }

        private String debugId() {
            return null == name ? "~" + keyword.dashed() + "~" : name;
        }

        void debug(H.Method method, PrintStream ps) {
            if (null != handler) {
                ps.printf("%s %s %s\n", method, debugPath(), handler);
            }
            for (Node node : keywordMatchingChildren.values()) {
                node.debug(method, ps);
            }
            for (Node node : staticChildren.values()) {
                if (null != node.name) {
                    node.debug(method, ps);
                }
            }
            for (Node node : dynamicChildren) {
                node.debug(method, ps);
            }
        }

        private void debug(H.Method method, List<RouteInfo> routes, Set<Node> circularReferenceDetector) {
            if (circularReferenceDetector.contains(this)) {
                return;
            }
            circularReferenceDetector.add(this);
            if (null != handler) {
                routes.add(new RouteInfo(method, debugPath(), handler));
            }
            for (Node node : keywordMatchingChildren.values()) {
                node.debug(method, routes, circularReferenceDetector);
            }
            for (Node node : staticChildren.values()) {
                if (null != node.name) {
                    node.debug(method, routes, circularReferenceDetector);
                }
            }
            for (Node node : dynamicChildren) {
                node.debug(method, routes, circularReferenceDetector);
            }
            for (Node node : dynamicAliases.values()) {
                node.debug(method, routes, circularReferenceDetector);
            }
        }

        private void parseDynaName(String name) {
            $.Var<Pattern> patternVar = $.var();
            $.Var<String> patternTraitsVar = $.var();
            boolean isDynamic = parseDynaNameStyleA(name, varNames, patternVar, patternTraitsVar);
            this.isDynamic = isDynamic || parseDynaNameStyleB(
                    name, varNames, patternVar,
                    patternTraitsVar, nodeValueBuilders);
            if (!this.isDynamic) {
                return;
            }
            this.patternTrait = patternTraitsVar.get();
            if (MATCH_ALL != this.patternTrait) {
                this.pattern = patternVar.get();
            }
        }

        /*
         * case one `{var_name<regex>}`, e.g /{foo<[a-b]+>}
         * case two `{<regex>var_name}`, e.g /{<[a-b]+>foo>}
         * case three `foo-{var_name<regex>}-{var_name<regex}-bar...`
         */
        boolean parseDynaNameStyleB(
                String name,
                List<String> varNames,
                @NotNull $.Var<Pattern> pattern,
                @NotNull $.Var<String> patternTrait,
                List<$.Transformer<Map<String, Object>, String>> nodeValueBuilders
        ) {
            int pos = name.indexOf('{');
            if (pos < 0) {
                return false;
            }
            int len = name.length();
            int lastPos = 0;
            int leftPos = pos;
            S.Buffer patternTraitBuilder = S.buffer();
            S.Buffer patternStrBuilder = null == pattern ? null : S.buffer();
            while (leftPos >= 0 & leftPos < len) {
                final String literal = name.substring(lastPos, leftPos);
                if (!literal.isEmpty()) {
                    patternTraitBuilder.append(literal);
                    if (null != pattern) {
                        patternStrBuilder.append(literal);
                    }
                    if (null != nodeValueBuilders) {
                        nodeValueBuilders.add(new $.Transformer<Map<String, Object>, String>() {
                            @Override
                            public String transform(Map<String, Object> stringObjectMap) {
                                return S.string(literal);
                            }
                        });
                    }
                }

                int rightAngle = name.indexOf('>', leftPos);
                if (rightAngle < 0) {
                    if (name.indexOf('<', leftPos) < 0) {
                        rightAngle = leftPos;
                    } else {
                        throw new RoutingException("Invalid route: " + name);
                    }
                }
                pos = name.indexOf('}', rightAngle);
                if (pos < 0) {
                    throw new RuntimeException("Invalid node: " + name);
                }
                $.T2<? extends String, Pattern> t2 = parseVarBlock(name, leftPos + 1, pos);
                final String varName = t2._1;
                if (null != varNames) {
                    addVarName(varName, varNames);
                }
                Pattern pattern1 = t2._2;
                String patternStr = ".*?";
                if (null != pattern1) {
                    patternStr = pattern1.pattern();
                }
                if (null != patternStrBuilder) {
                    patternStrBuilder.append("(?<").append(varName).append(">").append(patternStr).append(")");
                }
                patternTraitBuilder.append("(").append(patternStr).append(")");
                if (null != nodeValueBuilders) {
                    nodeValueBuilders.add(new $.Transformer<Map<String, Object>, String>() {
                        @Override
                        public String transform(Map<String, Object> stringObjectMap) {
                            String s = S.string(varName);
                            s = S.notBlank(s) ? s : "-";
                            return S.string(stringObjectMap.remove(s));
                        }
                    });
                }
                lastPos = pos + 1;
                leftPos = name.indexOf('{', lastPos);
            }
            String literal = name.substring(lastPos, name.length());
            if (!literal.isEmpty()) {
                int literalLen = literal.length();
                if (literal.charAt(literalLen - 1) == '}') {
                    literal = literal.substring(0, literalLen - 1);
                }
                if (!literal.isEmpty()) {
                    final String finalLiteral = literal;
                    patternTraitBuilder.append(literal);
                    if (null != patternStrBuilder) {
                        patternStrBuilder.append(literal);
                    }
                    if (null != nodeValueBuilders) {
                        nodeValueBuilders.add(new $.Transformer<Map<String, Object>, String>() {
                            @Override
                            public String transform(Map<String, Object> stringObjectMap) {
                                return S.string(finalLiteral);
                            }
                        });
                    }
                }
            }
            if (null != pattern) {
                String s = patternStrBuilder.toString();
                String expanded = macroLookup.expand(s);
                if (expanded != s) {
                    pattern.set(Pattern.compile(s));
                } else {
                    try {
                        pattern.set(Pattern.compile(s));
                    } catch (PatternSyntaxException e) {
                        String escaped = escapeUnderscore(s);
                        if (escaped == s) {
                            throw e;
                        }
                        pattern.set(Pattern.compile(escaped));
                    }
                }
            }
            patternTrait.set(patternTraitBuilder.toString().intern());
            return true;
        }

        private static String escapeUnderscore(String s) {
            boolean updated = false;
            S.Buffer buf = S.buffer(s);
            for (int i = 0, l = s.length(); i < l; ++i) {
                if ('_' == s.charAt(i)) {
                    buf.set(i, '7');
                    updated = true;
                }
            }
            return updated ? buf.toString() : s;
        }

        private $.T2<? extends String, Pattern> parseVarBlock(String name, int blockStart, int blockEnd) {
            int pos = name.indexOf('<', blockStart);
            if (pos < 0 || pos >= blockEnd) {
                return $.T2(name.substring(blockStart, blockEnd), null);
            }
            Pattern pattern;
            String varName;
            if (pos == blockStart) {
                pos = name.indexOf('>', blockStart);
                if (pos >= blockEnd) {
                    throw new RoutingException("Invalid route: " + name);
                }
                pattern = Pattern.compile(macroLookup.expand(name.substring(blockStart + 1, pos)));
                varName = name.substring(pos + 1, blockEnd);
            } else {
                if (name.charAt(blockEnd - 1) != '>') {
                    throw new RoutingException("Invalid route: " + name);
                }
                pattern = Pattern.compile(macroLookup.expand(name.substring(pos + 1, blockEnd - 1)));
                varName = name.substring(blockStart, pos);
            }
            return $.T2(varName, pattern);
        }

        /*
         * case one: `var_name:regex`, e.g /foo:[a-b]+
         * case two: `:var_name`, e.g /:foo
         * case three: `var_name:`, e.g /foo:
         */
        boolean parseDynaNameStyleA(
                String name,
                List<String> varNames,
                $.Var<Pattern> pattern,
                $.Var<String> patternTrait
        ) {
            int pos = name.indexOf(':');

            if (pos < 0) {
                return false;
            }

            if (0 == pos) {
                if (null != varNames) {
                    addVarName(name.substring(1), varNames);
                }
            } else {
                int len = name.length();
                if (pos == len - 1) {
                    if (null != varNames) {
                        addVarName(name.substring(0, len - 2), varNames);
                    }
                } else {
                    if (null != varNames) {
                        addVarName(name.substring(0, pos), varNames);
                    }
                    String patternStr = name.substring(pos + 1, name.length());
                    patternStr = macroLookup.expand(patternStr).intern();
                    if (null != pattern && MATCH_ALL != patternStr) {
                        pattern.set(Pattern.compile(patternStr));
                    }
                    patternTrait.set(patternStr);
                }
            }

            return true;
        }

        private static void addVarName(String varName, List<String> varNames) {
            if (varNames.contains(varName)) {
                throw new RouteMappingException("Duplicate URL path variable: " + varName);
            }
            varNames.add(varName);
        }
    }

    private enum BuiltInHandlerDecorator {
        authenticated, external, throttled
    }

    private enum BuiltInHandlerResolver {
        echo() {
            @Override
            public RequestHandler resolve(String msg, App app, EnumSet<BuiltInHandlerDecorator> decorators) {
                return decorators.contains(BuiltInHandlerDecorator.authenticated) ?
                        new ContextualHandler(new AuthenticatedEcho(msg)) :
                        new Echo(msg);
            }
        },
        redirect() {
            @Override
            public RequestHandler resolve(String payload, App app, EnumSet<BuiltInHandlerDecorator> decorators) {
                return decorators.contains(BuiltInHandlerDecorator.authenticated) ?
                        new ContextualHandler(new AuthenticatedRedirect(payload)) :
                        new Redirect(payload);
            }
        },
        moved() {
            @Override
            public RequestHandler resolve(String payload, App app, EnumSet<BuiltInHandlerDecorator> decorators) {
                return decorators.contains(BuiltInHandlerDecorator.authenticated) ?
                        new ContextualHandler(new AuthenticatedRedirect(payload)) :
                        new Redirect(payload);
            }
        },
        redirectdir() {
            @Override
            public RequestHandler resolve(String payload, App app, EnumSet<BuiltInHandlerDecorator> decorators) {
                return decorators.contains(BuiltInHandlerDecorator.authenticated) ?
                        new ContextualHandler(new AuthenticatedRedirectDir(payload)) :
                        new RedirectDir(payload);
            }
        },
        file() {
            @Override
            public RequestHandler resolve(String base, App app, EnumSet<BuiltInHandlerDecorator> decorators) {
                File file = decorators.contains(BuiltInHandlerDecorator.external) ?
                        new File(base) :
                        app.file(base);
                if (!file.canRead()) {
                    LOGGER.warn("file not found: %s", file.getPath());
                }
                return decorators.contains(BuiltInHandlerDecorator.authenticated) ?
                        new ContextualHandler(new AuthenticatedFileGetter(file)) :
                        new FileGetter(file);
            }
        },
        authenticatedfile() {
            @Override
            public RequestHandler resolve(String base, App app, EnumSet<BuiltInHandlerDecorator> decorators) {
                return new ContextualHandler(new AuthenticatedFileGetter(app.file(base)));
            }
        },
        externalfile() {
            @Override
            public RequestHandler resolve(String base, App app, EnumSet<BuiltInHandlerDecorator> decorators) {
                File file = new File(base);
                if (!file.canRead()) {
                    LOGGER.warn("External file not found: %s", file.getPath());
                }
                return decorators.contains(BuiltInHandlerDecorator.authenticated) ?
                        new ContextualHandler(new AuthenticatedFileGetter(file)) :
                        new FileGetter(file);
            }
        },
        resource() {
            @Override
            public RequestHandler resolve(String payload, App app, EnumSet<BuiltInHandlerDecorator> decorators) {
                return decorators.contains(BuiltInHandlerDecorator.authenticated) ?
                        new ContextualHandler(new AuthenticatedResourceGetter(payload)) :
                        new ResourceGetter(payload);
            }
        },
        ws() {
            @Override
            protected RequestHandler resolve(String payload, App app, EnumSet<BuiltInHandlerDecorator> decorators) {
                return createWebSocketConnectionHandler(payload);
            }
        }
        ;

        protected abstract RequestHandler resolve(String payload, App app, EnumSet<BuiltInHandlerDecorator> decorators);

        private static RequestHandler tryResolve(String directive, String payload, App app) {
            String s = directive.toLowerCase();
            String resolver = s, sDecorators;
            BuiltInHandlerResolver r;
            EnumSet<BuiltInHandlerDecorator> decorators = EnumSet.noneOf(BuiltInHandlerDecorator.class);
            int pos = s.indexOf('[');
            if (pos > -1) {
                if (pos > 0) {
                    resolver = s.substring(0, pos);
                    E.illegalArgumentIf(']' != s.charAt(s.length() - 1), "Invalid directive: %s", s);
                    sDecorators = s.substring(pos + 1, s.length() - 1);
                } else {
                    int pos2 = s.indexOf(']');
                    resolver = s.substring(pos2 + 1, s.length());
                    sDecorators = s.substring(1, pos2);
                }
                for (String dec : sDecorators.split(S.COMMON_SEP)) {
                    BuiltInHandlerDecorator decorator = BuiltInHandlerDecorator.valueOf(dec);
                    decorators.add(decorator);
                }
            }
            r = valueOf(resolver);
            try {
                final RequestHandler h = r.resolve(payload, app, decorators);
                if (decorators.contains(BuiltInHandlerDecorator.throttled)) {
                    AppConfig config = app.config();
                    final ThrottleFilter throttleFilter = new ThrottleFilter(config.requestThrottle(), config.requestThrottleExpireScale());
                    return new RequestHandlerBase() {
                        @Override
                        public void handle(ActionContext context) {
                            Result r = throttleFilter.handle(context);
                            if (null == r) {
                                h.handle(context);
                            } else {
                                r.apply(context.req(), context.prepareRespForResultEvaluation());
                            }
                        }

                        @Override
                        public String toString() {
                            return h.toString();
                        }

                        @Override
                        public void prepareAuthentication(ActionContext context) {
                            h.prepareAuthentication(context);
                        }

                        @Override
                        public boolean express(ActionContext context) {
                            return h.express(context);
                        }

                        @Override
                        public boolean skipEvents(ActionContext context) {
                            return h.skipEvents(context);
                        }

                        @Override
                        public boolean sessionFree() {
                            return h.sessionFree();
                        }

                        @Override
                        public CORS.Spec corsSpec() {
                            return h.corsSpec();
                        }

                        @Override
                        public CSRF.Spec csrfSpec() {
                            return h.csrfSpec();
                        }

                        @Override
                        public String contentSecurityPolicy() {
                            return h.contentSecurityPolicy();
                        }

                        @Override
                        public boolean disableContentSecurityPolicy() {
                            return h.disableContentSecurityPolicy();
                        }
                    };
                } else {
                    return h;
                }
            } catch (RuntimeException e) {
                LOGGER.warn(e, "cannot resolve directive %s on payload: %s", directive, payload);
                return null;
            }
        }
    }

    private static class ContextualHandler extends DelegateRequestHandler {
        protected ContextualHandler(RequestHandlerBase next) {
            super(next);
        }

        @Override
        public void handle(ActionContext context) {
            context.handler(realHandler());
            context.resolve();
            Router router = context.router();
            if (router.requireBodyParsing.contains(handler_)) {
                context.markRequireBodyParsing();
            }
            context.proceedWithHandler(this.handler_);
        }
    }

}
