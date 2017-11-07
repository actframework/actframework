package act;

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

import act.app.ActionContext;
import act.conf.AppConfig;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.MvcConfig;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Redirect;
import org.osgl.util.E;

import java.util.Locale;

public abstract class ActResponse<T extends ActResponse> extends H.Response<T> {

    private boolean ready;
    private boolean closed;
    protected String charset;
    protected Locale locale;
    protected String contentType;
    private boolean charsetSet;

    protected ActResponse() {}

    protected ActResponse(AppConfig config) {
        charset = config.encoding();
        locale = config.locale();
    }

    @Override
    public String characterEncoding() {
        return charset;
    }

    @Override
    public T characterEncoding(String encoding) {
        charset = encoding;
        charsetSet = true;
        return me();
    }

    @Override
    public T sendError(int sc, String msg) {
        throw E.unsupport();
    }

    @Override
    public T sendError(int sc) {
        throw E.unsupport();
    }

    @Override
    public T sendRedirect(String location) {
        throw new Redirect(location);
    }

    /**
     * Mark the framework is ready to write to the response.
     */
    public void markReady() {
       this.ready = true;
    }

    public void commitContentType() {
        header(H.Header.Names.CONTENT_TYPE, _getContentType());
    }

    public boolean isClosed() {
        return closed;
    }

    protected void markClosed() {
        this.closed = true;
    }

    protected void beforeWritingContent() {
        if (ready) {
            return;
        }
        ActionContext ctx = context();
        ctx.dissolve();
        MvcConfig.applyBeforeCommitResultHandler(Ok.get(), ctx.req(), this);
    }

    protected void afterWritingContent() {
        if (ready) {
            return;
        }
        commit();
        ActionContext ctx = context();
        MvcConfig.applyAfterCommitResultHandler(Ok.get(), ctx.req(), this);
    }

    @Override
    protected void _setContentType(String type) {
        this.contentType = type;
    }

    protected String _getContentType() {
        return this.contentType != null ? (this.charsetSet ? this.contentType + ";charset=" + this.charset : this.contentType) : null;
    }

    protected final T me() {
        return (T) this;
    }

    private ActionContext ctx() {
        return $.cast(this.context());
    }
}
