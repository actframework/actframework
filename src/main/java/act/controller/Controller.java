package act.controller;

import act.app.AppContext;
import act.conf.AppConfigKey;
import act.view.ActServerError;
import act.view.RenderAny;
import act.view.RenderTemplate;
import org.osgl.http.H;
import org.osgl.mvc.result.*;
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
        public static Ok ok() {
            return OK;
        }

        /**
         * Returns an {@link NotFound} result
         */
        public static NotFound notFound() {
            return NOT_FOUND;
        }

        /**
         * Returns an {@link NotFound} result with custom message
         * template and arguments. The final message is rendered with
         * the template and arguments using {@link String#format(String, Object...)}
         * @param msg the message template
         * @param args the message argument
         */
        public static NotFound notFound(String msg, Object... args) {
            return new NotFound(msg, args);
        }

        /**
         * Throws out an {@link NotFound} result if the object specified is
         * {@code null}
         * @param o the object to be evaluated
         */
        public static void notFoundIfNull(Object o) {
            if (null == o) {
                throw notFound();
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
                throw notFound(msg, args);
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
                throw notFound();
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
                throw notFound(msg, args);
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
                throw notFound();
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
                throw notFound(msg, args);
            }
        }

        public static BadRequest badRequest() {
            return BAD_REQUEST;
        }

        /**
         * Returns a {@link Forbidden} result
         */
        public static Forbidden forbidden() {
            return FORBIDDEN;
        }

        public static void forbiddenIf(boolean test) {
            if (test) {
                throw forbidden();
            }
        }

        /**
         * Returns a {@link Forbidden} result with custom message
         * template and arguments. The final message is rendered with
         * the template and arguments using {@link String#format(String, Object...)}
         * @param msg the message template
         * @param args the message argument
         */
        public static Forbidden forbidden(String msg, Object... args) {
            return null == msg ? FORBIDDEN : new Forbidden(msg, args);
        }

        public static Redirect redirect(String url, Object ... args) {
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
         * Returns a {@link RenderTemplate} result with a render arguments map.
         * Note the template path should be set via {@link AppContext#templatePath(String)}
         * method
         * @param args
         */
        public static Result template(Map<String, Object> args) {
            return new RenderTemplate(args);
        }

        /**
         * The caller to this magic {@code render} method is subject to byte code enhancement. All
         * parameter passed into this method will be put into the application context via
         * {@link AppContext#renderArg(String, Object)} using the variable name found in the
         * local variable table. If the first argument is of type String and there is no variable name
         * associated with that variable then it will be treated as template path and get set to the
         * context via {@link AppContext#templatePath(String)} method.
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

        public static Result inferResult(Result r, AppContext appContext) {
            return r;
        }

        public static Result inferResult(String s, AppContext appContext) {
            if (appContext.isJSON()) {
                s = s.trim();
                if (!s.startsWith("[") && !s.startsWith("{")) {
                    String action = appContext.actionPath();
                    s = S.fmt("{\"%s\": \"%s\"}", S.str(action).afterLast('.'), s);
                }
                return new RenderJSON(s);
            }
            H.Format fmt = appContext.accept();
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

        public static Result inferResult(Map<String, ?> map, AppContext appContext) {
            if (appContext.isJSON()) {
                return new RenderJSON(map);
            }
            throw E.tbd("render template with render args in map");
        }

        /**
         *
         * @param array
         * @param appContext
         * @return
         */
        public static Result inferResult(Object[] array, AppContext appContext) {
            if (appContext.isJSON()) {
                return new RenderJSON(array);
            }
            throw E.tbd("render template with render args in array");
        }

        /**
         * Infer {@link Result} from an {@link InputStream}. If the current context is in
         * {@code JSON} format then it will render a {@link RenderJSON JSON} result from the content of the
         * input stream. Otherwise, it will render a {@link RenderBinary binary} result from the inputstream
         * @param is the inputstream
         * @param appContext
         * @return a Result inferred from the inputstream specified
         */
        public static Result inferResult(InputStream is, AppContext appContext) {
            if (appContext.isJSON()) {
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
         * @param appContext
         * @return a Result inferred from the file specified
         */
        public static Result inferResult(File file, AppContext appContext) {
            if (appContext.isJSON()) {
                return new RenderJSON(IO.readContentAsString(file));
            } else {
                return new RenderBinary(file);
            }
        }

        /**
         * Infer a {@link Result} from a {@link Object object} value v:
         * <ul>
         *     <li>If v is {@code null} then null returned</li>
         *     <li>If v is instance of {@code Result} then it is returned directly</li>
         *     <li>If v is instance of {@code String} then {@link #inferResult(String, AppContext)} is used
         *     to infer the {@code Result}</li>
         *     <li>If v is instance of {@code InputStream} then {@link #inferResult(InputStream, AppContext)} is used
         *     to infer the {@code Result}</li>
         *     <li>If v is instance of {@code File} then {@link #inferResult(File, AppContext)} is used
         *     to infer the {@code Result}</li>
         *     <li>If v is instance of {@code Map} then {@link #inferResult(Map, AppContext)} is used
         *     to infer the {@code Result}</li>
         *     <li>If v is an array of {@code Object} then {@link #inferResult(Object[], AppContext)} is used
         *     to infer the {@code Result}</li>
         * </ul>
         * @param v
         * @param appContext
         * @return
         */
        public static Result inferResult(Object v, AppContext appContext) {
            if (null == v) {
                return null;
            } else if (v instanceof Result) {
                return (Result) v;
            } else if (v instanceof String) {
                return inferResult((String) v, appContext);
            } else if (v instanceof InputStream) {
                return inferResult((InputStream) v, appContext);
            } else if (v instanceof File) {
                return inferResult((File) v, appContext);
            } else if (v instanceof Map) {
                return inferResult((Map) v, appContext);
            } else if (v instanceof Object[]) {
                return inferResult((Object[]) v, appContext);
            } else {
                if (appContext.isJSON()) {
                    return new RenderJSON(v);
                } else {
                    return inferResult(v.toString(), appContext);
                }
            }
        }
    }

}
