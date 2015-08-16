package act.controller;

import act.app.ActionContext;
import act.conf.AppConfigKey;
import act.view.ActForbidden;
import act.view.ActNotFound;
import act.view.RenderAny;
import act.view.RenderTemplate;
import org.osgl.http.H;
import org.osgl.mvc.result.*;
import org.osgl.storage.ISObject;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.io.InputStream;
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
 * <p>The framework will scan all Classes under the {@link AppConfigKey#CONTROLLER_PACKAGE}
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

    /**
     * Specify the port this controller's action method shall be
     * routed from.
     *
     * @return the port name
     * @see AppConfigKey#NAMED_PORTS
     */
    String port() default "";

    /**
     * Provides utilities for controller action methods to emit rendering results
     */
    public static class Util {

        private static final Ok OK = Ok.INSTANCE;
        private static final NotFound NOT_FOUND = NotFound.INSTANCE;
        private static final Forbidden FORBIDDEN = Forbidden.INSTANCE;
        private static final BadRequest BAD_REQUEST = BadRequest.INSTANCE;

        /**
         * Returns an {@link Ok} result
         */
        public static Result ok() {
            return OK;
        }

        /**
         * Returns an {@link NotFound} result
         */
        public static Result notFound() {
            return new ActNotFound();
        }

        /**
         * Returns an {@link NotFound} result with custom message
         * template and arguments. The final message is rendered with
         * the template and arguments using {@link String#format(String, Object...)}
         * @param msg the message template
         * @param args the message argument
         */
        public static Result notFound(String msg, Object... args) {
            return new ActNotFound(msg, args);
        }

        /**
         * Throws out an {@link NotFound} result if the object specified is
         * {@code null}
         * @param o the object to be evaluated
         */
        public static void notFoundIfNull(Object o) {
            if (null == o) {
                throw new ActNotFound();
            }
        }

        /**
         * Throws out an {@link NotFound} result with custom message template and
         * arguments if the object specified is {@code null}. The final message is
         * rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         * @param o the object to be evaluated
         * @param msg the message template
         * @param args the message argument
         */
        public static void notFoundIfNull(Object o, String msg, Object... args) {
            if (null == o) {
                throw new ActNotFound(msg, args);
            }
        }

        /**
         * Throws out an {@link NotFound} result if the boolean expression specified
         * is {@code true}
         * {@code null}
         * @param test the boolean expression to be evaluated
         */
        public static void notFoundIf(boolean test) {
            if (test) {
                throw new ActNotFound();
            }
        }

        /**
         * Throws out an {@link NotFound} result with custom message template and
         * arguments if the expression specified is {@code true}. The final message is
         * rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         * @param test the boolean expression
         * @param msg the message template
         * @param args the message argument
         */
        public static void notFoundIf(boolean test, String msg, Object... args) {
            if (test) {
                throw new ActNotFound(msg, args);
            }
        }

        /**
         * Throws out an {@link NotFound} result if the boolean expression specified
         * is {@code false}
         * {@code null}
         * @param test the boolean expression to be evaluated
         */
        public static void notFoundIfNot(boolean test) {
            if (!test) {
                throw new ActNotFound();
            }
        }

        /**
         * Throws out an {@link NotFound} result with custom message template and
         * arguments if the expression specified is {@code false}. The final message is
         * rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         * @param test the boolean expression
         * @param msg the message template
         * @param args the message argument
         */
        public static void notFoundIfNot(boolean test, String msg, Object... args) {
            if (!test) {
                throw new ActNotFound(msg, args);
            }
        }

        public static Result badRequest() {
            return BAD_REQUEST;
        }

        /**
         * Returns a {@link Forbidden} result
         */
        public static Result forbidden() {
            return new ActForbidden();
        }

        public static void forbiddenIf(boolean test) {
            if (test) {
                throw new ActForbidden();
            }
        }

        /**
         * Returns a {@link Forbidden} result with custom message
         * template and arguments. The final message is rendered with
         * the template and arguments using {@link String#format(String, Object...)}
         * @param msg the message template
         * @param args the message argument
         */
        public static Result forbidden(String msg, Object... args) {
            return null == msg ? new ActForbidden(): new ActForbidden(msg, args);
        }

        public static Result redirect(String url, Object ... args) {
            return new Redirect(url, args);
        }

        /**
         * Returns a {@link RenderText} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         * @param msg
         * @param args
         */
        public static Result text(String msg, Object... args) {
            return new RenderText(msg, args);
        }

        /**
         * Returns a {@link RenderText} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         * @param msg
         * @param args
         */
        public static Result html(String msg, Object... args) {
            return new RenderHtml(msg, args);
        }

        /**
         * Returns a {@link RenderJSON} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         * @param msg
         * @param args
         */
        public static Result json(String msg, Object... args) {
            return new RenderJSON(msg, args);
        }

        /**
         * Returns a {@link RenderJSON} result with any object. This method will
         * call underline JSON serializer to transform the object into a JSON string
         * @param data
         */
        public static Result json(Object data) {
            return new RenderJSON(data);
        }


        /**
         * Returns a {@link RenderBinary} result with an {@link ISObject} instance. The result will render
         * the binary using "inline" content disposition
         * @param sobj the {@link ISObject} instance
         */
        public static Result binary(ISObject sobj) {
            return new RenderBinary(sobj.asInputStream(), sobj.getAttribute(ISObject.ATTR_FILE_NAME), sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE), true);
        }

        /**
         * Returns a {@link RenderBinary} result with an {@link ISObject} instance. The result will render
         * the binary using "attachment" content disposition
         * @param sobj the {@link ISObject} instance
         */
        public static Result download(ISObject sobj) {
            return new RenderBinary(sobj.asInputStream(), sobj.getAttribute(ISObject.ATTR_FILE_NAME), sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE), false);
        }

        /**
         * Returns a {@link RenderBinary} result with a file. The result will render
         * the binary using "inline" content disposition.
         * @param file the file to be rendered
         */
        public static Result binary(File file) {
            return new RenderBinary(file);
        }

        /**
         * Returns a {@link RenderBinary} result with a file. The result will render
         * the binary using "attachment" content disposition.
         * @param file the file to be rendered
         */
        public static Result download(File file) {
            return new RenderBinary(file, file.getName(), false);
        }

        /**
         * Returns a {@link RenderTemplate} result with a render arguments map.
         * Note the template path should be set via {@link ActionContext#templatePath(String)}
         * method
         * @param args
         */
        public static Result template(Map<String, Object> args) {
            return new RenderTemplate(args);
        }

        /**
         * The caller to this magic {@code render} method is subject to byte code enhancement. All
         * parameter passed into this method will be put into the application context via
         * {@link ActionContext#renderArg(String, Object)} using the variable name found in the
         * local variable table. If the first argument is of type String and there is no variable name
         * associated with that variable then it will be treated as template path and get set to the
         * context via {@link ActionContext#templatePath(String)} method.
         * <p>This method returns different render results depends on the request format</p>
         * <table>
         *     <tr>
         *         <th>Format</th>
         *         <th>Result type</th>
         *     </tr>
         *     <tr>
         *         <td>{@link org.osgl.http.H.Format#json}</td>
         *         <td>A JSON string that map the arguments to their own local variable names</td>
         *     </tr>
         *     <tr>
         *         <td>{@link org.osgl.http.H.Format#html} or any other text formats</td>
         *         <td>{@link RenderTemplate}</td>
         *     </tr>
         *     <tr>
         *         <td>{@link org.osgl.http.H.Format#pdf} or any other binary format</td>
         *         <td>If first argument is of type File or InputStream, then outbound the
         *         content as a binary stream, otherwise throw out {@link org.osgl.exception.UnsupportedException}</td>
         *     </tr>
         * </table>
         *
         * @param args
         */
        public static Result render(Object... args) {
            return new RenderAny();
        }

        /**
         * Kind of like {@link #render(Object...)}, the only differences is this method force to render a template
         * without regarding to the request format
         * @param args
         */
        public static Result renderTemplate(Object ... args) {
            return new RenderTemplate();
        }

        private static void setDefaultContextType(H.Request req, H.Response resp) {
            resp.contentType(req.contentType().toContentType());
        }

        public static Result inferResult(Result r, ActionContext actionContext) {
            return r;
        }

        public static Result inferResult(String s, ActionContext actionContext) {
            if (actionContext.isJSON()) {
                s = s.trim();
                if (!s.startsWith("[") && !s.startsWith("{")) {
                    String action = actionContext.actionPath();
                    s = S.fmt("{\"%s\": \"%s\"}", S.str(action).afterLast('.'), s);
                }
                return new RenderJSON(s);
            }
            H.Format fmt = actionContext.accept();
            switch (fmt) {
                case txt:
                case csv:
                    return new RenderText(fmt, s);
                case html:
                case unknown:
                    return html(s);
                default:
                    throw E.unexpected("Cannot apply text result to format: %s", fmt);
            }
        }

        public static Result inferResult(Map<String, ?> map, ActionContext actionContext) {
            if (actionContext.isJSON()) {
                return new RenderJSON(map);
            }
            throw E.tbd("render template with render args in map");
        }

        /**
         *
         * @param array
         * @param actionContext
         * @return
         */
        public static Result inferResult(Object[] array, ActionContext actionContext) {
            if (actionContext.isJSON()) {
                return new RenderJSON(array);
            }
            throw E.tbd("render template with render args in array");
        }

        /**
         * Infer {@link Result} from an {@link InputStream}. If the current context is in
         * {@code JSON} format then it will render a {@link RenderJSON JSON} result from the content of the
         * input stream. Otherwise, it will render a {@link RenderBinary binary} result from the inputstream
         * @param is the inputstream
         * @param actionContext
         * @return a Result inferred from the inputstream specified
         */
        public static Result inferResult(InputStream is, ActionContext actionContext) {
            if (actionContext.isJSON()) {
                return new RenderJSON(IO.readContentAsString(is));
            } else {
                return new RenderBinary(is, null, true);
            }
        }

        /**
         * Infer {@link Result} from an {@link File}. If the current context is in
         * {@code JSON} format then it will render a {@link RenderJSON JSON} result from the content of the
         * file. Otherwise, it will render a {@link RenderBinary binary} result from the file specified
         * @param file the file
         * @param actionContext
         * @return a Result inferred from the file specified
         */
        public static Result inferResult(File file, ActionContext actionContext) {
            if (actionContext.isJSON()) {
                return new RenderJSON(IO.readContentAsString(file));
            } else {
                return new RenderBinary(file);
            }
        }

        public static Result inferResult(ISObject sobj, ActionContext context) {
            if (context.isJSON()) {
                return new RenderJSON(sobj.asString());
            } else {
                return binary(sobj);
            }
        }

        /**
         * Infer a {@link Result} from a {@link Object object} value v:
         * <ul>
         *     <li>If v is {@code null} then null returned</li>
         *     <li>If v is instance of {@code Result} then it is returned directly</li>
         *     <li>If v is instance of {@code String} then {@link #inferResult(String, ActionContext)} is used
         *     to infer the {@code Result}</li>
         *     <li>If v is instance of {@code InputStream} then {@link #inferResult(InputStream, ActionContext)} is used
         *     to infer the {@code Result}</li>
         *     <li>If v is instance of {@code File} then {@link #inferResult(File, ActionContext)} is used
         *     to infer the {@code Result}</li>
         *     <li>If v is instance of {@code Map} then {@link #inferResult(Map, ActionContext)} is used
         *     to infer the {@code Result}</li>
         *     <li>If v is an array of {@code Object} then {@link #inferResult(Object[], ActionContext)} is used
         *     to infer the {@code Result}</li>
         * </ul>
         * @param v
         * @param actionContext
         * @return
         */
        public static Result inferResult(Object v, ActionContext actionContext) {
            if (null == v) {
                return null;
            } else if (v instanceof Result) {
                return (Result) v;
            } else if (v instanceof String) {
                return inferResult((String) v, actionContext);
            } else if (v instanceof InputStream) {
                return inferResult((InputStream) v, actionContext);
            } else if (v instanceof File) {
                return inferResult((File) v, actionContext);
            } else if (v instanceof ISObject) {
                return inferResult((ISObject) v, actionContext);
            } else if (v instanceof Map) {
                return inferResult((Map) v, actionContext);
            } else if (v instanceof Object[]) {
                return inferResult((Object[]) v, actionContext);
            } else {
                if (actionContext.isJSON()) {
                    return new RenderJSON(v);
                } else {
                    return inferResult(v.toString(), actionContext);
                }
            }
        }
    }

}
