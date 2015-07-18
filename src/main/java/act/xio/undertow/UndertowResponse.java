package act.xio.undertow;

import act.ResponseImplBase;
import act.app.ActionContext;
import act.conf.AppConfig;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Locale;

public class UndertowResponse extends ResponseImplBase<UndertowResponse> {
    @Override
    protected Class<UndertowResponse> _impl() {
        return UndertowResponse.class;
    }

    private HttpServerExchange hse;
    private ActionContext ctx;
    private volatile OutputStream os;
    private volatile Writer w;


    public UndertowResponse(HttpServerExchange exchange, AppConfig config) {
        super(config);
        E.NPE(exchange);
        hse = exchange;
    }

    @Override
    public void addCookie(H.Cookie cookie) {
        hse.setResponseCookie(CookieConverter.osgl2undertow(cookie));
    }

    @Override
    public boolean containsHeader(String name) {
        return hse.getResponseHeaders().contains(name);
    }

    @Override
    public UndertowResponse characterEncoding(String encoding) {
        hse.getResponseHeaders().put(HttpString.tryFromString(H.Header.Names.ACCEPT_CHARSET), encoding);
        return this;
    }

    @Override
    public UndertowResponse contentLength(long len) {
        hse.setResponseContentLength(len);
        return this;
    }

    @Override
    protected OutputStream createOutputStream() {
        return hse.getOutputStream();
    }

    @Override
    protected void _setContentType(String type) {
        hse.getResponseHeaders().put(HttpString.tryFromString(H.Header.Names.CONTENT_TYPE), type);
    }

    @Override
    protected void _setLocale(Locale loc) {
        if (responseStarted()) {
            return;
        }
        locale = loc;
        hse.getResponseHeaders().put(Headers.CONTENT_LANGUAGE, loc.getLanguage() + "-" + loc.getCountry());
        if (null != charset && null == w) {
        }
    }

    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    public void commit() {
        hse.endExchange();
    }

    @Override
    public UndertowResponse sendError(int sc, String msg) {
        return null;
    }

    @Override
    public UndertowResponse sendError(int sc) {
        return null;
    }

    @Override
    public UndertowResponse sendRedirect(String location) {
        return null;
    }

    @Override
    public UndertowResponse header(String name, String value) {
        hse.getResponseHeaders().put(new HttpString(name), value);
        return this;
    }

    @Override
    public UndertowResponse status(int sc) {
        hse.setResponseCode(sc);
        return this;
    }

    @Override
    public UndertowResponse addHeader(String name, String value) {
        return null;
    }

    public void closeStreamAndWriter() {
        if (writer != null) {
            IO.close(writer);
        } else {
            IO.close(outputStream());
        }
    }

    public void freeResources() {
        if (writer != null) {
            IO.close(writer);
        } else if (outputStream != null) {
            IO.close(outputStream);
        }
    }


    private boolean responseStarted() {
        return hse.isResponseStarted();
    }

}
