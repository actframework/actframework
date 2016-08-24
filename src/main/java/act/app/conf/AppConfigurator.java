package act.app.conf;

import act.app.event.AppEventId;
import act.conf.AppConfig;
import act.route.RouteSource;
import act.route.Router;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Base class for app developer implement source code based configuration
 */
public abstract class AppConfigurator<T extends AppConfigurator> extends AppConfig<T> {

    protected static Logger logger = AppConfig.logger;

    protected static final H.Method GET = H.Method.GET;
    protected static final H.Method POST = H.Method.POST;
    protected static final H.Method PUT = H.Method.PUT;
    protected static final H.Method DELETE = H.Method.DELETE;

    private transient Set<String> controllerClasses = C.newSet();
    private Map<String, Object> userProps = C.newMap();

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return obj.getClass() == getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    @Override
    protected void releaseResources() {
        controllerClasses.clear();
        userProps.clear();
        releaseAppConfigResources();
        super.releaseResources();
    }

    protected T registerStringValueResolver(Class<T> targetType, StringValueResolver<T> resolver) {
        app().resolverManager().register(targetType, resolver);
        return me();
    }

    protected CorsSetting cors() {
        return new CorsSetting(this);
    }

    protected RouteBuilder route(String path) {
        return new RouteBuilder(this).map(path);
    }

    protected RouteBuilder route(H.Method ... methods) {
        return new RouteBuilder(this).on(methods);
    }

    public void onRouteAdded(String controllerClassName) {
        controllerClasses.add(controllerClassName);
    }

    public Set<String> controllerClasses() {
        return C.newSet(controllerClasses);
    }

    protected T prop(String key, Object val) {
        userProps.put(key, val);
        return me();
    }

    public Set<String> propKeys() {
        return userProps.keySet();
    }

    public <V> V propVal(String key) {
        return $.cast(userProps.get(key));
    }

    /**
     * Sub class shall override this method to do the configuration
     */
    public abstract void configure();

    protected void releaseAppConfigResources() {}

    protected static class CorsSetting {
        private AppConfigurator conf;
        private boolean enabled;
        private String allowOrigin;
        private int maxAge;
        private List<String> methods = new ArrayList<>();
        private List<String> headersBoth = new ArrayList<>();
        private List<String> headersAllowed = new ArrayList<>();
        private List<String> headersExpose = new ArrayList<>();

        CorsSetting(AppConfigurator conf) {
            this.conf = conf;
            this.enabled = true;
            conf.app().jobManager().on(AppEventId.CONFIG_PREMERGE, new Runnable() {
                @Override
                public void run() {
                    checkAndCommit();
                }
            });
        }

        public CorsSetting enable() {
            enabled = true;
            return this;
        }

        public CorsSetting disable() {
            enabled = false;
            return this;
        }

        public CorsSetting allowOrigin(String allowOrigin) {
            E.illegalArgumentIf(S.blank(allowOrigin), "allow origin cannot be empty");
            this.allowOrigin = allowOrigin;
            return this;
        }

        public CorsSetting allowMethods(String ... methods) {
            this.methods.addAll(C.listOf(methods));
            return this;
        }

        public CorsSetting maxAge(int maxAge) {
            E.illegalArgumentIf(maxAge < 0);
            this.maxAge = maxAge;
            return this;
        }

        public CorsSetting allowHeaders(String ... headers) {
            headersAllowed.addAll(C.listOf(headers));
            return this;
        }

        public CorsSetting exposeHeaders(String ... headers) {
            headersExpose.addAll(C.listOf(headers));
            return this;
        }

        public CorsSetting allowAndExposeHeaders(String ... headers) {
            headersBoth.addAll(C.listOf(headers));
            return this;
        }

        private void checkAndCommit() {
            if (!enabled) {
                logger.info("Global CORS is disabled");
                conf.enableCors(false);
                return;
            }
            logger.info("Global CORS is enabled");
            conf.enableCors(true);
            conf.corsAllowOrigin(allowOrigin);
            conf.corsHeaders(consolidate(headersBoth));
            conf.corsAllowHeaders(consolidate(headersAllowed));
            conf.corsHeadersExpose(consolidate(headersExpose));
            conf.corsAllowMethods(consolidate(methods));
            conf.corsMaxAge(maxAge);
        }

        private String consolidate(List<String> stringList) {
            if (stringList.isEmpty()) {
                return null;
            }
            Set<String> set = new HashSet<>();
            for (String s : stringList) {
                set.addAll(C.listOf(s.split(",")));
            }
            return S.join(", ", set);
        }
    }

    protected static class RouteBuilder {

        private AppConfigurator conf;
        private Router router;
        private C.List<H.Method> methods = C.newListOf(Router.supportedHttpMethods());
        private String path;
        private String action;
        RouteBuilder(AppConfigurator config) {
            router = config.app().router();
            E.illegalStateIf(null == router);
            conf = config;
        }
        public RouteBuilder on(H.Method ... methods) {
            E.illegalArgumentIf(0 == methods.length, "Http method expected");
            this.methods.clear();
            this.methods.append(C.listOf(methods));
            return this;
        }
        public RouteBuilder map(String path) {
            E.illegalArgumentIf(S.blank(path), "path cannot be empty");
            this.path = path.trim();
            return this;
        }
        public RouteBuilder to(String action) {
            E.illegalArgumentIf(S.blank(action), "action cannot be empty");
            this.action = action;
            checkAndCommit();
            String controllerClass = FastStr.of(action).beforeLast('.').toString();
            if (!controllerClass.contains(":")) {
                // no directive so it's a real controller class
                conf.onRouteAdded(controllerClass);
            }
            return this;
        }
        public RouteBuilder to(Class<?> controller, String actionMethod) {
            E.NPE(controller);
            E.illegalArgumentIf(S.blank(actionMethod), "action method cannot be empty");
            E.illegalArgumentIf(actionMethod.contains("."), "action method cannot contain [.]");
            this.action = S.builder(controller.getCanonicalName()).append(".").append(actionMethod.trim()).toString();
            checkAndCommit();
            conf.onRouteAdded(controller.getCanonicalName());
            return this;
        }
        private static Set<String> NON_ACTIONS = C.newSet(C.listOf("equals", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait"));
        public RouteBuilder to(Class<?> controller) {
            E.illegalStateIf(S.blank(path), "route path not specified");
            String context = controller.getCanonicalName();
            logger.warn("wildcard matching %s's all public methods into router", context);
            Method[] methods = controller.getMethods();
            boolean isAbstract = Modifier.isAbstract(controller.getModifiers());
            boolean mapped = false;
            for (Method m: methods) {
                if (isAbstract && !Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                String name = m.getName();
                if (NON_ACTIONS.contains(name)) {
                    continue;
                }
                CharSequence path = S.builder(this.path).append("/").append(name);
                CharSequence action = S.builder(context).append(".").append(name);
                for (H.Method method : this.methods) {
                    router.addMapping(method, path, action, RouteSource.APP_CONFIG);
                }
                mapped = true;
            }
            if (!mapped) {
                logger.warn("No public methods found in the class %s, route not added", context);
            } else {
                conf.onRouteAdded(controller.getCanonicalName());
            }
            this.methods.clear();
            path = null;
            action = null;
            return this;
        }

        private void checkAndCommit() {
            E.illegalStateIf(S.blank(path), "route path not specified");
            for (H.Method method: methods) {
                router.addMapping(method, path, action, RouteSource.APP_CONFIG);
            }
            methods.clear();
            path = null;
            action = null;
        }
    }

}
