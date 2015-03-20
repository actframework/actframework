package org.osgl.oms.xio.undertow;

import io.undertow.io.BlockingSenderImpl;
import io.undertow.io.Sender;
import io.undertow.server.BlockingHttpExchange;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.core.BlockingWriterSenderImpl;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import io.undertow.util.AttachmentKey;
import org.osgl.http.H;
import org.osgl.oms.app.AppContext;

import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OmsBlockingExchange implements BlockingHttpExchange {

    public static final AttachmentKey<AppContext> KEY_APP_CTX = AttachmentKey.create(AppContext.class);

    private final HttpServerExchange exchange;

    public OmsBlockingExchange(final HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public InputStream getInputStream() {
        H.Request request = ctx().req();
        return request.inputStream();
    }

    @Override
    public OutputStream getOutputStream() {
        H.Response response = ctx().resp();
        return response.outputStream();
    }

    @Override
    public Sender getSender() {
        H.Response response = ctx().resp();
        if (response.writerCreated()) {
            return new BlockingWriterSenderImpl(exchange, response.writer(), response.characterEncoding());
        } else {
            return new BlockingSenderImpl(exchange, response.outputStream());
        }
    }

    @Override
    public void close() throws IOException {
        AppContext ctx = ctx();
        if (!exchange.isComplete()) {
            try {
                HttpServletRequestImpl request = servletRequestContext.getOriginalRequest();
                request.closeAndDrainRequest();
            } finally {
                HttpServletResponseImpl response = servletRequestContext.getOriginalResponse();
                response.closeStreamAndWriter();
            }
        } else {
            try {
                HttpServletRequestImpl request = servletRequestContext.getOriginalRequest();
                request.freeResources();
            } finally {
                HttpServletResponseImpl response = servletRequestContext.getOriginalResponse();
                response.freeResources();
            }
        }
    }

    private AppContext ctx() {
        return exchange.getAttachment(KEY_APP_CTX);
    }
}
