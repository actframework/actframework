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

import static org.osgl.http.H.Format.*;
import static org.osgl.http.H.Header.Names.ETAG;

import act.ActResponse;
import org.osgl.$;
import org.osgl.exception.UnexpectedIOException;
import org.osgl.http.H;
import org.osgl.storage.ISObject;
import org.osgl.util.Charsets;
import org.osgl.util.Output;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class ResponseCache extends ActResponse implements Serializable {

    private Map<String, H.Cookie> cookies = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();

    private Long len;
    private H.Status status;
    private String etag;

    private ByteBuffer buffer;
    private OutputStreamCache osCache;
    private WriterCache writerCache;
    private OutputCache outputCache;
    private boolean wroteDirectly;

    private transient ActResponse realResponse;

    public ResponseCache() {}

    public ResponseCache(ActResponse realResponse) {
        this.realResponse = $.requireNotNull(realResponse);
    }

    public String etag() {
        return this.etag;
    }

    public void applyTo(ActResponse response) {
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
        if (null != buffer) {
            response.writeContent(buffer.duplicate());
        } else if (null != osCache) {
            osCache.apply(response);
        } else if (null != writerCache) {
            writerCache.apply(response);
        } else if (null != outputCache) {
            outputCache.apply(response);
        }
    }

    public boolean isValid() {
        if (wroteDirectly) {
            return true;
        } else if (null != osCache) {
            return osCache.isCommitted();
        } else if (null != writerCache) {
            return writerCache.isCommitted();
        } else if (null != outputCache) {
            return outputCache.isCommitted();
        }
        return false;
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
        osCache = new OutputStreamCache(realResponse.outputStream());
        return osCache;
    }

    @Override
    public Output output() {
        outputCache = new OutputCache(realResponse.output());
        return outputCache;
    }

    @Override
    public Writer writer() throws IllegalStateException, UnexpectedIOException {
        writerCache = new WriterCache(realResponse.writer());
        return writerCache;
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
    public ActResponse contentType(H.Format fmt) {
        contentType = fmt.contentType();
        return super.contentType(fmt);
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
            if (ETAG.equalsIgnoreCase(name)) {
                this.etag = value;
            }
        }
        return this;
    }

    @Override
    public H.Response writeBinary(ISObject binary) {
        byte[] ba = binary.asByteArray();
        ByteBuffer buffer = ByteBuffer.allocateDirect(ba.length);
        buffer.put(ba);
        buffer.flip();
        this.buffer = buffer;
        realResponse.writeContent(buffer);
        this.wroteDirectly = true;
        return this;
    }

    @Override
    public H.Response writeContent(String s) {
        byte[] ba = s.getBytes(Charsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocateDirect(ba.length);
        buffer.put(ba);
        buffer.flip();
        this.buffer = buffer;
        realResponse.writeContent(s);
        this.wroteDirectly = true;
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
    protected Output createOutput() {
        return null;
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
    public ActResponse characterEncoding(String encoding) {
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
    public ActResponse sendError(int sc, String msg) {
        realResponse.sendError(sc, msg);
        return this;
    }

    @Override
    public ActResponse sendError(int sc) {
        realResponse.sendError(sc);
        return this;
    }

    @Override
    public ActResponse sendRedirect(String location) {
        realResponse.sendRedirect(location);
        return this;
    }

    @Override
    public H.Response header(String name, String value) {
        realResponse.header(name, value);
        headers.put(name, value);
        if (ETAG.equalsIgnoreCase(name)) {
            this.etag = value;
        }
        return this;
    }

    @Override
    protected void _setStatusCode(int sc) {
        realResponse.status(sc);
        this.status = H.Status.of(sc);
    }

    @Override
    public H.Response addHeader(String name, String value) {
        realResponse.addHeader(name, value);
        headers.put(name, value);
        if (ETAG.equalsIgnoreCase(name)) {
            this.etag = value;
        }
        return this;
    }

    @Override
    public H.Response writeContent(ByteBuffer buffer) {
        realResponse.writeContent(buffer);
        // we don't cache the byte buffer
        return this;
    }

    @Override
    protected void doCommit() {
        if (null != outputCache) {
            outputCache.commit();
        } else if (null != osCache) {
            osCache.commit();
        } else if (null != writerCache) {
            writerCache.commit();
        }
        realResponse.commit();
    }

}
