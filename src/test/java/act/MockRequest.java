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

import org.osgl.http.H;

import java.io.InputStream;

public class MockRequest extends H.Request<MockRequest> {
    @Override
    protected Class<MockRequest> _impl() {
        return MockRequest.class;
    }

    @Override
    public H.Method method() {
        return null;
    }

    @Override
    public MockRequest method(H.Method method) {
        return this;
    }

    @Override
    public String header(String name) {
        return null;
    }

    @Override
    public Iterable<String> headers(String name) {
        return null;
    }

    @Override
    public Iterable<String> headerNames() {
        return null;
    }

    @Override
    public H.Format accept() {
        return super.accept();
    }

    @Override
    public boolean isAjax() {
        return super.isAjax();
    }

    @Override
    public String path() {
        return null;
    }

    @Override
    public String contextPath() {
        return null;
    }

    @Override
    public String query() {
        return null;
    }

    @Override
    public boolean secure() {
        return false;
    }

    @Override
    protected String _ip() {
        return null;
    }

    @Override
    protected void _initCookieMap() {

    }



    @Override
    public InputStream createInputStream() throws IllegalStateException {
        return null;
    }

    @Override
    public String paramVal(String name) {
        return null;
    }

    @Override
    public String[] paramVals(String name) {
        return new String[0];
    }

    @Override
    public Iterable<String> paramNames() {
        return null;
    }
}
