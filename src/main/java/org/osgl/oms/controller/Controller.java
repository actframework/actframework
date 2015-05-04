package org.osgl.oms.controller;

import org.osgl.http.H;
import org.osgl.mvc.result.Forbidden;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * Mark a class as Controller, which contains at least one of the following:
 * <ul>
 * <li>Action handler method</li>
 * <li>Any one of Before/After/Exception/Finally interceptor</li>
 * </ul>
 * <p>The framework will scan all Classes under the {@link org.osgl.oms.conf.AppConfigKey#CONTROLLER_PACKAGE}
 * or any class outside of the package but with this annotation marked</p>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Controller {

    /**
     * Indicate the context path for all action methods declared
     * in this controller.
     * <p/>
     * <p>Default value: "{@code /}"</p>
     *
     * @return the controller context path
     */
    String value() default "/";

    public static enum Util {
        ;

        private static final Result OK = new Ok();
        private static final Result NOT_FOUND = new NotFound();

        public static Result Ok() {
            return OK;
        }

        public static void ok() {
            throw OK;
        }

        public static Result NotFound() {
            return NOT_FOUND;
        }

        public static void notFound() {
            throw NOT_FOUND;
        }

        public static Result NotFound(String msg, Object... args) {
            return new NotFound(msg, args);
        }

        public static void notFound(String msg, Object... args) {
            throw new NotFound(msg, args);
        }

        public static void notFoundIfNull(Object o) {
            if (null == o) {
                notFound();
            }
        }

        public static void notFoundIfNull(Object o, String msg, Object... args) {
            if (null == o) {
                notFound(msg, args);
            }
        }

        public static void notFoundIf(boolean test) {
            if (test) {
                notFound();
            }
        }

        public static void notFoundIf(boolean test, String msg, Object... args) {
            if (test) {
                notFound(msg, args);
            }
        }

        public static void notFoundIfNot(boolean test) {
            if (!test) {
                notFound();
            }
        }

        public static void notFoundIfNot(boolean test, String msg, Object... args) {
            if (!test) {
                notFound(msg, args);
            }
        }

        public static void forbidden() {
            throw new Forbidden();
        }

        public static void forbidden(String msg) {
            throw new Forbidden(msg);
        }

        public static void print(String msg, Object... args) {
            if (S.empty(msg)) ok();
            AppContext ctx = AppContext.get();
            H.Request req = ctx.req();
            H.Response resp = ctx.resp();
            print(req, resp, msg, args);
        }

        public static void print(H.Request req, H.Response resp, String msg, Object... args) {
            setDefaultContextType(req, resp);
            IO.writeContent(S.fmt(msg, args), resp.writer());
        }

        public static void renderTemplate(String template, Map<String, Object> args) {

        }

        private static void setDefaultContextType(H.Request req, H.Response resp) {
            resp.contentType(req.contentType());
        }

    }

}
