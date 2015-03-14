package org.osgl.oms;

import io.undertow.util.Headers;
import org.osgl.http.H;
import org.osgl.oms.app.AppContext;
import org.osgl.util.E;

import java.util.Locale;

public abstract class ResponseImplBase<T extends ResponseImplBase> extends H.Response<T> {

    private AppContext ctx;
    protected String charset;
    protected Locale locale;

    protected ResponseImplBase(AppContext ctx) {
        E.NPE(ctx);
        this.ctx = ctx;
        charset = ctx.config().encoding();
        locale = ctx.config().locale();
    }

    @Override
    public String characterEncoding() {
        return charset;
    }

    @Override
    public T characterEncoding(String encoding) {
        charset = encoding;
        return me();
    }

    protected final AppContext ctx() {
        return ctx;
    }

    protected final T me() {
        return (T) this;
    }
}
