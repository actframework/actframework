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

import act.ResponseImplBase;
import act.app.ActionContext;
import act.conf.AppConfig;
import io.undertow.io.IoCallback;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.osgl.exception.UnexpectedIOException;
import org.osgl.http.H;
import org.osgl.storage.ISObject;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;

public class UndertowResponse extends ResponseImplBase<UndertowResponse> {
    @Override
    protected Class<UndertowResponse> _impl() {
        return UndertowResponse.class;
    }

    private HttpServerExchange hse;


    public UndertowResponse(HttpServerExchange exchange, AppConfig config) {
        super(config);
        E.NPE(exchange);
        hse = exchange;
        header(H.Header.Names.SERVER, config.serverHeader());
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
    public UndertowResponse contentLength(long len) {
        hse.setResponseContentLength(len);
        return this;
    }

    @Override
    public UndertowResponse writeContent(String s) {
        hse.getResponseSender().send(s);
        return this;
    }

    @Override
    public UndertowResponse writeContent(ByteBuffer byteBuffer) {
        hse.getResponseSender().send(byteBuffer);
        return this;
    }

    @Override
    public UndertowResponse writeBinary(ISObject binary) {
        File file = tryGetFileFrom(binary);
        if (null == file) {
            byte[] ba = binary.asByteArray();
            ByteBuffer buffer = ByteBuffer.wrap(ba);
            hse.getResponseSender().send(buffer);
        } else {
            try {
                hse.getResponseSender().transferFrom(FileChannel.open(file.toPath()), IoCallback.END_EXCHANGE);
            } catch (IOException e) {
                throw E.ioException(e);
            }
        }
        return this;
    }

    private File tryGetFileFrom(ISObject sobj) {
        String className = sobj.getClass().getSimpleName();
        if (className.contains("FileSObject")) {
            return sobj.asFile();
        }
        return null;
    }

    @Override
    protected OutputStream createOutputStream() {
        ensureBlocking();
        return hse.getOutputStream();
    }

    @Override
    public OutputStream outputStream() throws IllegalStateException, UnexpectedIOException {
        return super.outputStream();
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
        hse.setStatusCode(sc);
        return this;
    }

    @Override
    public UndertowResponse addHeader(String name, String value) {
        HeaderMap map = hse.getResponseHeaders();
        map.add(HttpString.tryFromString(name), value);
        return this;
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

    private void ensureBlocking() {
        if (!hse.isBlocking()) {
            hse.startBlocking(new ActBlockingExchange(hse, ActionContext.current()));
        }
    }


    private boolean responseStarted() {
        return hse.isResponseStarted();
    }

}
