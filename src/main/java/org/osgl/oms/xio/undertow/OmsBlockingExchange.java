package org.osgl.oms.xio.undertow;

import io.undertow.io.BlockingSenderImpl;
import io.undertow.io.Sender;
import io.undertow.io.UndertowInputStream;
import io.undertow.io.UndertowOutputStream;
import io.undertow.server.BlockingHttpExchange;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.core.BlockingWriterSenderImpl;
import io.undertow.util.AttachmentKey;
import org.osgl.http.H;
import org.osgl.oms.app.AppContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OmsBlockingExchange implements BlockingHttpExchange {

    public static final AttachmentKey<AppContext> KEY_APP_CTX = AttachmentKey.create(AppContext.class);

    private InputStream inputStream;
    private OutputStream outputStream;
    private final HttpServerExchange exchange;

    public OmsBlockingExchange(final HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public InputStream getInputStream() {
        if (inputStream == null) {
            inputStream = new UndertowInputStream(exchange);
        }
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new UndertowOutputStream(exchange);
        }
        return outputStream;
    }

    @Override
    public Sender getSender() {
        H.Response response = ctx().resp();
        if (response.writerCreated()) {
            return new BlockingWriterSenderImpl(exchange, response.printWriter(), response.characterEncoding());
        } else {
            return new BlockingSenderImpl(exchange, response.outputStream());
        }
    }

    @Override
    public void close() throws IOException {
        AppContext ctx = ctx();
        if (!exchange.isComplete()) {
            try {
                UndertowRequest req = (UndertowRequest) ctx.req();
                req.closeAndDrainRequest();
            } finally {
                UndertowResponse resp = (UndertowResponse) ctx.resp();
                resp.closeStreamAndWriter();
            }
        } else {
            try {
                UndertowRequest req = (UndertowRequest) ctx.req();
                req.freeResources();
            } finally {
                UndertowResponse resp = (UndertowResponse) ctx.resp();
                resp.freeResources();
            }
        }
    }

    private AppContext ctx() {
        return exchange.getAttachment(KEY_APP_CTX);
    }
}
