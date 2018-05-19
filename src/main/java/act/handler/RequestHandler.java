package act.handler;

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

import act.Destroyable;
import act.app.ActionContext;
import act.handler.builtin.FileGetter;
import act.security.CORS;
import act.security.CSRF;
import org.osgl.$;

/**
 * Defines a thread-save function object that can be applied
 * to a {@link ActionContext} context to
 * produce certain output which could be applied to the
 * {@link org.osgl.http.H.Response} associated with the
 * context
 */
public interface RequestHandler extends $.Function<ActionContext, Void>, Destroyable {

    /**
     * Invoke handler upon an action context
     *
     * @param context the context data
     */
    void handle(ActionContext context);

    /**
     * Indicate the handler logic is fast enough to be put into network layer's io thread and
     * does not require to dispatch to worker thread.
     *
     * Note a handler that implements {@link ExpressHandler} should
     * always return `true` for this method
     *
     * @param context the action context
     * @return `true` if this handler support direct io thread processing
     */
    boolean express(ActionContext context);

    /**
     * If this method returns `true` then the following events will not get triggered:
     *
     * * {@link act.handler.event.PreHandle}
     * * {@link act.handler.event.BeforeResultCommit}
     * * {@link act.handler.event.AfterResultCommit}
     * * {@link act.handler.event.PostHandle}
     *
     * @param context the action context.
     * @return `true` if it shall skip triggering result commit events for this handler.
     */
    boolean skipEvents(ActionContext context);

    /**
     * Indicate if this request handler support partial path lookup.
     * Usually this method should return {@code false}. However for
     * certain request handler like {@link FileGetter}
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
     * {@link FileGetter}
     */
    boolean requireResolveContext();

    /**
     * Setting up handler for authentication. For example, initialize the missing authentication
     * handler, csrf failure handler.
     * @param context the current action context
     */
    void prepareAuthentication(ActionContext context);

    /**
     * Returns if the handler is session free or not. If a handler is session free then
     * the framework will NOT resolve session
     * @return `true` if the handler is session free
     */
    boolean sessionFree();

    /**
     * Get CORS specification that specifically applied to this request handler
     * @return the the CORS specification applied to this request handler
     */
    CORS.Spec corsSpec();

    /**
     * Get CSRF specification that applied to this request handler
     *
     * @return the CSRF specification applied to this request handler
     */
    CSRF.Spec csrfSpec();

    /**
     * Get `Content-Security-Policy` that applied to this request handler
     *
     * @return `Content-Security-Policy` that applied to this request handler
     */
    String contentSecurityPolicy();

    boolean disableContentSecurityPolicy();

}
