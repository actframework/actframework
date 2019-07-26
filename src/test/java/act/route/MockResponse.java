package act.route;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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
import org.osgl.http.H;
import org.osgl.util.Output;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Locale;

public class MockResponse extends ActResponse<MockResponse> {
    @Override
    protected Class<MockResponse> _impl() {
        return MockResponse.class;
    }

    @Override
    protected OutputStream createOutputStream() {
        return null;
    }

    @Override
    protected Output createOutput() {
        return null;
    }

    @Override
    public MockResponse contentLength(long len) {
        return null;
    }

    @Override
    protected void _setLocale(Locale loc) {

    }

    @Override
    public Locale locale() {
        return null;
    }

    @Override
    public void addCookie(H.Cookie cookie) {

    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public MockResponse header(String name, String value) {
        return null;
    }

    @Override
    protected void _setStatusCode(int sc) {
    }

    @Override
    public MockResponse addHeader(String name, String value) {
        return null;
    }

    @Override
    public MockResponse writeContent(ByteBuffer buffer) {
        return null;
    }

    @Override
    protected void doCommit() {

    }

}
