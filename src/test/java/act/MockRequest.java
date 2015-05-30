package act;

import org.osgl.http.H;

import java.io.InputStream;

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
    public H.Format accept() {
        return super.accept();
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
    protected String _ip() {
        return null;
    }

    @Override
    protected void _initCookieMap() {

    }

    @Override
    public InputStream createInputStream() throws IllegalStateException {
        return null;
    }

    @Override
    public String paramVal(String name) {
        return null;
    }

    @Override
    public String[] paramVals(String name) {
        return new String[0];
    }

    @Override
    public Iterable<String> paramNames() {
        return null;
    }
}
