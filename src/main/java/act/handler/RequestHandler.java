package act.handler;

import act.Destroyable;
import act.handler.builtin.StaticFileGetter;
import org.osgl._;
import act.app.AppContext;

/**
 * Defines a thread-save function object that can be applied
 * to a {@link AppContext} context to
 * produce certain output which could be applied to the
 * {@link org.osgl.http.H.Response} associated with the
 * context
 */
public interface RequestHandler extends _.Function<AppContext, Void>, Destroyable {

    /**
     * Invoke handler upon an application context
     *
     * @param context
     */
    void handle(AppContext context);

    /**
     * Indicate if this request handler support partial path lookup.
     * Usually this method should return {@code false}. However for
     * certain request handler like {@link StaticFileGetter}
     * they need to support partial path lookup. Take the example of
     * the following route mapping:
     * <p/>
     * <code>
     * GET /public staticDir: /public
     * </code>
     * <p/>
     * which map url path {@code /public} to a {@code StaticFileGetter} with
     * base dir set to {@code /public}, it needs to support all path starts
     * with "/public", like "/public/js/jquery.js" etc.
     *
     * @return {@code true} if the request handler support partial path lookup
     * or {@code false} otherwise
     */
    boolean supportPartialPath();

    /**
     * Returns if the handler require framework to resolve context. Usually it needs to
     * resolve the context so that handler can access request params, session/flash etc.
     * However some static handlers doesn't require framework to do those things, e.g.
     * {@link StaticFileGetter}
     */
    boolean requireResolveContext();

}
