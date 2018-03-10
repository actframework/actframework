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

import act.RequestImplBase;
import act.app.ActionContext;
import act.conf.AppConfig;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import org.osgl.util.Codec;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.Map;

public class UndertowRequest extends RequestImplBase<UndertowRequest> {

    private static final HttpStringCache HEADER_NAMES = HttpStringCache.HEADER;

    private String path;
    private HttpServerExchange hse;
    private Map<String, Deque<String>> queryParams;

    public UndertowRequest(HttpServerExchange exchange, AppConfig config) {
        super(config);
        E.NPE(exchange);
        hse = exchange;
    }

    @Override
    public String path() {
        // cannot use path as it cut the URI like
        // "/gh/325/data/foo=3;bar=6/key/bar"
        // to "/gh/325/data/foo=3"
        // and put "bar=6/key/bar" as path parameters
        //return hse.getRequestPath();
        if (null == path) {
            path = hse.getPathParameters().isEmpty() ? hse.getRelativePath() : Codec.decodeUrl(hse.getRequestURI());
        }
        return path;
    }

    public void forward(final String path) {
        this.path = path;
    }

    @Override
    public String query() {
        return hse.getQueryString();
    }

    @Override
    protected String methodName() {
        return hse.getRequestMethod().toString();
    }

    @Override
    public String header(String name) {
        return hse.getRequestHeaders().get(HEADER_NAMES.get(name), 0);
    }

    @Override
    public Iterable<String> headers(String name) {
        return hse.getRequestHeaders().eachValue(HEADER_NAMES.get(name));
    }

    @Override
    public InputStream createInputStream() throws IllegalStateException {
        if (!hse.isBlocking()) {
            hse.startBlocking(new ActBlockingExchange(hse, ActionContext.current()));
        }
        return hse.getInputStream();
    }

    @Override
    public String paramVal(String name) {
        if (null == queryParams) {
            queryParams = hse.getQueryParameters();
        }
        Deque<String> dq = queryParams.get(name);
        return null == dq ? null : dq.peekFirst();
    }

    @Override
    public String[] paramVals(String name) {
        if (null == queryParams) {
            queryParams = hse.getQueryParameters();
        }
        Deque<String> deque = queryParams.get(name);
        if (null == deque) {
            return null;
        }
        String[] sa = new String[deque.size()];
        sa = deque.toArray(sa);
        return sa;
    }

    @Override
    public Iterable<String> paramNames() {
        if (null == queryParams) {
            queryParams = hse.getQueryParameters();
        }
        return queryParams.keySet();
    }

    public void closeAndDrainRequest() {
        if (null != reader) {
            IO.close(reader);
        } else {
            IO.close(inputStream());
        }
    }

    public void freeResources() {
        if (reader != null) {
            IO.close(reader);
        } else if (inputStream != null) {
            IO.close(inputStream);
        }
    }

    @Override
    protected String _ip() {
        InetSocketAddress sourceAddress = hse.getSourceAddress();
        if (sourceAddress == null) {
            return "";
        }
        InetAddress address = sourceAddress.getAddress();
        if (address == null) {
            //this is unresolved, so we just return the host className
            //not exactly spec, but if the className should be resolved then a PeerNameResolvingHandler should be used
            //and this is probably better than just returning null
            return sourceAddress.getHostString();
        }
        return address.getHostAddress();
    }

    @Override
    protected void _initCookieMap() {
        Map<String, Cookie> cookies = hse.getRequestCookies();
        if (cookies.isEmpty()) {
            return;
        }
        for (Map.Entry<String, io.undertow.server.handlers.Cookie> entry : cookies.entrySet()) {
            io.undertow.server.handlers.Cookie cookie = entry.getValue();
            try {
                _setCookie(cookie.getName(), CookieConverter.undertow2osgl(cookie));
            } catch (IllegalArgumentException e) {
                // Ignore bad cookie
            }
        }
    }

    @Override
    protected Class<UndertowRequest> _impl() {
        return UndertowRequest.class;
    }

    HttpServerExchange exchange() {
        return hse;
    }


}
