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
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.osgl.$;
import org.osgl.exception.UnexpectedIOException;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.storage.ISObject;
import org.osgl.util.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class UndertowResponse extends ActResponse<UndertowResponse> {

    protected static Logger LOGGER = LogManager.get(UndertowResponse.class);

    private static class Buffer {
        boolean isSending;
        ByteBuffer buffer;
        IoCallback callback;
        ReentrantLock lock;
        Condition finalPartToGo;

        Buffer(IoCallback callback, ReentrantLock lock) {
            this.callback = $.requireNotNull(callback);
            this.isSending = true;
            this.lock = lock;
        }

        void sendOrBuf(String content, Sender sender) {
            sendOrBuf(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)), sender);
        }

        void sendOrBuf(ByteBuffer content, Sender sender) {
            lock.lock();
            try {
                if (null == buffer) {
                    buffer = content;
                } else {
                    ByteBuffer merged = ByteBuffer.allocate(buffer.limit() + content.limit());
                    merged.put(buffer).put(content).flip();
                    buffer = merged;
                }
                if (!isSending) {
                    isSending = true;
                    ByteBuffer buffer = this.buffer;
                    this.buffer = null;
                    sender.send(buffer, callback);
                }
            } finally {
                lock.unlock();
            }
        }

        void sendThroughFinalPart(Sender sender) {
            if (null == this.buffer) {
                return;
            }
            lock.lock();
            try {
                if (!isSending) {
                    isSending = true;
                    sender.send(this.buffer);
                } else {
                    finalPartToGo = lock.newCondition();
                    while (isSending) {
                        try {
                            finalPartToGo.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw E.unexpected(e);
                        }
                    }
                    ByteBuffer buffer = this.buffer;
                    this.buffer = null;
                    sender.send(buffer, callback);
                }
            } finally {
                lock.unlock();
            }
        }

        void partSent() {
            isSending = false;
            if (null != finalPartToGo) {
                finalPartToGo.signal();
            }
        }

        private void clear() {
            isSending = false;
            buffer = null;
        }
    }

    private static final HttpString _SERVER = new HttpString(H.Header.Names.SERVER);
    private static final HttpStringCache HEADER_NAMES = HttpStringCache.HEADER;

    private HttpServerExchange hse;

    private boolean endAsync;
    private Sender sender;
    private ReentrantLock lock;
    private boolean isPartialMode;
    private AtomicBoolean partialSent = new AtomicBoolean();
    private AtomicBoolean closeExchange = new AtomicBoolean(false);
    private IoCallback ioCallback = new DefaultIoCallback() {
        @Override
        public void onComplete(HttpServerExchange exchange, Sender sender) {
            if (null != lock) {
                lock.lock();
                buffer.partSent();
                lock.unlock();
            } else if (closeExchange.get()) {
                exchange.endExchange();
            } else {
                partialSent.set(true);
            }
        }

        @Override
        public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
            if (null != buffer) {
                buffer.clear();
                buffer = null;
            }
            super.onException(exchange, sender, exception);
        }
    };
    private Buffer buffer;

    public UndertowResponse(HttpServerExchange exchange, AppConfig config) {
        super(config);
        hse = $.requireNotNull(exchange);
        hse.getResponseHeaders().put(_SERVER, config.serverHeader());
    }

    @Override
    protected Output createOutput() {
        return BufferedOutput.wrap(blocking() ? new OutputStreamOutput(createOutputStream()) : new UndertowResponseOutput(this));
    }

    @Override
    public void addCookie(H.Cookie cookie) {
        hse.setResponseCookie(CookieConverter.osgl2undertow(cookie));
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
        if ("" == s) {
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

    public void writeContentPart(String s) {
        ByteBuffer buffer = ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
        writeContentPart(buffer);
    }

    public void writeContentPart(ByteBuffer buffer) {
        isPartialMode = true;
        try {
            if (null != buffer) {
                partialSent.set(false);
                sender().send(buffer, ioCallback);
            } else {
                this.buffer.sendOrBuf(buffer, sender());
            }
        } catch (IllegalStateException e) {
            lock = new ReentrantLock();
            this.buffer = new Buffer(ioCallback, lock);
            this.buffer.sendOrBuf(buffer, sender());
        } catch (RuntimeException e) {
            endAsync = false;
            throw e;
        }
    }

    @Override
    public UndertowResponse writeContent(ByteBuffer byteBuffer) {
        beforeWritingContent();
        try {
            sender().send(byteBuffer);
            endAsync = !blocking();
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
        if (null == file) {
            byte[] ba = binary.asByteArray();
            ByteBuffer buffer = ByteBuffer.wrap(ba);
            sender().send(buffer);
            endAsync = !blocking();
            afterWritingContent();
        } else {
            try {
                sender().transferFrom(FileChannel.open(file.toPath()), IoCallback.END_EXCHANGE);
                endAsync = !blocking();
                afterWritingContent();
            } catch (IOException e) {
                endAsync = false;
                afterWritingContent();
                throw E.ioException(e);
            }
        }
        return this;
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
    public void commit() {
        if (null != this.output) {
            output.flush();
        } else if (null != this.outputStream) {
            IO.close(outputStream);
        } else if (null != this.writer) {
            IO.close(writer);
        }
        if (!endAsync) {
            hse.endExchange();
        } else {
            if (null != buffer) {
                buffer.sendThroughFinalPart(sender());
            } else if (isPartialMode) {
                if (partialSent.get()) {
                    hse.endExchange();
                } else {
                    closeExchange.set(true);
                }
            }
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
        return blocking() ? createBlockingOutputStream() : new UndertowResponseOutputStream(this);
    }

    private OutputStream createBlockingOutputStream() {
        ensureBlocking();
        return hse.getOutputStream();
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

}
