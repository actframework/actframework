package org.osgl.mvc.server;

import org.osgl.http.H;

import java.io.InputStream;
import java.io.Reader;

public class MockRequest extends H.Request<MockRequest> {
    @Override
    protected Class<MockRequest> _impl() {
        return MockRequest.class;
    }

    @Override
    public H.Method method() {
        return null;
    }

    @Override
    public String header(String name) {
        return null;
    }

    @Override
    public Iterable<String> headers(String name) {
        return null;
    }

    @Override
    public H.Format format() {
        return super.format();
    }

    @Override
    public boolean isAjax() {
        return super.isAjax();
    }

    @Override
    public String path() {
        return null;
    }

    @Override
    public String contextPath() {
        return null;
    }

    @Override
    public String query() {
        return null;
    }

    @Override
    public boolean secure() {
        return false;
    }

    @Override
    protected String _remoteAddr() {
        return null;
    }

    @Override
    protected void _initCookieMap() {

    }

    @Override
    public InputStream inputStream() throws IllegalStateException {
        return null;
    }

    @Override
    public Reader reader() throws IllegalStateException {
        return null;
    }

    @Override
    public String param(String name) {
        return null;
    }

    @Override
    public String[] allParam(String name) {
        return new String[0];
    }

    @Override
    public Iterable<String> paramNames() {
        return null;
    }
}
