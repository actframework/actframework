package act;

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

import org.osgl.exception.UnexpectedIOException;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.Output;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Locale;

public class MockResponse extends ActResponse<MockResponse> {

    private String contentType;
    private String encoding = "utf-8";
    private Locale locale = Locale.getDefault();
    private Writer writer;
    public int status = -1;
    private OutputStream os;

    private long len;

    @Override
    protected Class<MockResponse> _impl() {
        return MockResponse.class;
    }

    @Override
    public OutputStream createOutputStream() throws IllegalStateException, UnexpectedIOException {
        E.illegalStateIf(null != writer);
        if (null == os) {
            os = new ByteArrayOutputStream();
        }
        return os;
    }

    @Override
    protected Output createOutput() {
        throw E.tbd();
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
    public MockResponse contentLength(long len) {
        this.len = len;
        return this;
    }

    @Override
    public MockResponse writeContent(ByteBuffer byteBuffer) {
        return this;
    }

    @Override
    protected void doCommit() {

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
    protected void _setStatusCode(int sc) {
        this.status = sc;
    }

    @Override
    public MockResponse addHeader(String name, String value) {
        return null;
    }
}
