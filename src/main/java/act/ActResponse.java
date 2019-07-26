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
import org.osgl.storage.impl.SObject;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.File;
import java.net.URL;
import java.util.Locale;

public abstract class ActResponse<T extends ActResponse> extends H.Response<T> {

    private boolean onResult;
    private boolean closed;
    protected String charset;
    protected Locale locale;
    protected String contentType;
    protected H.Format fmt;
    private boolean charsetSet;
    private boolean committed;
    protected int statusCode = -1;

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

    public T send(URL url) {
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            return send(new File(url.getPath()));
        }
        writeBinary(SObject.of(IO.inputStream(url)));
        return me();
    }

    public T send(File file) {
        writeBinary(SObject.of(file));
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

    public T contentType(H.Format fmt) {
        contentType(fmt.contentType());
        this.fmt = fmt;
        return me();
    }

    public H.Format lastContentType() {
        return fmt;
    }

    /**
     * This response is ready for Result evaluation.
     */
    public void onResult() {
       this.onResult = true;
    }

    public T commitContentType() {
        header(H.Header.Names.CONTENT_TYPE, _getContentType());
        return me();
    }

    @Override
    public void close() {
        super.close();
        markClosed();
    }

    public boolean isClosed() {
        return closed;
    }

    protected void markClosed() {
        this.closed = true;
        ActionContext ctx = context();
        ctx.markAsReadyForClose();
    }

    protected abstract void _setStatusCode(int sc);

    @Override
    public T status(int sc) {
        E.illegalArgumentIf(sc < 100, "Invalid status code");
        statusCode = sc;
        _setStatusCode(sc);
        return me();
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    public ActResponse beforeWritingContent() {
        if (onResult) {
            return this;
        }
        ActionContext ctx = context();
        ctx.dissolve();
        MvcConfig.applyBeforeCommitResultHandler(Ok.get(), ctx.req(), this);
        return this;
    }

    public ActResponse afterWritingContent() {
        if (committed) {
            return this;
        }
        commit();
        ActionContext ctx = context();
        MvcConfig.applyAfterCommitResultHandler(Ok.get(), ctx.req(), this);
        return this;
    }

    @Override
    public final void commit() {
        if (isCommitted()) {
            return;
        }
        committed = true;
        doCommit();
    }

    protected abstract void doCommit();

    protected final boolean isCommitted() {
        return committed;
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
