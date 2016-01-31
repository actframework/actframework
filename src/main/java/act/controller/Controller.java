package act.controller;

import act.Act;
import act.app.ActionContext;
import act.conf.AppConfigKey;
import act.controller.meta.HandlerMethodMetaInfo;
import act.util.ActContext;
import act.util.FastJsonIterable;
import act.util.PropertySpec;
import act.view.*;
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
import java.util.Collection;
import java.util.Map;

import static org.osgl.http.H.Format.*;

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
     * Specify the port(s) this controller's action method shall be
     * routed from.
     *
     * @return the port name
     * @see AppConfigKey#NAMED_PORTS
     */
    String[] port() default {};

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
         * Returns a {@link Created} result
         *
         * @param resourceGetUrl the URL to access the new resource been created
         * @return the result as described
         */
        public static Result created(String resourceGetUrl) {
            return new Created(resourceGetUrl);
        }

        /**
         * Returns a {@link Accepted} result
         *
         * @param statusMonitorUrl the URL to check the request process status
         * @return the result as described
         */
        public static Result accepted(String statusMonitorUrl) {
            return new Accepted(statusMonitorUrl);
        }

        /**
         * Returns an {@link NotFound} result
         */
        public static Result notFound() {
            return ActNotFound.create();
        }

        /**
         * Returns an {@link NotFound} result with custom message
         * template and arguments. The final message is rendered with
         * the template and arguments using {@link String#format(String, Object...)}
         *
         * @param msg  the message template
         * @param args the message argument
         */
        public static Result notFound(String msg, Object... args) {
            return ActNotFound.create(msg, args);
        }

        /**
         * Throws out an {@link NotFound} result if the object specified is
         * {@code null}
         *
         * @param o the object to be evaluated
         */
        public static void notFoundIfNull(Object o) {
            if (null == o) {
                throw ActNotFound.create();
            }
        }

        /**
         * Throws out an {@link NotFound} result with custom message template and
         * arguments if the object specified is {@code null}. The final message is
         * rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param o    the object to be evaluated
         * @param msg  the message template
         * @param args the message argument
         */
        public static void notFoundIfNull(Object o, String msg, Object... args) {
            if (null == o) {
                throw ActNotFound.create(msg, args);
            }
        }

        /**
         * Throws out an {@link NotFound} result if the boolean expression specified
         * is {@code true}
         * {@code null}
         *
         * @param test the boolean expression to be evaluated
         */
        public static void notFoundIf(boolean test) {
            if (test) {
                throw ActNotFound.create();
            }
        }

        /**
         * Throws out an {@link NotFound} result with custom message template and
         * arguments if the expression specified is {@code true}. The final message is
         * rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param test the boolean expression
         * @param msg  the message template
         * @param args the message argument
         */
        public static void notFoundIf(boolean test, String msg, Object... args) {
            if (test) {
                throw ActNotFound.create(msg, args);
            }
        }

        /**
         * Throws out an {@link NotFound} result if the boolean expression specified
         * is {@code false}
         * {@code null}
         *
         * @param test the boolean expression to be evaluated
         */
        public static void notFoundIfNot(boolean test) {
            notFoundIf(!test);
        }

        /**
         * Throws out an {@link NotFound} result with custom message template and
         * arguments if the expression specified is {@code false}. The final message is
         * rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param test the boolean expression
         * @param msg  the message template
         * @param args the message argument
         */
        public static void notFoundIfNot(boolean test, String msg, Object... args) {
            notFoundIf(!test, msg, args);
        }

        public static Result badRequest() {
            return ActBadRequest.create();
        }

        public static void badRequestIf(boolean test) {
            if (test) {
                throw ActBadRequest.create();
            }
        }

        public static void badRequestIf(boolean test, String msg, Object... args) {
            if (test) {
                throw ActBadRequest.create(msg, args);
            }
        }

        public static void badRequestIfNot(boolean test) {
            if (!test) {
                throw ActBadRequest.create();
            }
        }

        public static void badRequestIfNot(boolean test, String msg, Object... args) {
            badRequestIf(!test, msg, args);
        }

        public static Result conflict() {
            return ActConflict.create();
        }

        public static Result conflict(String message, Object... args) {
            return ActConflict.create(message, args);
        }

        public static void conflictIf(boolean test) {
            if (test) {
                throw ActConflict.create();
            }
        }

        public static void conflictIf(boolean test, String message, Object... args) {
            if (test) {
                throw ActConflict.create(message, args);
            }
        }

        public static void conflictIfNot(boolean test) {
            conflictIf(!test);
        }

        public static void conflictIfNot(boolean test, String message, Object... args) {
            conflictIf(!test, message, args);
        }

        /**
         * Returns a {@link Forbidden} result
         */
        public static Result forbidden() {
            return ActForbidden.create();
        }

        /**
         * Throws a {@link Forbidden} result if the test condition is {@code true}
         *
         * @param test the test condition
         */
        public static void forbiddenIf(boolean test) {
            if (test) {
                throw ActForbidden.create();
            }
        }

        /**
         * Throws a {@link Forbidden} result if the test condition is {@code false}
         *
         * @param test the test condition
         */
        public static void forbiddenIfNot(boolean test) {
            forbiddenIf(!test);
        }

        /**
         * Returns a {@link Forbidden} result with custom message
         * template and arguments. The final message is rendered with
         * the template and arguments using {@link String#format(String, Object...)}
         *
         * @param msg  the message template
         * @param args the message argument
         */
        public static Result forbidden(String msg, Object... args) {
            return ActForbidden.create(msg, args);
        }

        /**
         * Throws a {@link Forbidden} result if test condition is {@code true}
         * @param test the test condition
         * @param msg the message format template
         * @param args the message format arguments
         */
        public static void forbiddenIf(boolean test, String msg, Object... args) {
            if (test) {
                throw ActForbidden.create(msg, args);
            }
        }

        /**
         * Throws a {@link Forbidden} result if the test condition is {@code false}
         *
         * @param test the test condition
         * @param msg the message format template
         * @param args the message format arguments
         */
        public static void forbiddenIfNot(boolean test, String msg, Object... args) {
            forbiddenIf(!test, msg, args);
        }

        public static Result redirect(String url, Object... args) {
            return new Redirect(url, args);
        }

        /**
         * Returns a {@link RenderText} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param msg the message format template
         * @param args the message format arguments
         */
        public static Result text(String msg, Object... args) {
            return new RenderText(msg, args);
        }

        /**
         * Returns a {@link RenderText} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param msg the message format template
         * @param args the message format arguments
         */
        public static Result html(String msg, Object... args) {
            return new RenderHtml(msg, args);
        }

        /**
         * Returns a {@link RenderJSON} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param msg the message format template
         * @param args the message format arguments
         */
        public static Result json(String msg, Object... args) {
            return new RenderJSON(msg, args);
        }

        /**
         * Returns a {@link RenderJSON} result with any object. This method will
         * call underline JSON serializer to transform the object into a JSON string
         *
         * @param data the data to be rendered as JSON string
         */
        public static Result json(Object data) {
            return new RenderJSON(data);
        }


        /**
         * Returns a {@link RenderBinary} result with an {@link ISObject} instance. The result will render
         * the binary using "inline" content disposition
         *
         * @param sobj the {@link ISObject} instance
         */
        public static Result binary(ISObject sobj) {
            return new RenderBinary(sobj.asInputStream(), sobj.getAttribute(ISObject.ATTR_FILE_NAME), sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE), true);
        }

        /**
         * Returns a {@link RenderBinary} result with an {@link ISObject} instance. The result will render
         * the binary using "attachment" content disposition
         *
         * @param sobj the {@link ISObject} instance
         */
        public static Result download(ISObject sobj) {
            return new RenderBinary(sobj.asInputStream(), sobj.getAttribute(ISObject.ATTR_FILE_NAME), sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE), false);
        }

        /**
         * Returns a {@link RenderBinary} result with a file. The result will render
         * the binary using "inline" content disposition.
         *
         * @param file the file to be rendered
         */
        public static Result binary(File file) {
            return new RenderBinary(file);
        }

        /**
         * Returns a {@link RenderBinary} result with a file. The result will render
         * the binary using "attachment" content disposition.
         *
         * @param file the file to be rendered
         */
        public static Result download(File file) {
            return new RenderBinary(file, file.getName(), false);
        }

        /**
         * Returns a {@link RenderTemplate} result with a render arguments map.
         * Note the template path should be set via {@link ActionContext#templatePath(String)}
         * method
         *
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
         * <tr>
         * <th>Format</th>
         * <th>Result type</th>
         * </tr>
         * <tr>
         * <td>{@link org.osgl.http.H.Format#json}</td>
         * <td>A JSON string that map the arguments to their own local variable names</td>
         * </tr>
         * <tr>
         * <td>{@link org.osgl.http.H.Format#html} or any other text formats</td>
         * <td>{@link RenderTemplate}</td>
         * </tr>
         * <tr>
         * <td>{@link org.osgl.http.H.Format#pdf} or any other binary format</td>
         * <td>If first argument is of type File or InputStream, then outbound the
         * content as a binary stream, otherwise throw out {@link org.osgl.exception.UnsupportedException}</td>
         * </tr>
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
         *
         * @param args
         */
        public static Result renderTemplate(Object... args) {
            return new RenderTemplate();
        }

        private static void setDefaultContextType(H.Request req, H.Response resp) {
            resp.contentType(req.contentType().contentType());
        }

        public static Result inferResult(Result r, ActionContext actionContext) {
            return r;
        }

        public static Result inferResult(String s, ActionContext actionContext) {
            if (actionContext.isJSON()) {
                s = s.trim();
                if (!s.startsWith("[") && !s.startsWith("{")) {
                    s = S.fmt("{\"result\": \"%s\"}", s);
                }
                return new RenderJSON(s);
            }
            H.Format fmt = actionContext.accept();
            if (HTML == fmt || H.Format.UNKNOWN == fmt) {
                return html(s);
            }
            if (TXT == fmt || CSV == fmt) {
                return new RenderText(fmt, s);
            }
            if (XML == fmt) {
                s = s.trim();
                if (!s.startsWith("<") && !s.endsWith(">")) {
                    s = S.fmt("<result>%s</result>", s);
                }
                return new RenderText(fmt, s);
            }
            throw E.unexpected("Cannot apply text result to format: %s", fmt);
        }

        public static Result inferResult(Map<String, Object> map, ActionContext actionContext) {
            if (actionContext.isJSON()) {
                return new RenderJSON(map);
            }
            return new RenderTemplate(map);
        }

        /**
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
         *
         * @param is            the inputstream
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
         *
         * @param file          the file
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
         * <li>If v is {@code null} then null returned</li>
         * <li>If v is instance of {@code Result} then it is returned directly</li>
         * <li>If v is instance of {@code String} then {@link #inferResult(String, ActionContext)} is used
         * to infer the {@code Result}</li>
         * <li>If v is instance of {@code InputStream} then {@link #inferResult(InputStream, ActionContext)} is used
         * to infer the {@code Result}</li>
         * <li>If v is instance of {@code File} then {@link #inferResult(File, ActionContext)} is used
         * to infer the {@code Result}</li>
         * <li>If v is instance of {@code Map} then {@link #inferResult(Map, ActionContext)} is used
         * to infer the {@code Result}</li>
         * <li>If v is an array of {@code Object} then {@link #inferResult(Object[], ActionContext)} is used
         * to infer the {@code Result}</li>
         * </ul>
         *
         * @param v
         * @param actionContext
         * @return
         */
        public static Result inferResult(HandlerMethodMetaInfo meta, Object v, ActionContext actionContext, boolean hasTemplate) {
            if (null == v && !hasTemplate) {
                return null;
            } else if (v instanceof Result) {
                return (Result) v;
            } else if (v instanceof String) {
                if (hasTemplate) {
                    return inferToTemplate(v, actionContext);
                }
                return inferResult((String) v, actionContext);
            } else if (v instanceof InputStream) {
                return inferResult((InputStream) v, actionContext);
            } else if (v instanceof File) {
                return inferResult((File) v, actionContext);
            } else if (v instanceof ISObject) {
                return inferResult((ISObject) v, actionContext);
            } else if (v instanceof Map) {
                if (hasTemplate) {
                    return inferToTemplate((Map) v, actionContext);
                }
                return inferResult((Map) v, actionContext);
            } else if (v instanceof Object[]) {
                if (hasTemplate) {
                    throw E.tbd("Render template with array");
                }
                return inferResult((Object[]) v, actionContext);
            } else {
                if (hasTemplate) {
                    return inferToTemplate(v, actionContext);
                }
                if (actionContext.isJSON()) {
                    // patch https://github.com/alibaba/fastjson/issues/478
                    if (v instanceof Iterable && !(v instanceof Collection)) {
                        v = new FastJsonIterable<>((Iterable) v);
                    }
                    PropertySpec.MetaInfo propertySpec = (null == meta) ? null : meta.propertySpec();
                    if (null == propertySpec) {
                        return new RenderJSON(v);
                    }
                    return new FilteredRenderJSON(v, propertySpec, actionContext);
                } else {
                    return inferResult(v.toString(), actionContext);
                }
            }
        }

        private static Result inferToTemplate(Object v, ActionContext actionContext) {
            actionContext.renderArg("result", v);
            return new RenderTemplate();
        }

        private static Result inferToTemplate(Map map, ActionContext actionContext) {
            return new RenderTemplate(map);
        }
    }

}
