package act.controller;

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
import org.osgl.$;
import org.osgl.exception.UnexpectedIOException;
import org.osgl.http.H;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.osgl.http.H.Format.*;

public class ResponseCache extends ResponseImplBase implements Serializable {

    private Map<String, H.Cookie> cookies = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private Long len;
    private H.Status status;

    private String content;
    private byte[] binary;

    private transient H.Response realResponse;

    public ResponseCache() {}

    public ResponseCache(H.Response realResponse) {
        this.realResponse = $.notNull(realResponse);
    }

    public void applyTo(ResponseImplBase response) {
        for (H.Cookie cookie : cookies.values()) {
            response.addCookie(cookie);
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            response.header(entry.getKey(), entry.getValue());
        }
        if (null != contentType) {
            response.contentType(contentType);
        }
        if (null != charset) {
            response.characterEncoding(charset);
        }
        response.commitContentType();
        if (null != len) {
            response.contentLength(len);
        }
        if (null != locale) {
            response.locale(locale);
        }
        if (null != status) {
            response.status(status);
        }
        if (null != content) {
            response.writeContent(content);
        } else if (null != binary) {
            response.writeBinary(SObject.of(binary));
        }
    }

    @Override
    public H.Response context(Object context) {
        realResponse.context(context);
        return this;
    }

    @Override
    public Object context() {
        return realResponse.context();
    }

    @Override
    public boolean writerCreated() {
        return realResponse.writerCreated();
    }

    @Override
    public OutputStream outputStream() throws IllegalStateException, UnexpectedIOException {
        return realResponse.outputStream();
    }

    @Override
    public Writer writer() throws IllegalStateException, UnexpectedIOException {
        return realResponse.writer();
    }

    @Override
    public PrintWriter printWriter() {
        Writer w = writer();
        if (w instanceof PrintWriter) {
            return (PrintWriter) w;
        } else {
            return new PrintWriter(w);
        }
    }

    @Override
    public H.Response contentType(String type) {
        realResponse.contentType(type);
        contentType = type;
        return this;
    }

    @Override
    public H.Response initContentType(String type) {
        realResponse.initContentType(type);
        if (null == contentType) {
            contentType = type;
        }
        return this;
    }

    @Override
    public H.Response contentDisposition(String filename, boolean inline) {
        realResponse.contentDisposition(filename, inline);
        // set through header call
        return this;
    }

    @Override
    public H.Response prepareDownload(String filename) {
        realResponse.prepareDownload(filename);
        // set through header call
        return this;
    }

    @Override
    public H.Response etag(String etag) {
        realResponse.etag(etag);
        // set through header call
        return this;
    }

    @Override
    public H.Response locale(Locale locale) {
        realResponse.locale(locale);
        this.locale = locale;
        return this;
    }

    @Override
    public H.Response sendError(int sc, String msg, Object... args) {
        realResponse.sendError(sc, msg, args);
        return this;
    }

    @Override
    public H.Response status(H.Status s) {
        realResponse.status(s);
        this.status = s;
        return this;
    }

    @Override
    public H.Response addHeaderIfNotAdded(String name, String value) {
        realResponse.addHeaderIfNotAdded(name, value);
        if (!headers.containsKey(name)) {
            headers.put(name, value);
        }
        return this;
    }

    @Override
    public H.Response writeBinary(ISObject binary) {
        realResponse.writeBinary(binary);
        this.binary = binary.asByteArray();
        return this;
    }

    @Override
    public H.Response writeContent(String s) {
        realResponse.writeContent(s);
        this.content = s;
        return this;
    }

    @Override
    public H.Response writeText(String content) {
        return writeContent(content, TXT);
    }

    @Override
    public H.Response writeHtml(String content) {
        return writeContent(content, HTML);
    }

    @Override
    public H.Response writeJSON(String content) {
        return writeContent(content, JSON);
    }

    private H.Response writeContent(String content, H.Format contentType) {
        contentType(contentType.contentType());
        writeContent(content);
        return this;
    }

    @Override
    protected Class _impl() {
        return getClass();
    }

    @Override
    protected OutputStream createOutputStream() {
        return null;
    }

    @Override
    public String characterEncoding() {
        return charset;
    }

    @Override
    public ResponseImplBase characterEncoding(String encoding) {
        realResponse.characterEncoding(encoding);
        super.characterEncoding(encoding);
        return this;
    }

    @Override
    public H.Response contentLength(long len) {
        realResponse.contentLength(len);
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
        return realResponse.locale();
    }

    @Override
    public void addCookie(H.Cookie cookie) {
        realResponse.addCookie(cookie);
        cookies.put(cookie.name(), cookie);
    }

    @Override
    public boolean containsHeader(String name) {
        return realResponse.containsHeader(name);
    }

    @Override
    public H.Response sendError(int sc, String msg) {
        realResponse.sendError(sc, msg);
        return this;
    }

    @Override
    public H.Response sendError(int sc) {
        realResponse.sendError(sc);
        return this;
    }

    @Override
    public H.Response sendRedirect(String location) {
        realResponse.sendRedirect(location);
        return this;
    }

    @Override
    public H.Response header(String name, String value) {
        realResponse.header(name, value);
        return this;
    }

    @Override
    public H.Response status(int sc) {
        realResponse.status(sc);
        this.status = H.Status.of(sc);
        return this;
    }

    @Override
    public H.Response addHeader(String name, String value) {
        realResponse.addHeader(name, value);
        headers.put(name, value);
        return this;
    }

    @Override
    public H.Response writeContent(ByteBuffer buffer) {
        realResponse.writeContent(buffer);
        // we don't cache the byte buffer
        return this;
    }

    @Override
    public void commit() {
        realResponse.commit();
    }

}
