package org.osgl.mvc.server;

import org.osgl.exception.UnexpectedIOException;
import org.osgl.http.H;
import org.osgl.util.E;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

public class MockResponse extends H.Response<MockResponse> {

    private String contentType;
    private String encoding;
    private Locale locale;
    private Writer writer;
    private OutputStream os;

    private int len;

    @Override
    protected Class<MockResponse> _impl() {
        return MockResponse.class;
    }

    @Override
    public Writer writer() throws IllegalStateException, UnexpectedIOException {
        E.illegalStateIf(null != os);
        if (null == writer) {
            writer = new StringWriter();
        }
        return writer;
    }

    @Override
    public OutputStream outputStream() throws IllegalStateException, UnexpectedIOException {
        E.illegalStateIf(null != writer);
        if (null == os) {
            os = new ByteArrayOutputStream();
        }
        return os;
    }

    @Override
    public String characterEncoding() {
        return encoding;
    }

    @Override
    public MockResponse characterEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    @Override
    public MockResponse contentLength(int len) {
        this.len = len;
        return this;
    }

    @Override
    protected void _setContentType(String type) {
        this.contentType = type;
    }

    @Override
    protected void _setLocale(Locale loc) {
        this.locale = loc;
    }

    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    public void addCookie(H.Cookie cookie) {

    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public MockResponse sendError(int sc, String msg) {
        return null;
    }

    @Override
    public MockResponse sendError(int sc) {
        return null;
    }

    @Override
    public MockResponse sendRedirect(String location) {
        return null;
    }

    @Override
    public MockResponse header(String name, String value) {
        return null;
    }

    @Override
    public MockResponse status(int sc) {
        return null;
    }

    @Override
    public MockResponse addHeader(String name, String value) {
        return null;
    }
}
