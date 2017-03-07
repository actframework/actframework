package act.controller;

import act.Act;
import act.app.ActionContext;
import act.conf.AppConfigKey;
import act.controller.meta.HandlerMethodMetaInfo;
import act.data.Versioned;
import act.util.DisableFastJsonCircularReferenceDetect;
import act.util.FastJsonIterable;
import act.util.PropertySpec;
import act.view.*;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.http.H;
import org.osgl.mvc.result.*;
import org.osgl.storage.ISObject;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
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
    class Util {

        public static final Ok OK = Ok.get();
        public static final Created CREATED = Created.INSTANCE;
        public static final Result CREATED_JSON = new Result(H.Status.CREATED, "{\"message\": \"Created\"}") {};
        public static final Result CREATED_XML = new Result(H.Status.CREATED, "<?xml version=\"1.0\" ?><message>Created</message>") {};
        public static final Result OK_JSON = new Result(H.Status.OK, "{\"message\": \"Okay\"}") {};
        public static final Result OK_XML = new Result(H.Status.OK, "<?xml version=\"1.0\" ?><message>Okay</message>") {};
        public static final NoContent NO_CONTENT = NoContent.get();

        /**
         * Returns an {@link Ok} result
         */
        public static Result ok() {
            H.Format accept = ActionContext.current().accept();
            if (H.Format.JSON == accept) {
                return OK_JSON;
            } else if (H.Format.XML == accept) {
                return OK_XML;
            }
            return OK;
        }

        /**
         * Returns a {@link Created} result
         *
         * @param resourceGetUrl the URL to access the new resource been created
         * @return the result as described
         */
        public static Created created(String resourceGetUrl) {
            return Created.withLocation(resourceGetUrl);
        }

        /**
         * Return a {@link Created} result
         * @return the result as described
         */
        public static Created created() {
            return Created.INSTANCE;
        }

        public static NotModified notModified() {
            return NotModified.get();
        }

        public static NotModified notModified(String etag, Object... args) {
            return NotModified.of(etag, args);
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

        public static Result notAcceptable() {
            return NotAcceptable.get();
        }

        public static Result notAcceptable(String msg, Object... args) {
            return NotAcceptable.of(msg, args);
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
        public static <T> T notFoundIfNull(T o) {
            if (null == o) {
                throw ActNotFound.create();
            }
            return o;
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
        public static <T> T notFoundIfNull(T o, String msg, Object... args) {
            if (null == o) {
                throw ActNotFound.create(msg, args);
            }
            return o;
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

        public static void badRequestIfBlank(String test) {
            if (S.blank(test)) {
                throw ActBadRequest.create();
            }
        }

        public static void badRequestIfBlank(String test, String msg, Object... args) {
            if (S.blank(test)) {
                throw ActBadRequest.create(msg, args);
            }
        }

        public static void badRequestIfNull(Object test) {
            if (null == test) {
                throw ActBadRequest.create();
            }
        }

        public static void badRequestIfNull(Object test, String msg, Object... args) {
            if (null == test) {
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

        public static Result unauthorized() {
            return ActUnauthorized.create();
        }

        public static Result unauthorized(String realm) {
            return ActUnauthorized.create(realm);
        }

        public static void unauthorizedIf(boolean test) {
            if (test) {
                throw ActUnauthorized.create();
            }
        }

        public static void unauthorizedIf(boolean test, String realm) {
            if (test) {
                throw ActUnauthorized.create(realm);
            }
        }

        public static void unauthorizedIfNot(boolean test) {
            unauthorizedIf(!test);
        }

        public static void unauthorizedIfNot(boolean test, String realm) {
            unauthorizedIf(!test, realm);
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
         *
         * @param test the test condition
         * @param msg  the message format template
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
         * @param msg  the message format template
         * @param args the message format arguments
         */
        public static void forbiddenIfNot(boolean test, String msg, Object... args) {
            forbiddenIf(!test, msg, args);
        }

        public static Result redirect(String url, Object... args) {
            return Redirect.of(url, args);
        }

        /**
         * Returns a {@link RenderText} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param msg  the message format template
         * @param args the message format arguments
         */
        public static Result text(String msg, Object... args) {
            return RenderText.of(successStatus(), msg, args);
        }

        /**
         * Alias of {@link #text(String, Object...)}
         * @param msg  the message format template
         * @param args the message format arguments
         * @return the result
         */
        public static Result renderText(String msg, Object... args) {
            return text(msg, args);
        }

        /**
         * Returns a {@link RenderText} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param msg  the message format template
         * @param args the message format arguments
         * @return the result
         */
        public static Result html(String msg, Object... args) {
            return RenderHtml.of(successStatus(), msg, args);
        }

        /**
         * Alias of {@link #html(String, Object...)}
         * @param msg  the message format template
         * @param args the message format arguments
         * @return the result
         */
        public static Result renderHtml(String msg, Object args) {
            return html(msg, args);
        }

        /**
         * Returns a {@link RenderJSON} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param msg  the message format template
         * @param args the message format arguments
         * @return the result
         */
        public static Result json(String msg, Object... args) {
            return RenderJSON.of(successStatus(), msg, args);
        }

        /**
         * Alias of {@link #json(String, Object...)}
         * @param msg the message format template
         * @param args the message format arguments
         * @return the result
         */
        public static Result renderJson(String msg, Object... args) {
            return json(msg, args);
        }

        /**
         * Returns a {@link RenderJSON} result with any object. This method will
         * call underline JSON serializer to transform the object into a JSON string
         *
         * @param data the data to be rendered as JSON string
         * @return the result
         */
        public static Result json(Object data) {
            return RenderJSON.of(successStatus(), data);
        }

        /**
         * Alias of {@link #json(Object)}
         * @param data the data to be rendered as JSON string
         * @return the result
         */
        public static Result renderJson(Object data) {
            return json(data);
        }

        /**
         * Returns a {@link RenderJsonMap} result with any object. This method will
         * generate a JSON object out from the {@link ActionContext#renderArgs}.
         * The response is always in JSON format and ignores the HTTP `Accept`
         * header setting
         * @param data the varargs of Object to be put into the JSON map
         * @return the result
         */
        public static Result jsonMap(Object... data) {
            return RenderJsonMap.get();
        }

        /**
         * Alias of {@link #jsonMap(Object...)}
         * @param data the data to be put into the JSON map
         * @return the result
         */
        public static Result renderJsonMap(Object ... data) {
            return jsonMap(data);
        }


        /**
         * Returns a {@link RenderXML} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param msg  the message format template
         * @param args the message format arguments
         * @return the result
         */
        public static Result xml(String msg, Object... args) {
            return RenderXML.of(successStatus(), msg, args);
        }

        /**
         * Alias of {@link #xml(String, Object...)}
         * @param msg the message format template
         * @param args the message format arguments
         * @return the result
         */
        public static Result renderXml(String msg, Object... args) {
            return xml(msg, args);
        }

        /**
         * Returns a {@link RenderBinary} result with an {@link ISObject} instance. The result will render
         * the binary using "inline" content disposition
         *
         * @param sobj the {@link ISObject} instance
         * @return the result
         */
        public static Result binary(ISObject sobj) {
            return new RenderBinary(sobj.asInputStream(), sobj.getAttribute(ISObject.ATTR_FILE_NAME), sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE), true);
        }

        /**
         * Alias of {@link #binary(ISObject)}
         * @param sobj the {@link ISObject} instance
         * @return the result
         */
        public static Result renderBinary(ISObject sobj) {
            return binary(sobj);
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
         * @return a result
         */
        public static Result binary(File file) {
            return new RenderBinary(file);
        }

        /**
         * Alias of {@link #binary(File)}
         * @param file the file to be rendered
         * @return a result
         */
        public static Result renderBinary(File file) {
            return binary(file);
        }

        /**
         * Returns a {@link RenderBinary} result with a delayed output stream writer.
         * The result will render the binary using "inline" content disposition.
         *
         * @param outputStreamWriter the delayed writer
         * @return the result
         */
        public static Result binary($.Function<OutputStream, ?> outputStreamWriter) {
            return new RenderBinary(outputStreamWriter);
        }

        /**
         * Alias of {@link #binary(Osgl.Function)}
         * @param outputStreamWriter the delayed writer
         * @return the result
         */
        public static Result renderBinary($.Function<OutputStream, ?> outputStreamWriter) {
            return binary(outputStreamWriter);
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
         * Render barcode for given content
         * @param content the content to generate the barcode
         * @return the barcode as a binary result
         */
        public static ZXingResult barcode(String content) {
            return ZXingResult.barcode(content);
        }

        /**
         * Alias of {@link #barcode(String)}
         * @param content the content to generate the barcode
         * @return the barcode as a binary result
         */
        public static ZXingResult renderBarcode(String content) {
            return barcode(content);
        }

        /**
         * Render QRCode for given content
         * @param content the content to generate the qrcode
         * @return the qrcode as a binary result
         */
        public static ZXingResult qrcode(String content) {
            return ZXingResult.qrcode(content);
        }

        /**
         * Alias of {@link #qrcode(String)}
         * @param content the content to generate the barcode
         * @return the barcode as a binary result
         */
        public static ZXingResult renderQrcode(String content) {
            return qrcode(content);
        }

        /**
         * Returns a {@link RenderTemplate} result with a render arguments map.
         * Note the template path should be set via {@link ActionContext#templatePath(String)}
         * method
         *
         * @param args the template arguments
         * @return a result to render template
         */
        public static Result template(Map<String, Object> args) {
            return RenderTemplate.of(args);
        }

        /**
         * Alias of {@link #template(Map)}
         *
         * @param args the template arguments
         * @return a result to render template
         */
        public static Result renderTemplate(Map<String, Object> args) {
            return template(args);
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
            return RenderAny.get();
        }

        /**
         * This method is deprecated, please use {@link #template(Object...)} instead
         *
         * @param args template argument list
         */
        public static Result renderTemplate(Object... args) {
            return RenderTemplate.get();
        }

        /**
         * Kind of like {@link #render(Object...)}, the only differences is this method force to render a template
         * without regarding to the request format
         *
         * @param args template argument list
         */
        public static Result template(Object... args) {
            return RenderTemplate.get(ActionContext.current().successStatus());
        }

        public static Result inferResult(Result r, ActionContext actionContext) {
            return r;
        }

        public static Result inferResult(String s, ActionContext actionContext) {
            final H.Status status = actionContext.successStatus();
            if (actionContext.acceptJson()) {
                s = s.trim();
                if (!s.startsWith("[") && !s.startsWith("{")) {
                    s = S.fmt("{\"result\": \"%s\"}", org.rythmengine.utils.S.escapeJSON(s));
                }
                return RenderJSON.of(status, s);
            }
            H.Format fmt = actionContext.accept();
            if (HTML == fmt || H.Format.UNKNOWN == fmt) {
                return RenderHtml.of(status, s);
            }
            if (TXT == fmt || CSV == fmt) {
                return RenderText.of(status, fmt, s);
            }
            if (XML == fmt) {
                s = s.trim();
                if (!s.startsWith("<") && !s.endsWith(">")) {
                    s = S.fmt("<result>%s</result>", s);
                }
                return RenderText.of(status, fmt, s);
            }
            throw E.unexpected("Cannot apply text result to format: %s", fmt);
        }

        public static Result inferResult(Map<String, Object> map, ActionContext actionContext) {
            if (actionContext.acceptJson()) {
                return RenderJSON.of(map);
            }
            return RenderTemplate.of(map);
        }

        /**
         * @param array
         * @param actionContext
         * @return
         */
        public static Result inferResult(Object[] array, ActionContext actionContext) {
            if (actionContext.acceptJson()) {
                return RenderJSON.of(actionContext.successStatus(), array);
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
            if (actionContext.acceptJson()) {
                return RenderJSON.of(IO.readContentAsString(is));
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
            if (actionContext.acceptJson()) {
                return RenderJSON.of(IO.readContentAsString(file));
            } else {
                return new RenderBinary(file);
            }
        }

        public static Result inferResult(ISObject sobj, ActionContext context) {
            if (context.acceptJson()) {
                return RenderJSON.of(sobj.asString());
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
         * @param meta the HandlerMethodMetaInfo
         * @param v the value to be rendered
         * @param context the action context
         * @param hasTemplate a boolean flag indicate if the current handler method has corresponding template
         * @return the rendered result
         */
        public static Result inferResult(HandlerMethodMetaInfo meta, Object v, ActionContext context, boolean hasTemplate) {
            if (v instanceof Result) {
                return (Result)v;
            }
            final H.Request req = context.req();
            final H.Status status = req.method() == H.Method.POST ? H.Status.CREATED : H.Status.OK;
            if (Act.isProd() && v instanceof Versioned && req.method().safe()) {
                processEtag(meta, v, context, req);
            }
            if (hasTemplate) {
                if (v instanceof Map) {
                    return inferToTemplate(((Map) v), context);
                }
                return inferToTemplate(v, context);
            }
            if (null == v) {
                return null;
            } else if (v instanceof String) {
                return inferResult((String) v, context);
            } else if (v instanceof InputStream) {
                return inferResult((InputStream) v, context);
            } else if (v instanceof File) {
                return inferResult((File) v, context);
            } else if (v instanceof ISObject) {
                return inferResult((ISObject) v, context);
            } else if (v instanceof Map) {
                return RenderJSON.of(v);
            } else {
                if (context.acceptJson()) {
                    // patch https://github.com/alibaba/fastjson/issues/478
                    if (meta.disableJsonCircularRefDetect()) {
                        DisableFastJsonCircularReferenceDetect.option.set(true);
                    }
                    if (v instanceof Iterable && !(v instanceof Collection)) {
                        v = new FastJsonIterable((Iterable) v);
                    }
                    PropertySpec.MetaInfo propertySpec = PropertySpec.MetaInfo.withCurrent(meta, context);
                    try {
                        if (null == propertySpec) {
                            return RenderJSON.of(status, v);
                        }
                        return FilteredRenderJSON.get(status, v, propertySpec, context);
                    } finally {
                        if (meta.disableJsonCircularRefDetect()) {
                            DisableFastJsonCircularReferenceDetect.option.set(false);
                        }
                    }
                } else if (context.acceptXML()) {
                    PropertySpec.MetaInfo propertySpec = PropertySpec.MetaInfo.withCurrent(meta, context);
                    return new FilteredRenderXML(v, propertySpec, context);
                } else if (context.accept() == H.Format.CSV) {
                    PropertySpec.MetaInfo propertySpec = PropertySpec.MetaInfo.withCurrent(meta, context);
                    return RenderCSV.get(status, v, propertySpec, context);
                } else {
                    String s = meta.returnType().getDescriptor().startsWith("[") ? $.toString2(v) : v.toString();
                    return inferResult(s, context);
                }
            }
        }

        private static void processEtag(HandlerMethodMetaInfo meta, Object v, ActionContext context, H.Request req) {
            if (!(v instanceof Versioned)) {
                return;
            }
            String version = ((Versioned) v)._version();
            String etagVersion = etag(meta, version);
            if (req.etagMatches(etagVersion)) {
                throw NotModified.get();
            } else {
                context.resp().etag(etagVersion);
            }
        }

        private static String etag(HandlerMethodMetaInfo meta, String version) {
            return S.newBuffer(version).append(meta.hashCode()).toString();
        }

        private static Result inferToTemplate(Object v, ActionContext actionContext) {
            actionContext.renderArg("result", v);
            return RenderTemplate.get();
        }

        private static Result inferToTemplate(Map map, ActionContext actionContext) {
            return RenderTemplate.of(map);
        }

        private static H.Status successStatus() {
            return ActionContext.current().successStatus();
        }
    }

}
