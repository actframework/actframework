package act.route;

import act.RequestImplBase;
import act.conf.AppConfig;
import org.osgl.http.H;

import java.io.InputStream;

public class MockRequest extends RequestImplBase<MockRequest> {
    private H.Method method;
    private String url;
    public MockRequest(AppConfig config, H.Method method, String url) {
        super(config);
        this.method = method;
        this.url = url;
    }

    @Override
    protected String methodName() {
        return method.name();
    }

    @Override
    protected Class<MockRequest> _impl() {
        return MockRequest.class;
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
    public String path() {
        return url;
    }

    @Override
    public String query() {
        return null;
    }

    @Override
    protected String _ip() {
        return null;
    }

    @Override
    protected void _initCookieMap() {

    }

    @Override
    protected InputStream createInputStream() {
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
