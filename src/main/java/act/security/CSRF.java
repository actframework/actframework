package act.security;

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.conf.AppConfig;
import act.util.MissingAuthenticationHandler;
import org.osgl.Osgl;
import org.osgl.http.H;
import org.osgl.inject.BeanSpec;
import org.osgl.util.S;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static act.app.ActionContext.ATTR_CSR_TOKEN_PREFETCH;
import static act.app.ActionContext.ATTR_WAS_UNAUTHENTICATED;

public class CSRF {

    /**
     * Mark a controller class or action handler method that
     * requires the CSRF protection. This can be used
     * to construct the whitelist of CSRF protection resources
     * when the global {@link act.conf.AppConfigKey#CSRF} is
     * disabled
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface Enable {
    }

    /**
     * Mark a controller class or action handler method that
     * is not subject to CSRF protection. This can be used
     * to construct the blacklist of CSRF protection resources
     * when the global {@link act.conf.AppConfigKey#CSRF} is
     * enabled
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface Disable {
    }

    public static String calcCsrfToken(H.Session session) {
        App app = App.instance();
        String id = session.id();
        String username = session.get(app.config().sessionKeyUsername());
        StringBuilder sb = new StringBuilder(id);
        if (S.notBlank(username)) {
            sb.append(username);
        }
        String payload = sb.toString();
        String sign = app.sign(payload);
        String toBeEncrypted = S.builder(payload).append("-").append(sign).toString();
        return app.encrypt(toBeEncrypted);
    }

    public static Spec spec(Class controller) {
        return spec(BeanSpec.of(controller, Act.injector()));
    }

    public static Spec spec(Method action) {
        Type type = Method.class;
        Annotation[] annotations = action.getDeclaredAnnotations();
        return spec(BeanSpec.of(type, annotations, Act.injector()));
    }

    private static Spec spec(BeanSpec beanSpec) {
        if (null != beanSpec.getAnnotation(Enable.class)) {
            return new Spec(true);
        } else if (null != beanSpec.getAnnotation(Disable.class)) {
            return new Spec(false);
        }
        return Spec.DUMB;
    }


    public static class Spec {

        public static final Spec DUMB = new Spec() {
            @Override
            public void check(ActionContext context) {
                // do nothing implementation
            }
        };

        private Boolean enabled;
        private String paramName;
        private String headerName;
        private String cookieName;
        private String cookieDomain;

        private Spec() {this(null);}

        private Spec(Boolean enabled) {
            AppConfig config = Act.appConfig();
            boolean globalEnabled = config.csrfEnabled();
            this.enabled = null == enabled ? globalEnabled : enabled;
            if (!this.enabled) {
                return;
            }
            this.paramName = config.csrfParamName();
            this.headerName = config.csrfHeaderName();
            this.cookieName = config.csrfCookieName();
            this.cookieDomain = config.cookieDomain();
        }

        private boolean effective() {
            return this != DUMB;
        }

        /**
         * Do sanity check to see if CSRF token is present. This method
         * is called before session resolved
         *
         * @param context the current context
         */
        public void preCheck(ActionContext context) {
            if (!enabled) {
                return;
            }
            H.Method method = context.req().method();
            if (method.safe()) {
                return;
            }
            String token = retrieveCsrfToken(context);
            if (S.blank(token)) {
                raiseCsrfNotVerified(context);
            }
        }

        /**
         * Check CSRF token after session resolved
         * @param context the current context
         */
        public void check(ActionContext context) {
            String token = context.attribute(ATTR_CSR_TOKEN_PREFETCH);
            if (S.neq(calcCsrfToken(context.session()), token)) {
                raiseCsrfNotVerified(context);
            }
        }

        public CSRF.Spec chain(final Spec next) {
            return effective() ? this : next;
        }

        public void setCookieAndRenderArgs(ActionContext context) {
            if (!enabled) {
                return;
            }
            String token = retrieveCsrfToken(context);
            if (S.blank(token) || justLoggedIn(context)) {
                token = calcCsrfToken(context.session());
                AppConfig config = context.config();
                H.Cookie cookie = new H.Cookie(cookieName, token);
                cookie.domain(cookieDomain);
                cookie.path("/");
                cookie.domain(config.host());
                context.resp().addCookie(cookie);
            }
            context.renderArg(paramName, token);
        }

        private String retrieveCsrfToken(ActionContext context) {
            String token = context.attribute(ATTR_CSR_TOKEN_PREFETCH);
            if (S.blank(token)) {
                token = context.req().header(headerName);
            }
            if (S.blank(token)) {
                token = context.paramVal(paramName);
            }
            if (S.notBlank(token)) {
                context.attribute(ATTR_CSR_TOKEN_PREFETCH, token);
            }
            return token;
        }

        private static boolean justLoggedIn(ActionContext context) {
            boolean wasUnauthenticated = context.attribute(ATTR_WAS_UNAUTHENTICATED);
            return wasUnauthenticated && context.isLoggedIn();
        }

        private static void raiseCsrfNotVerified(ActionContext context) {
            AppConfig config = context.config();
            MissingAuthenticationHandler handler = context.isAjax() ? config.ajaxMissingAuthenticationHandler() : config.missingAuthenticationHandler();
            context.removeAttribute(ATTR_CSR_TOKEN_PREFETCH);
            throw handler.result(context);
        }

    }

}
