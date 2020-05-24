package act.security;

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

import static org.osgl.http.H.Header.Names.*;

import act.Act;
import act.app.ActionContext;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.inject.BeanSpec;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;

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
    public @interface Disable {
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
     * needs to add `Access-Control-Allow-Credentials` header
     * and set the value to `false`
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DisallowCredentials {
    }

    /**
     * Mark a controller class or action handler method that
     * needs to add `Access-Control-Allow-Credentials` header
     * and set the value to `true`
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface AllowCredentials {
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

    public static Spec spec(Collection<H.Method> methods) {
        return new Spec(methods);
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
        return new Spec()
                .with(beanSpec.getAnnotation(Disable.class))
                .with(beanSpec.getAnnotation(AllowOrigin.class))
                .with(beanSpec.getAnnotation(ExposeHeaders.class))
                .with(beanSpec.getAnnotation(AllowCredentials.class))
                .with(beanSpec.getAnnotation(DisallowCredentials.class))
                .with(beanSpec.getAnnotation(AllowHeaders.class))
                .with(beanSpec.getAnnotation(MaxAge.class));
    }

    public static class Spec extends $.Visitor<ActionContext> {

        public static final Spec DUMB = new Spec() {

            @Override
            public void visit(ActionContext context) throws $.Break {
                // do nothing implementation
            }

            @Override
            public void applyTo(ActionContext context) throws $.Break {
                // do nothing implementation
            }
        };

        private boolean disableCORS;
        private String origin;
        private String methods;
        private String exposeHeaders;
        private String allowHeaders;
        private Boolean allowCredentials;
        private int maxAge = -1;
        private boolean effective = false;

        private Spec(Collection<H.Method> methodSet) {
            E.illegalArgumentIf(methodSet.isEmpty());
            methods = S.join(", ", C.list(methodSet).map($.F.<H.Method>asString()));
            effective = true;
        }

        private Spec() {}

        public boolean effective() {
            return effective;
        }

        public boolean disabled() {
            return disableCORS;
        }

        public Spec with(Disable disableCORS) {
            if (null != disableCORS) {
                this.effective = true;
                this.disableCORS = true;
            }
            return this;
        }

        public Spec with(AllowOrigin allowOrigin) {
            if (null != allowOrigin) {
                this.effective = true;
                origin = allowOrigin.value();
            }
            return this;
        }

        public Spec with(AllowHeaders allowHeaders) {
            if (null != allowHeaders) {
                this.effective = true;
                this.allowHeaders = allowHeaders.value();
            }
            return this;
        }

        public Spec with(ExposeHeaders exposeHeaders) {
            if (null != exposeHeaders) {
                this.effective = true;
                this.exposeHeaders = exposeHeaders.value();
            }
            return this;
        }

        public Spec with(AllowCredentials allowCredentials) {
            if (null != allowCredentials) {
                this.effective = true;
                this.allowCredentials = true;
            }
            return this;
        }

        public Spec with(DisallowCredentials disallowCredentials) {
            if (null != disallowCredentials) {
                this.effective = true;
                this.allowCredentials = false;
            }
            return this;
        }

        public Spec with(MaxAge maxAge) {
            if (null != maxAge) {
                this.effective = true;
                this.maxAge = maxAge.value();
            }
            return this;
        }

        @Override
        public void visit(ActionContext context) throws $.Break {
            applyTo(context);
        }

        public void applyTo(ActionContext context) throws $.Break {
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
            if (null != exposeHeaders) {
                r.addHeaderIfNotAdded(ACCESS_CONTROL_EXPOSE_HEADERS, exposeHeaders);
            }
            if (null != allowCredentials) {
                r.addHeaderIfNotAdded(ACCESS_CONTROL_ALLOW_CREDENTIALS, S.string(allowCredentials));
            }
            if (context.isOptionsMethod()) {
                if (null != methods) {
                    r.addHeaderIfNotAdded(ACCESS_CONTROL_ALLOW_METHODS, methods);
                }
                if (null != allowHeaders) {
                    r.addHeaderIfNotAdded(ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
                }
                if (-1 < maxAge) {
                    r.addHeaderIfNotAdded(ACCESS_CONTROL_MAX_AGE, S.string(maxAge));
                }
            }
        }
        public Spec chain(final Spec next) {
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
            final Spec me = this;
            return new Spec() {
                @Override
                public boolean effective() {
                    return true;
                }

                @Override
                public void applyTo(ActionContext context) throws $.Break {
                    me.visit(context);
                    next.visit(context);
                }

            };
        }
    }

}
