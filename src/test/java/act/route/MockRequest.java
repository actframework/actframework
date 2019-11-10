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

import act.RequestImplBase;
import act.app.ActionContext;
import act.conf.AppConfig;
import act.handler.RequestHandler;
import org.osgl.http.H;

import java.io.InputStream;

public class MockRequest extends RequestImplBase<MockRequest> {
    private H.Method method;
    private String url;
    public MockRequest(AppConfig config, H.Method method, String url) {
        super(config);
        this.method = method;
        this.url = url;
    }

    @Override
    protected String methodName() {
        return method.name();
    }

    @Override
    protected Class<MockRequest> _impl() {
        return MockRequest.class;
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
    public String path() {
        return url;
    }

    @Override
    public String query() {
        return null;
    }

    @Override
    protected String _ip() {
        return null;
    }

    @Override
    protected void _initCookieMap() {

    }

    @Override
    public void receiveFullBytesAndProceed(ActionContext context, RequestHandler handler) {
        //handler.handle(context);
    }

    @Override
    protected InputStream createInputStream() {
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
