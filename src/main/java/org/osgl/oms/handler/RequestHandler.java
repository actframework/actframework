package org.osgl.oms.handler;

import org.osgl._;
import org.osgl.oms.app.AppContext;

/**
 * Defines a thread-save function object that can be applied
 * to a {@link org.osgl.oms.app.AppContext} context to
 * produce certain output which could be applied to the
 * {@link org.osgl.http.H.Response} associated with the
 * context
 */
public interface RequestHandler extends _.Function<AppContext, Void> {

    /**
     * Invoke handler upon an application context
     * @param context
     */
    void handle(AppContext context);

    /**
     * Indicate if this request handler support partial path lookup.
     * Usually this method should return {@code false}. However for
     * certain request handler like {@link org.osgl.oms.handler.builtin.StaticFileGetter}
     * they need to support partial path lookup. Take the example of
     * the following route mapping:
     *
     * <code>
     * GET /public staticDir: /public
     * </code>
     *
     * which map url path {@code /public} to a {@code StaticFileGetter} with
     * base dir set to {@code /public}, it needs to support all path starts
     * with "/public", like "/public/js/jquery.js" etc.
     *
     * @return {@code true} if the request handler support partial path lookup
     *      or {@code false} otherwise
     */
    boolean supportPartialPath();
}
