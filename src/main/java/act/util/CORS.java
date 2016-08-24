package act.util;

import act.Act;
import act.app.ActionContext;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.http.H;
import org.osgl.inject.BeanSpec;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

import static org.osgl.http.H.Header.Names.*;

/**
 * Provice CORS header manipulation methods
 */
public class CORS {

    /**
     * Mark a controller class or action handler method that
     * must not add any CORS headers irregarding to the
     * global CORS setting
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DisableCORS {
    }


    /**
     * Mark a controller class or action handler method that
     * needs to add `Access-Control-Allow-Origin` header
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface AllowOrigin {
        /**
         * The value set to the `Access-Control-Allow-Origin` header
         * @return the value
         */
        String value() default "*";
    }

    /**
     * Mark a controller class or action handler method that
     * needs to add `Access-Control-Allow-Headers` header
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface AllowHeaders {
        /**
         * The value set to the `Access-Control-Allow-Headers` header
         * @return the value
         */
        String value() default "*";
    }

    /**
     * Mark a controller class or action handler method that
     * needs to add `Access-Control-Expose-Headers` header
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface ExposeHeaders {
        /**
         * The value set to the `Access-Control-Expose-Headers` header
         * @return the value
         */
        String value() default "*";
    }

    /**
     * Mark a controller class or action handler method that
     * needs to add `Access-Control-Max-Age` header
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface MaxAge {
        /**
         * The value set to the `Access-Control-Max-Age` header
         * @return the value
         */
        int value() default 30 * 60;
    }

    public static Handler handler(Collection<H.Method> methods) {
        return new Handler(methods);
    }

    public static Handler handler(Class controller) {
        return handler(BeanSpec.of(controller, Act.injector()));
    }

    public static Handler handler(Method action) {
        Type type = Method.class;
        Annotation[] annotations = action.getDeclaredAnnotations();
        return handler(BeanSpec.of(type, annotations, Act.injector()));
    }

    private static Handler handler(BeanSpec spec) {
        return new Handler()
                .with(spec.getAnnotation(DisableCORS.class))
                .with(spec.getAnnotation(AllowOrigin.class))
                .with(spec.getAnnotation(ExposeHeaders.class))
                .with(spec.getAnnotation(AllowHeaders.class))
                .with(spec.getAnnotation(MaxAge.class));
    }

    public static class Handler extends $.Visitor<ActionContext> {

        public static final Handler DUMB = new Handler();

        private boolean disableCORS;
        private String origin;
        private String methods;
        private String exposeHeaders;
        private String allowHeaders;
        private int maxAge = -1;
        private boolean effective = false;

        private Handler(Collection<H.Method> methodSet) {
            E.illegalArgumentIf(methodSet.isEmpty());
            methods = S.join(", ", C.list(methodSet).map($.F.<H.Method>asString()));
            effective = true;
        }

        private Handler() {}

        public boolean effective() {
            return effective;
        }

        public boolean disabled() {
            return disableCORS;
        }

        public Handler with(DisableCORS disableCORS) {
            if (null != disableCORS) {
                this.effective = true;
                this.disableCORS = true;
            }
            return this;
        }

        public Handler with(AllowOrigin allowOrigin) {
            if (null != allowOrigin) {
                this.effective = true;
                origin = allowOrigin.value();
            }
            return this;
        }

        public Handler with(AllowHeaders allowHeaders) {
            if (null != allowHeaders) {
                this.effective = true;
                this.allowHeaders = allowHeaders.value();
            }
            return this;
        }

        public Handler with(ExposeHeaders exposeHeaders) {
            if (null != exposeHeaders) {
                this.effective = true;
                this.exposeHeaders = exposeHeaders.value();
            }
            return this;
        }

        public Handler with(MaxAge maxAge) {
            if (null != maxAge) {
                this.effective = true;
                this.maxAge = maxAge.value();
            }
            return this;
        }

        @Override
        public void visit(ActionContext context) throws Osgl.Break {
            if (!effective) {
                return;
            }
            if (disableCORS) {
                context.disableCORS();
                return;
            }
            H.Response r = context.resp();
            if (null != origin) {
                r.addHeaderIfNotAdded(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }
            if (null != methods) {
                r.addHeaderIfNotAdded(ACCESS_CONTROL_ALLOW_METHODS, methods);
            }
            if (null != exposeHeaders) {
                r.addHeaderIfNotAdded(ACCESS_CONTROL_EXPOSE_HEADERS, exposeHeaders);
            }
            if (null != allowHeaders) {
                r.addHeaderIfNotAdded(ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
            }
            if (-1 < maxAge) {
                r.addHeaderIfNotAdded(ACCESS_CONTROL_MAX_AGE, S.string(maxAge));
            }
        }
        public Handler chain(final Handler next) {
            if (!next.effective()) {
                return this;
            }
            if (!effective()) {
                return next;
            }
            if (next.disabled()) {
                return next;
            }
            if (disabled()) {
                return this;
            }
            final Handler me = this;
            return new Handler() {
                @Override
                public boolean effective() {
                    return true;
                }

                @Override
                public void visit(ActionContext context) throws Osgl.Break {
                    me.visit(context);
                    next.visit(context);
                }
            };
        }
    }

}
