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

import act.app.ActionContext;
import act.conf.AppConfig;
import act.handler.RequestHandler;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.S;

public abstract class RequestImplBase<T extends H.Request> extends H.Request<T> {
    private AppConfig cfg;
    private H.Method method;
    private Boolean secure;

    protected RequestImplBase(AppConfig config) {
        E.NPE(config);
        cfg = config;
    }

    private H.Method _method() {
        return H.Method.valueOfIgnoreCase(methodName());
    }

    protected abstract String methodName();

    @Override
    public String fullPath() {
        return path();
    }

    @Override
    public String contextPath() {
        return "";
    }

    @Override
    public T method(H.Method method) {
        this.method = $.NPE(method);
        return me();
    }

    @Override
    public H.Method method() {
        if (null == method) {
            method = _method();
            if (method == H.Method.POST) {
                // check the method overload
                String s = header(H.Header.Names.X_HTTP_METHOD_OVERRIDE);
                if (S.blank(s)) {
                    s = paramVal("_method"); // Spring convention
                }
                if (S.notBlank(s)) {
                    method = H.Method.valueOfIgnoreCase(s);
                }
            }
        }
        return method;
    }

    @Override
    public boolean secure() {
        if (null == secure) {
            if ("https".equals(cfg.xForwardedProtocol())) {
                secure = true;
            } else {
                secure = parseSecureXHeaders();
            }
        }
        return secure;
    }

    public abstract void receiveFullBytesAndProceed(final ActionContext context, final RequestHandler handler);

    private boolean parseSecureXHeaders() {
        String s = header(H.Header.Names.X_FORWARDED_PROTO);
        if ("https".equals(s)) {
            return true;
        }
        s = header(H.Header.Names.X_FORWARDED_SSL);
        if ("on".equals(s)) {
            return true;
        }
        s = header(H.Header.Names.FRONT_END_HTTPS);
        if ("on".equals(s)) {
            return true;
        }
        s = header(H.Header.Names.X_URL_SCHEME);
        if ("https".equals(s)) {
            return true;
        }
        return false;
    }
}
