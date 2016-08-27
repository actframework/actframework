package act.security;

import act.Act;
import act.app.App;
import act.conf.AppConfig;
import org.osgl.inject.BeanSpec;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

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
        return new Spec();
    }


    public static class Spec {
        private Boolean enabled;
        private String paramName;
        private String headerName;
        private String cookieName;

        private Spec() {this(null);}

        private Spec(Boolean enabled) {
            AppConfig config = Act.appConfig();
            boolean globalEnabled = config.csrfEnabled();
            this.enabled = null == enabled ? globalEnabled : enabled;
            if (!enabled) {
                return;
            }
            this.paramName = config.csrfParamName();
            this.headerName = config.csrfHeaderName();
            this.cookieName = config.csrfCookieName();
        }


    }

}
