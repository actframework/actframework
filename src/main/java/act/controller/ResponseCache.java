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

import org.osgl.$;
import org.osgl.exception.UnexpectedIOException;
import org.osgl.http.H;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.IO;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ResponseCache extends H.Response implements Serializable {

    private Map<String, H.Cookie> cookies = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private String contentType;
    private Locale locale;
    private H.Status status;

    private String content;
    private byte[] binary;
    private ByteArrayOutputStream outputStream;
    private StringWriter writer;

    private transient OutputStream _os;
    private transient Writer _w;

    private H.Response realResponse;

    public ResponseCache(H.Response realResponse) {
        this.realResponse = $.notNull(realResponse);
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
        if (null == _os) {
            outputStream = new ByteArrayOutputStream();
            _os = new TeeOutputStream(realResponse.outputStream(), outputStream);
        }
        return _os;
    }

    @Override
    public Writer writer() throws IllegalStateException, UnexpectedIOException {
        if (null == _w) {
            writer = new StringWriter();
            _w = new TeeWriter(realResponse.writer(), writer);
        }
        return _w;
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
        this.binary = binary.asByteArray();
        IO.copy(SObject.of(this.binary).asInputStream(), realResponse.outputStream(), false);
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
        contentType(H.Format.TXT.contentType());
        writeContent(content);
        return this;
    }

    @Override
    public H.Response writeHtml(String content) {
        contentType(H.Format.HTML.contentType());
        writeContent(content);
        return this;
    }

    @Override
    public H.Response writeJSON(String content) {
        contentType(H.Format.JSON.contentType());
        writeContent(content);
        return this;
    }

    @Override
    protected H.Response me() {
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
        return realResponse.characterEncoding();
    }

    @Override
    public H.Response characterEncoding(String encoding) {
        header(H.Header.Names.ACCEPT_CHARSET, encoding);
        return this;
    }

    @Override
    public H.Response contentLength(long len) {
        realResponse.contentLength(len);
        return this;
    }

    @Override
    protected void _setContentType(String type) {
    }

    @Override
    protected void _setLocale(Locale loc) {
    }

    @Override
    public Locale locale() {
        return realResponse.locale();
    }

    @Override
    public void addCookie(H.Cookie cookie) {
        realResponse.addCookie(cookie);
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
        return null;
    }

    @Override
    public H.Response addHeader(String name, String value) {
        return null;
    }

    @Override
    public H.Response writeContent(ByteBuffer buffer) {
        return null;
    }

    @Override
    public void commit() {

    }

    private static class TeeWriter extends Writer {
        private final Writer writer;
        private final Writer tee;

        TeeWriter(Writer writer, Writer tee) {
            this.writer = writer;
            this.tee = tee;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            writer.write(cbuf, off, len);
            tee.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
            tee.flush();
        }

        @Override
        public void close() throws IOException {
            writer.close();
            tee.close();
        }
    }


    private static class TeeOutputStream extends OutputStream {

        private final OutputStream out;
        private final OutputStream tee;

        TeeOutputStream(OutputStream out, OutputStream tee) {
            if (out == null)
                throw new NullPointerException();
            else if (tee == null)
                throw new NullPointerException();

            this.out = out;
            this.tee = tee;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            tee.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            out.write(b);
            tee.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            tee.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
            tee.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
            tee.close();
        }
    }
}
