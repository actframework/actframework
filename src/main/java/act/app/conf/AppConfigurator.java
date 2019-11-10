package act.app.conf;

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

import act.app.event.SysEventId;
import act.conf.AppConfig;
import act.security.CSRFProtector;
import act.util.LogSupport;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.*;

import java.util.*;

/**
 * Base class for app developer implement source code based configuration
 */
public abstract class AppConfigurator<T extends AppConfigurator> extends AppConfig<T> {

    protected static final H.Method GET = H.Method.GET;
    protected static final H.Method POST = H.Method.POST;
    protected static final H.Method PUT = H.Method.PUT;
    protected static final H.Method DELETE = H.Method.DELETE;

    private transient Set<String> controllerClasses = new HashSet<>();
    private Map<String, Object> userProps = new HashMap<>();

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return obj.getClass() == getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    @Override
    protected void releaseResources() {
        controllerClasses.clear();
        userProps.clear();
        releaseAppConfigResources();
        super.releaseResources();
    }

    protected T registerStringValueResolver(Class<T> targetType, StringValueResolver<T> resolver) {
        app().resolverManager().register(targetType, resolver);
        return me();
    }

    protected CorsSetting cors() {
        return new CorsSetting(this);
    }

    protected CsrfSetting csrf() {
        return new CsrfSetting(this);
    }

    public void onRouteAdded(String controllerClassName) {
        controllerClasses.add(controllerClassName);
    }

    public Set<String> controllerClasses() {
        return C.newSet(controllerClasses);
    }

    protected T prop(String key, Object val) {
        userProps.put(key, val);
        return me();
    }

    public Set<String> propKeys() {
        return userProps.keySet();
    }

    public <V> V propVal(String key) {
        return $.cast(userProps.get(key));
    }

    /**
     * Sub class shall override this method to do the configuration
     */
    public abstract void configure();

    protected void releaseAppConfigResources() {}

    protected static class CsrfSetting extends LogSupport {
        private AppConfigurator conf;
        private boolean enabled;
        private String headerName;
        private String paramName;
        private String cookieName;
        private CSRFProtector protector;

        CsrfSetting(AppConfigurator conf) {
            this.conf = conf;
            this.enabled = true;
            conf.app().jobManager().on(SysEventId.CONFIG_PREMERGE, "CsrfSetting:checkAndCommit", new Runnable() {
                @Override
                public void run() {
                    checkAndCommit();
                }
            });
        }


        public CsrfSetting enable() {
            enabled = true;
            return this;
        }

        public CsrfSetting disable() {
            enabled = false;
            return this;
        }

        public CsrfSetting headerName(String name) {
            this.headerName = name;
            return this;
        }

        public CsrfSetting paramName(String name) {
            this.paramName = name;
            return this;
        }

        public CsrfSetting cookieName(String name) {
            this.cookieName = name;
            return this;
        }

        public CsrfSetting protector(CSRFProtector protector) {
            this.protector = $.requireNotNull(protector);
            return this;
        }

        private void checkAndCommit() {
            if (!enabled) {
                logger.info("Global CSRF is disabled");
                conf.enableCsrf(false);
            }
            logger.info("Global CSRF is enabled");
            conf.csrfCookieName(this.cookieName);
            conf.csrfHeaderName(this.headerName);
            conf.csrfParamName(this.paramName);
            conf.csrfProtector(this.protector);
        }
    }

    protected static class CorsSetting extends LogSupport {
        private AppConfigurator conf;
        private boolean enabled;
        private String allowOrigin;
        private int maxAge;
        private List<String> headersBoth = new ArrayList<String>();
        private List<String> headersAllowed = new ArrayList<String>();
        private List<String> headersExpose = new ArrayList<String>();

        CorsSetting(AppConfigurator conf) {
            this.conf = conf;
            this.enabled = true;
            conf.app().jobManager().on(SysEventId.CONFIG_PREMERGE, "CorsSetting:checkAndCommit", new Runnable() {
                @Override
                public void run() {
                    checkAndCommit();
                }
            });
        }

        public CorsSetting enable() {
            enabled = true;
            return this;
        }

        public CorsSetting disable() {
            enabled = false;
            return this;
        }

        public CorsSetting allowOrigin(String allowOrigin) {
            E.illegalArgumentIf(S.blank(allowOrigin), "allow origin cannot be empty");
            this.allowOrigin = allowOrigin;
            return this;
        }

        public CorsSetting maxAge(int maxAge) {
            E.illegalArgumentIf(maxAge < 0);
            this.maxAge = maxAge;
            return this;
        }

        public CorsSetting allowHeaders(String ... headers) {
            headersAllowed.addAll(C.listOf(headers));
            return this;
        }

        public CorsSetting exposeHeaders(String ... headers) {
            headersExpose.addAll(C.listOf(headers));
            return this;
        }

        /**
         * This method is deprecated. Please use
         * * {@link #allowHeaders(String...)} and
         * * {@link #exposeHeaders(String...)}
         *
         * @param headers
         * @return
         */
        @Deprecated
        public CorsSetting allowAndExposeHeaders(String ... headers) {
            List<String> headerList = C.listOf(headers);
            headersAllowed.addAll(headerList);
            headersExpose.addAll(headerList);
            return this;
        }

        private void checkAndCommit() {
            if (!enabled) {
                logger.info("Global CORS is disabled");
                conf.enableCors(false);
                return;
            }
            logger.info("Global CORS is enabled");
            conf.enableCors(true);
            conf.corsAllowOrigin(allowOrigin);
            conf.corsHeaders(consolidate(headersBoth));
            conf.corsAllowHeaders(consolidate(headersAllowed));
            conf.corsHeadersExpose(consolidate(headersExpose));
            conf.corsMaxAge(maxAge);
        }

        private String consolidate(List<String> stringList) {
            if (stringList.isEmpty()) {
                return null;
            }
            Set<String> set = new HashSet<String>();
            for (String s : stringList) {
                set.addAll(C.listOf(s.split(",")));
            }
            return S.join(", ", set);
        }
    }

}
