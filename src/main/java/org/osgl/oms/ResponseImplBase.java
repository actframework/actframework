package org.osgl.oms;

import org.osgl.http.H;
import org.osgl.oms.conf.AppConfig;

import java.util.Locale;

public abstract class ResponseImplBase<T extends ResponseImplBase> extends H.Response<T> {

    protected String charset;
    protected Locale locale;

    protected ResponseImplBase(AppConfig config) {
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
        return me();
    }

    protected final T me() {
        return (T) this;
    }
}
