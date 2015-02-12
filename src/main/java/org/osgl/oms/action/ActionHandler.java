package org.osgl.oms.action;

import org.osgl._;
import org.osgl.oms.app.AppContext;

/**
 * ActionInvoker is a thread-save function object that can be applied
 * to a {@link org.osgl.oms.app.AppContext} context
 */
public interface ActionHandler extends _.Function<AppContext, Void> {

    /**
     * Invoke the action with given context
     * @param context
     */
    void invoke(AppContext context);

    /**
     * Indicate if this action invoker support partial path lookup.
     * Usually this method should return {@code false}. However for
     * certain action invoker like {@link org.osgl.oms.action.builtin.StaticFileGetter}
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
     * @return {@code true} if the action invoker support partial path lookup
     *      or {@code false} otherwise
     */
    boolean supportPartialPath();
}
