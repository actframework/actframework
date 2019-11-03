package act.xio.undertow;

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

import act.ActResponse;
import act.app.ActionContext;
import act.conf.AppConfig;
import io.undertow.io.DefaultIoCallback;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.util.*;
import org.osgl.$;
import org.osgl.exception.UnexpectedIOException;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.storage.ISObject;
import org.osgl.util.*;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public class UndertowResponse extends ActResponse<UndertowResponse> {

    protected static Logger LOGGER = LogManager.get(UndertowResponse.class);

    private static final HttpString _SERVER = new HttpString(H.Header.Names.SERVER);
    private static final HttpStringCache HEADER_NAMES = HttpStringCache.HEADER;

    private HttpServerExchange hse;

    private boolean endAsync;
    private Sender sender;

    public UndertowResponse(HttpServerExchange exchange, AppConfig config) {
        super(config);
        hse = $.requireNotNull(exchange);
        hse.getResponseHeaders().put(_SERVER, config.serverHeader());
    }

    @Override
    protected Output createOutput() {
        return isIoThread() ? new NonBlockOutput(sender()) : new OutputStreamOutput(createOutputStream());
    }

    @Override
    public void addCookie(H.Cookie cookie) {
        hse.setResponseCookie(CookieConverter.osgl2undertow(cookie));
    }

    @Override
    public void removeCookie(String name) {

    }

    @Override
    public boolean containsHeader(String name) {
        return hse.getResponseHeaders().contains(HEADER_NAMES.get(name));
    }

    @Override
    public UndertowResponse contentLength(long len) {
        hse.setResponseContentLength(len);
        return this;
    }

    Sender sender() {
        if (null == sender) {
            sender = hse.getResponseSender();
            endAsync = !blocking();
        }
        return sender;
    }

    @Override
    public UndertowResponse writeContent(String s) {
        beforeWritingContent();
        if (s.length() == 0) {
            afterWritingContent();
        } else {
            try {
                sender().send(s);
                endAsync = !blocking();
                afterWritingContent();
            } catch (RuntimeException e) {
                endAsync = false;
                afterWritingContent();
                throw e;
            }
        }
        return this;
    }

    @Override
    public UndertowResponse writeContent(ByteBuffer byteBuffer) {
        beforeWritingContent();
        try {
            endAsync = !blocking();
            Sender sender = sender();
            if (endAsync) {
                sender.send(byteBuffer, IoCallback.END_EXCHANGE);
            } else {
                sender.send(byteBuffer);
            }
            afterWritingContent();
        } catch (RuntimeException e) {
            endAsync = false;
            afterWritingContent();
            throw e;
        }
        return this;
    }

    @Override
    public UndertowResponse writeBinary(ISObject binary) {
        beforeWritingContent();
        File file = tryGetFileFrom(binary);
        if (null != file) {
            return send(file);
        }
        byte[] ba = binary.asByteArray();
        ByteBuffer buffer = ByteBuffer.wrap(ba);
        sender().send(buffer);
        endAsync = !blocking();
        afterWritingContent();
        return this;
    }

    @Override
    public UndertowResponse send(URL url) {
        Resource resource = new URLResource(url, "");
        resource.serve(sender(), hse, new DefaultIoCallback() {
            @Override
            public void onComplete(HttpServerExchange exchange, Sender sender) {
                super.onComplete(exchange, sender);
                afterWritingContent();
                if (!blocking()) {
                    ActionContext context = context();
                    context.destroy();
                }
            }

        });
        return me();
    }

    @Override
    public UndertowResponse send(File file) {
        try {
            final FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
            hse.setResponseContentLength(channel.size());
            sender().transferFrom(channel, new IoCallback() {
                @Override
                public void onComplete(HttpServerExchange exchange, Sender sender) {
                    IO.close(channel);
                    IoCallback.END_EXCHANGE.onComplete(exchange, sender);
                    afterWritingContent();
                    if (!blocking()) {
                        ActionContext context = context();
                        context.destroy();
                    }
                }

                @Override
                public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
                    IO.close(channel);
                    IoCallback.END_EXCHANGE.onException(exchange, sender, exception);
                }
            });
            endAsync = !blocking();
        } catch (IOException e) {
            endAsync = false;
            afterWritingContent();
            throw E.ioException(e);
        }
        return me();
    }

    @Override
    public OutputStream outputStream() throws IllegalStateException, UnexpectedIOException {
        return super.outputStream();
    }

    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    protected void doCommit() {
        if (null != this.output) {
            IO.close(output);
        } else if (null != this.outputStream) {
            IO.close(outputStream);
        } else if (null != this.writer) {
            IO.close(writer);
        }
        if (!endAsync) {
            hse.endExchange();
        }
        markClosed();
    }

    @Override
    public UndertowResponse header(String name, String value) {
        hse.getResponseHeaders().put(HEADER_NAMES.get(name), value);
        return this;
    }

    @Override
    public void _setStatusCode(int sc) {
        hse.setStatusCode(sc);
    }

    @Override
    public UndertowResponse addHeader(String name, String value) {
        HeaderMap map = hse.getResponseHeaders();
        map.add(HEADER_NAMES.get(name), value);
        return this;
    }

    public void freeResources() {
        if (writer != null) {
            IO.close(writer);
        } else if (outputStream != null) {
            IO.close(outputStream);
        }
    }

    private void ensureBlocking() {
        if (!blocking()) {
            hse.startBlocking(new ActBlockingExchange(hse, ActionContext.current()));
        }
    }

    private File tryGetFileFrom(ISObject sobj) {
        String className = sobj.getClass().getSimpleName();
        if (className.contains("FileSObject")) {
            return sobj.asFile();
        }
        return null;
    }

    @Override
    protected Writer createWriter() {
        // TODO #698 - understand why the following line cause performance issue
        //return blocking() ? super.createWriter() : new UndertowResponseOutput(this);
        return super.createWriter();
    }

    @Override
    protected OutputStream createOutputStream() {
        if (blocking()) {
            return hse.getOutputStream();
        } else {
            endAsync = true;
            final NonBlockOutput senderOutput = new NonBlockOutput(sender());
            return BufferedOutput.wrap(senderOutput).asOutputStream();
        }
    }

    @Override
    protected void _setLocale(Locale loc) {
        if (responseStarted()) {
            return;
        }
        locale = loc;
        hse.getResponseHeaders().put(Headers.CONTENT_LANGUAGE, loc.getLanguage() + "-" + loc.getCountry());
    }

    @Override
    protected Class<UndertowResponse> _impl() {
        return UndertowResponse.class;
    }

    private boolean responseStarted() {
        return hse.isResponseStarted();
    }

    private boolean blocking() {
        return hse.isBlocking();
    }

    private boolean isIoThread() {
        return hse.isInIoThread();
    }

}
