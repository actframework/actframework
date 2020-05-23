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
import act.handler.RequestHandler;
import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HttpString;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.util.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

public class UndertowRequest extends RequestImplBase<UndertowRequest> {

    private static final HttpStringCache HEADER_NAMES = HttpStringCache.HEADER;
    private static final Set<HttpString> PROTECTED_HEADER_NAMES = C.set(
            HEADER_NAMES.get("Authorization"),
            HEADER_NAMES.get("User-Agent"),
            HEADER_NAMES.get("Referer"),
            HEADER_NAMES.get("Cookie"),
            HEADER_NAMES.get("Host"),
            HEADER_NAMES.get("Proxy-Authorization"),
            HEADER_NAMES.get("X-Forwarded-For"),
            HEADER_NAMES.get("X-Forwarded-Host"),
            HEADER_NAMES.get("X-Forwarded-Proto"),
            HEADER_NAMES.get("X-ATT-DeviceId"),
            HEADER_NAMES.get("X-Wap-Profile"),
            HEADER_NAMES.get("Proxy-Connection"),
            HEADER_NAMES.get("X-UIDH"),
            HEADER_NAMES.get("Host"),
            HEADER_NAMES.get("Proxy-Client-Ip"),
            HEADER_NAMES.get("Wl-Proxy-Client-Ip"),
            HEADER_NAMES.get("HTTP_CLIENT_IP"),
            HEADER_NAMES.get("HTTP_X_FORWARDED_FOR"),
            HEADER_NAMES.get("If-None-Match"),
            HEADER_NAMES.get("If-Match")
    );

    private String path;
    private boolean headerOverwrite;
    private HttpServerExchange hse;
    private byte[] body;
    private Map<String, Deque<String>> queryParams;
    private Map<Keyword, Deque<String>> queryParamsByKeyword;
    private boolean keywordMatching;
    private Map<HttpString, String> headerCache = new HashMap<>();
    private static final String NULL_HEADER_VAL = "_NULL_";

    public UndertowRequest(HttpServerExchange exchange, AppConfig config) {
        super(config);
        E.NPE(exchange);
        hse = exchange;
        headerOverwrite = config.allowHeaderOverwrite();
        keywordMatching = config.paramBindingKeywordMatching();
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
        HttpString key = HEADER_NAMES.get(name);
        String val = headerCache.get(key);
        if (null == val) {
            if (headerOverwrite && !PROTECTED_HEADER_NAMES.contains(key)) {
                val = paramVal(headerQueryKey(name));
            }
            if (null == val) {
                val = hse.getRequestHeaders().get(key, 0);
            }
            headerCache.put(key, null == val ? NULL_HEADER_VAL : val);
        }
        return NULL_HEADER_VAL == val ? null : val;
    }

    private String headerQueryKey(String header) {
        return S.concat("act_header_", S.underscore(header.toLowerCase()));
    }

    @Override
    public Iterable<String> headers(String name) {
        if (!headerOverwrite) {
            return hse.getRequestHeaders().eachValue(HEADER_NAMES.get(name));
        }
        Iterable<String> vals = C.listOf(paramVals(headerQueryKey(name)));
        if (((List) vals).isEmpty()) {
            vals = hse.getRequestHeaders().eachValue(HEADER_NAMES.get(name));
        }
        return vals;
    }

    @Override
    public Iterable<String> headerNames() {
        return C.seq(hse.getRequestHeaders().getHeaderNames()).map($.F.<HttpString>asString());
    }

    @Override
    public InputStream createInputStream() throws IllegalStateException {
        if (null != body) {
            return new ByteArrayInputStream(body);
        }
        if (!hse.isBlocking()) {
            hse.startBlocking(new ActBlockingExchange(hse, ActionContext.current()));
        }
        return hse.getInputStream();
    }

    public void receiveFullBytesAndProceed(final ActionContext context, final RequestHandler handler) {
        ActionContext.clearLocal();
        hse.getRequestReceiver().receiveFullBytes(new Receiver.FullBytesCallback() {
            @Override
            public void handle(HttpServerExchange exchange, byte[] message) {
                body = message;
                context.saveLocal();
                handler.handle(context);
            }
        });
    }

    @Override
    public String paramVal(String name) {
        Deque<String> dq = queryParamVals(name);
        return null == dq ? null : dq.peekFirst();
    }

    @Override
    public String[] paramVals(String name) {
        Deque<String> deque = queryParamVals(name);
        if (null == deque) {
            return null;
        }
        String[] sa = new String[deque.size()];
        sa = deque.toArray(sa);
        return sa;
    }

    @Override
    public Iterable<String> paramNames() {
        return queryParams().keySet();
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

    private Map<String, Deque<String>> queryParams() {
        if (null == queryParams) {
            queryParams = hse.getQueryParameters();
            if (keywordMatching) {
                queryParamsByKeyword = new HashMap<>();
                for (Map.Entry<String, Deque<String>> entry : queryParams.entrySet()) {
                    queryParamsByKeyword.put(Keyword.of(entry.getKey()), entry.getValue());
                }
            }
        }
        return queryParams;
    }

    private Deque<String> queryParamVals(String name) {
        queryParams();
        return keywordMatching ? queryParamsByKeyword.get(Keyword.of(name)) : queryParams.get(name);
    }

}
