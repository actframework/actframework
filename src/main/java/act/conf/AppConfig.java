package act.conf;

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

import act.Act;
import act.Constants;
import act.app.ActionContext;
import act.app.App;
import act.app.AppHolder;
import act.app.ProjectLayout;
import act.app.conf.AppConfigurator;
import act.app.event.AppEventId;
import act.app.util.NamedPort;
import act.cli.CliOverHttpAuthority;
import act.db.util.SequenceNumberGenerator;
import act.db.util._SequenceNumberGenerator;
import act.handler.UnknownHttpMethodProcessor;
import act.handler.event.ResultEvent;
import act.i18n.I18n;
import act.security.CSRFProtector;
import act.ws.DefaultSecureTicketCodec;
import act.ws.SecureTicketCodec;
import act.ws.UsernameSecureTicketCodec;
import act.util.*;
import act.view.TemplatePathResolver;
import act.view.View;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.cache.CacheService;
import org.osgl.cache.CacheServiceProvider;
import org.osgl.exception.ConfigurationException;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.MvcConfig;
import org.osgl.util.*;
import org.osgl.web.util.UserAgent;

import javax.inject.Provider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static act.conf.AppConfigKey.*;

public class AppConfig<T extends AppConfigurator> extends Config<AppConfigKey> implements AppHolder<AppConfig<T>> {

    protected static Logger logger = L.get(AppConfig.class);

    public static final String CONF_FILE_NAME = "app.conf";

    public static final String PORT_CLI_OVER_HTTP = "__admin__";

    private App app;

    static {
        MvcConfig.errorPageRenderer(new ActErrorPageRender());
        MvcConfig.beforeCommitResultHandler(ResultEvent.BEFORE_COMMIT_HANDLER);
        MvcConfig.afterCommitResultHandler(ResultEvent.AFTER_COMMIT_HANDLER);
        MvcConfig.messageTranslater(new $.Transformer<String, String>() {
            @Override
            public String transform(String message) {
                if (Act.appConfig().i18nEnabled()) {
                    String translated = I18n.i18n(message);
                    if (message == translated) {
                        translated = I18n.i18n(MvcConfig.class, message);
                        message = translated;
                    }
                    return message;
                }
                return message;
            }
        });
    }

    /**
     * Construct a <code>AppConfig</code> with a map. The map is copied to
     * the original map of the configuration instance
     *
     * @param configuration
     */
    public AppConfig(Map<String, ?> configuration) {
        super(configuration);
    }

    public AppConfig() {
        this((Map) System.getProperties());
    }

    public AppConfig<T> app(App app) {
        E.NPE(app);
        this.app = app;
        AppConfigKey.onApp(app);
        return this;
    }

    public void preloadConfigurations() {
        // preload frequently used configurations so
        // that we do not need to synchronize on them
        // and avoid the NPE
        basicAuthenticationEnabled();
        corsEnabled();
        sessionCookieName();
        cookiePrefix();
        encryptSession();
        sessionMapper();
        sessionKeyUsername();
        sessionSecure();
        sessionTtl();

        MvcConfig.renderJsonOutputCharset(renderJsonOutputCharset());
        Osgl.Func0<H.Format> jsonContentProvider = jsonContentTypeProvider();
        if (null != jsonContentTypeProvider) {
            MvcConfig.jsonMediaTypeProvider(jsonContentProvider);
        }
    }

    public App app() {
        return app;
    }

    @Override
    protected ConfigKey keyOf(String s) {
        return AppConfigKey.valueOfIgnoreCase(s);
    }

    private Boolean basicAuth;

    protected T enableBasicAuthentication(boolean b) {
        this.basicAuth = b;
        return me();
    }

    public boolean basicAuthenticationEnabled() {
        if (null == basicAuth) {
            Boolean B = get(BASIC_AUTHENTICATION);
            if (null == B) {
                B = Act.isDev();
            }
            this.basicAuth = B;
        }
        return this.basicAuth;
    }

    private void _mergeBasicAuthentication(AppConfig conf) {
        if (!hasConfiguration(BASIC_AUTHENTICATION)) {
            this.basicAuth = conf.basicAuth;
        }
    }

    private Boolean cors;

    protected T enableCors(boolean b) {
        this.cors = b;
        return me();
    }

    public boolean corsEnabled() {
        if (null == cors) {
            Boolean B = get(CORS);
            if (null == B) {
                B = false;
            }
            this.cors = B;
        }
        return this.cors;
    }

    private void _mergeCors(AppConfig conf) {
        if (!hasConfiguration(CORS)) {
            this.cors = conf.cors;
        }
    }

    private String corsOrigin;

    protected T corsAllowOrigin(String s) {
        this.corsOrigin = s;
        return me();
    }

    public String corsAllowOrigin() {
        if (null == corsOrigin) {
            String s = get(CORS_ORIGIN);
            if (S.blank(s)) {
                s = "*";
            }
            corsOrigin = s;
        }
        return corsOrigin;
    }

    private void _mergeCorsOrigin(AppConfig conf) {
        if (!hasConfiguration(CORS_ORIGIN)) {
            corsOrigin = conf.corsOrigin;
        }
    }

    private String corsHeaders;

    protected T corsHeaders(String s) {
        this.corsHeaders = s;
        return me();
    }

    private String corsHeaders() {
        if (null == corsHeaders) {
            String s = get(CORS_HEADERS);
            if (null == s) {
                s = "Content-Type, X-HTTP-Method-Override, X-Requested-With";
            }
            corsHeaders = s;
        }
        return corsHeaders;
    }

    private void _mergeCorsHeaders(AppConfig conf) {
        if (!hasConfiguration(CORS_HEADERS)) {
            corsHeaders = conf.corsHeaders;
        }
    }

    private String corsHeadersExpose;

    protected T corsHeadersExpose(String s) {
        this.corsHeadersExpose = s;
        return me();
    }

    public String corsExposeHeaders() {
        if (null == corsHeadersExpose) {
            String s = get(CORS_HEADERS_EXPOSE);
            if (null == s) {
                s = corsHeaders();
            }
            corsHeadersExpose = s;
        }
        return corsHeadersExpose;
    }

    private void _mergeCorsHeadersExpose(AppConfig conf) {
        if (!hasConfiguration(CORS_HEADERS_EXPOSE)) {
            corsHeadersExpose = conf.corsHeadersExpose;
        }
    }

    private Boolean corsOptionCheck;
    protected T corsOptionCheck(Boolean b) {
        this.corsOptionCheck = b;
        return me();
    }
    public Boolean corsOptionCheck() {
        if (null == corsOptionCheck) {
            Boolean b = get(CORS_CHECK_OPTION_METHOD);
            corsOptionCheck = null == b ? true : b;
        }
        return corsOptionCheck;
    }
    private void _mergeCorsOptionCheck(AppConfig conf) {
        if (!hasConfiguration(CORS_CHECK_OPTION_METHOD)) {
            corsOptionCheck = conf.corsOptionCheck;
        }
    }

    private String corsHeadersAllowed;

    protected T corsAllowHeaders(String s) {
        this.corsHeadersExpose = s;
        return me();
    }

    public String corsAllowHeaders() {
        if (null == corsHeadersAllowed) {
            String s = get(CORS_HEADERS_ALLOWED);
            if (null == s) {
                s = corsHeaders();
            }
            corsHeadersAllowed = s;
        }
        return corsHeadersAllowed;
    }

    private void _mergeCorsHeadersAllowed(AppConfig conf) {
        if (!hasConfiguration(CORS_HEADERS_EXPOSE)) {
            corsHeadersAllowed = conf.corsHeadersAllowed;
        }
    }


    private Integer corsMaxAge;

    protected T corsMaxAge(int corsMaxAge) {
        this.corsMaxAge = corsMaxAge;
        return me();
    }

    public int corsMaxAge() {
        if (null == corsMaxAge) {
            Integer I = getInteger(CORS_MAX_AGE);
            if (null == I) {
                I = 30 * 60;
            }
            corsMaxAge = I;
        }
        return corsMaxAge;
    }

    private void _mergeCorsMaxAge(AppConfig conf) {
        if (!hasConfiguration(CORS_MAX_AGE)) {
            corsMaxAge = conf.corsMaxAge;
        }
    }

    private Boolean corsAllowCredentials;
    protected T corsAllowCredentials(boolean b) {
        this.corsAllowCredentials = b;
        return me();
    }
    public boolean corsAllowCredentials() {
        if (null == corsAllowCredentials) {
            corsAllowCredentials = get(CORS_ALLOW_CREDENTIALS);
            if (null == corsAllowCredentials) {
                corsAllowCredentials = false;
            }
        }
        return corsAllowCredentials;
    }
    private void _mergeCorsAllowCredential(AppConfig conf) {
        if (!hasConfiguration(CORS_ALLOW_CREDENTIALS)) {
            corsAllowCredentials = conf.corsAllowCredentials;
        }
    }


    private Boolean csrf;

    protected T enableCsrf(boolean b) {
        this.csrf = b;
        return me();
    }

    public boolean csrfEnabled() {
        if (null == csrf) {
            Boolean B = get(CSRF);
            if (null == B) {
                B = false;
            }
            this.csrf = B;
        }
        return this.csrf;
    }

    private void _mergeCsrf(AppConfig conf) {
        if (!hasConfiguration(CSRF)) {
            this.csrf = conf.csrf;
        }
    }

    private String csrfParamName;

    protected T csrfParamName(String s) {
        this.csrfParamName = s;
        return me();
    }

    public String csrfParamName() {
        if (null == csrfParamName) {
            String s = get(CSRF_PARAM_NAME);
            if (S.blank(s)) {
                s = ActionContext.ATTR_CSRF_TOKEN;
            }
            csrfParamName = s;
        }
        return csrfParamName;
    }

    private void _mergeCsrfParamName(AppConfig conf) {
        if (!hasConfiguration(CSRF_PARAM_NAME)) {
            csrfParamName = conf.csrfParamName;
        }
    }

    private CSRFProtector csrfProtector;

    protected T csrfProtector(CSRFProtector protector) {
        this.csrfProtector = $.notNull(protector);
        return me();
    }

    public CSRFProtector csrfProtector() {
        if (null == csrfProtector) {
            try {
                csrfProtector = get(CSRF_PROTECTOR);
                if (null == csrfProtector) {
                    csrfProtector = CSRFProtector.Predefined.HMAC;
                }
            } catch (ConfigurationException e) {
                Object obj = helper.getValFromAliases(raw, CSRF_PROTECTOR.key(), "impl", null);
                if (null != obj) {
                    this.csrfProtector = CSRFProtector.Predefined.valueOfIgnoreCase(obj.toString());
                    if (null != csrfProtector) {
                        return this.csrfProtector;
                    }
                }
                throw e;
            }
        }
        return csrfProtector;
    }

    private void _mergeCsrfProtector(AppConfig config) {
        if (!hasConfiguration(CSRF_PROTECTOR)) {
            csrfProtector = config.csrfProtector;
        }
    }

    private String csrfCookieName;

    protected T csrfCookieName(String s) {
        this.csrfCookieName = s;
        return me();
    }

    public String csrfCookieName() {
        if (null == csrfCookieName) {
            String s = get(CSRF_COOKIE_NAME);
            if (S.blank(s)) {
                s = cookieName("xsrf-token");
            }
            csrfCookieName = s;
        }
        return csrfCookieName;
    }

    private void _mergeCsrfCookieName(AppConfig conf) {
        if (!hasConfiguration(CSRF_COOKIE_NAME)) {
            csrfCookieName = conf.csrfCookieName;
        }
    }

    private String csrfHeaderName;

    protected T csrfHeaderName(String s) {
        this.csrfHeaderName = s;
        return me();
    }

    public String csrfHeaderName() {
        if (null == csrfHeaderName) {
            String s = get(CSRF_HEADER_NAME);
            if (S.blank(s)) {
                s = H.Header.Names.X_XSRF_TOKEN;
            }
            csrfHeaderName = s;
        }
        return csrfHeaderName;
    }

    private void _mergeCsrfHeaderName(AppConfig conf) {
        if (!hasConfiguration(CSRF_HEADER_NAME)) {
            csrfHeaderName = conf.csrfHeaderName;
        }
    }

    private Boolean cliEnabled;

    protected T cliEnable(boolean enable) {
        cliEnabled = enable;
        return me();
    }

    public boolean cliEnabled() {
        if (null == cliEnabled) {
            cliEnabled = get(CLI_ENABLED);
            if (null == cliEnabled) {
                cliEnabled = true;
            }
        }
        return cliEnabled;
    }

    private void _mergeCliEnabled(AppConfig conf) {
        if (!hasConfiguration(CLI_ENABLED)) {
            cliEnabled = conf.cliEnabled;
        }
    }
    
    private int cliTablePageSz = -1;

    protected T cliTablePageSz(int sz) {
        E.illegalArgumentIf(sz < 1, "CLI table page size not valid: %s", sz);
        this.cliTablePageSz = sz;
        return me();
    }

    public int cliTablePageSize() {
        if (-1 == cliTablePageSz) {
            Integer I = getInteger(CLI_PAGE_SIZE_TABLE);
            if (null == I) {
                I = 22;
            }
            cliTablePageSz = I;
        }
        return cliTablePageSz;
    }

    private void _mergeCliTablePageSz(AppConfig conf) {
        if (!hasConfiguration(CLI_PAGE_SIZE_TABLE)) {
            cliTablePageSz = conf.cliTablePageSz;
        }
    }

    private int cliJSONPageSz = -1;

    protected T cliJSONPageSz(int sz) {
        E.illegalArgumentIf(sz < 1, "CLI JSON page size not valid: %s", sz);
        this.cliJSONPageSz = sz;
        return me();
    }

    public int cliJSONPageSize() {
        if (-1 == cliJSONPageSz) {
            Integer I = getInteger(CLI_PAGE_SIZE_TABLE);
            if (null == I) {
                I = 22;
            }
            cliJSONPageSz = I;
        }
        return cliJSONPageSz;
    }

    private void _mergeCliJSONPageSz(AppConfig conf) {
        if (!hasConfiguration(CLI_PAGE_SIZE_TABLE)) {
            cliJSONPageSz = conf.cliJSONPageSz;
        }
    }

    private Boolean cliOverHttp;

    protected T cliOverHttp(boolean enabled) {
        this.cliOverHttp = enabled;
        return me();
    }

    public boolean cliOverHttp() {
        if (null == cliOverHttp) {
            Boolean B = get(CLI_OVER_HTTP);
            if (null == B) {
                B = false;
            }
            cliOverHttp = B;
        }
        return cliOverHttp;
    }

    private void _mergeCliOverHttp(AppConfig config) {
        if (!hasConfiguration(CLI_OVER_HTTP)) {
            cliOverHttp = config.cliOverHttp;
        }
    }

    private CliOverHttpAuthority cliOverHttpAuthority;

    protected T cliOverHttpAuthority(CliOverHttpAuthority authority) {
        this.cliOverHttpAuthority = authority;
        return me();
    }

    public CliOverHttpAuthority cliOverHttpAuthority() {
        if (null == cliOverHttpAuthority) {
            cliOverHttpAuthority = get(CLI_OVER_HTTP_AUTHORITY);
            if (null == cliOverHttpAuthority) {
                cliOverHttpAuthority = new CliOverHttpAuthority.AllowAll();
            }
        }
        return cliOverHttpAuthority;
    }

    private void _mergeCliOverHttpAuthority(AppConfig config) {
        if (!hasConfiguration(CLI_OVER_HTTP_AUTHORITY)) {
            cliOverHttpAuthority = config.cliOverHttpAuthority;
        }
    }

    Integer cliOverHttpPort;

    protected T cliOverHttpPort(int port) {
        this.cliOverHttpPort = port;
        return me();
    }

    public int cliOverHttpPort() {
        if (null == cliOverHttpPort) {
            String s = get(CLI_OVER_HTTP_PORT);
            cliOverHttpPort = null == s ? 5462 : Integer.parseInt(s);
        }
        return cliOverHttpPort;
    }

    private void _mergeCliOverHttpPort(AppConfig config) {
        if (!hasConfiguration(CLI_OVER_HTTP_PORT)) {
            cliOverHttpPort = config.cliOverHttpPort;
        }
    }

    String cliOverHttpTitle;

    protected T cliOverHttpTitle(String title) {
        this.cliOverHttpTitle = title;
        return me();
    }

    public String cliOverHttpTitle() {
        if (null == cliOverHttpTitle) {
            cliOverHttpTitle = get(CLI_OVER_HTTP_TITLE);
            if (null == cliOverHttpTitle) {
                cliOverHttpTitle = "Cli Over Http";
            }
        }
        return cliOverHttpTitle;
    }

    private void _mergeCliOverHttpTitle(AppConfig config) {
        if (!hasConfiguration(CLI_OVER_HTTP_TITLE)) {
            cliOverHttpTitle = config.cliOverHttpTitle;
        }
    }

    Boolean cliOverHttpSysCmd;

    protected T cliOverHttpSysCmd(boolean enabled) {
        this.cliOverHttpSysCmd = enabled;
        return me();
    }

    public boolean cliOverHttpSysCmdEnabled() {
        if (null == cliOverHttpSysCmd) {
            cliOverHttpSysCmd = get(CLI_OVER_HTTP_SYS_CMD);
            if (null == cliOverHttpSysCmd) {
                cliOverHttpSysCmd = true;
            }
        }
        return cliOverHttpSysCmd;
    }

    private void _mergeCliOverHttpSysCmd(AppConfig config) {
        if (!hasConfiguration(CLI_OVER_HTTP_SYS_CMD)) {
            cliOverHttpSysCmd = config.cliOverHttpSysCmd;
        }
    }

    private Integer cliPort;

    protected T cliPort(int port) {
        E.illegalArgumentIf(port < 1, "port value not valid: %s", port);
        this.cliPort = port;
        return me();
    }

    public int cliPort() {
        if (null == cliPort) {
            String s = get(CLI_PORT);
            cliPort = null == s ? 5461 : Integer.parseInt(s);
        }
        return cliPort;
    }

    private void _mergeCliPort(AppConfig conf) {
        if (!hasConfiguration(CLI_PORT)) {
            cliPort = conf.cliPort;
        }
    }

    private int cliSessionExpiration = -1;

    protected T cliSessionExpiration(int expire) {
        E.illegalArgumentIf(expire < 1, "cli session expire not valid: %s", expire);
        this.cliSessionExpiration = expire;
        return me();
    }

    public int cliSessionExpiration() {
        if (-1 == cliSessionExpiration) {
            Integer I = getInteger(CLI_SESSION_EXPIRATION);
            if (null == I) {
                I = 300;
            }
            cliSessionExpiration = I;
        }
        return cliSessionExpiration;
    }

    private void _mergeCliSessionExpiration(AppConfig conf) {
        if (!hasConfiguration(CLI_SESSION_EXPIRATION)) {
            cliSessionExpiration = conf.cliSessionExpiration;
        }
    }

    private String dspToken;

    protected T dspToken(final String tokenName) {
        this.dspToken = tokenName;
        return me();
    }

    public String dspToken() {
        if (null == dspToken) {
            dspToken = get(DOUBLE_SUBMISSION_PROTECT_TOKEN);
            if (null == dspToken) {
                dspToken = "act_dsp_token";
            }
        }
        return dspToken;
    }

    private void _mergeDspToken(AppConfig conf) {
        if (!hasConfiguration(DOUBLE_SUBMISSION_PROTECT_TOKEN)) {
            dspToken = conf.dspToken;
        }
    }

    private Provider<String> cookieDomainProvider;

    protected T cookieDomain(final String domain) {
        this.cookieDomainProvider = new Provider<String>() {
            @Override
            public String get() {
                return domain;
            }
        };
        return me();
    }

    protected T cookieDomainProvider(Provider<String> provider) {
        this.cookieDomainProvider = $.notNull(provider);
        return me();
    }

    public String cookieDomain() {
        String domain = cookieDomainProvider().get();
        return "localhost".equals(domain) ? null : domain;
    }

    private Provider<String> cookieDomainProvider() {
        if (null == cookieDomainProvider) {
            try {
                cookieDomainProvider = get(COOKIE_DOMAIN_PROVIDER);
            } catch (ConfigurationException e) {
                Object obj = helper.getValFromAliases(raw, COOKIE_DOMAIN_PROVIDER.key(), "impl", null);
                String s = obj.toString();
                if ("dynamic".equalsIgnoreCase(s) || "flexible".equalsIgnoreCase(s) || "contextual".equalsIgnoreCase(s)) {
                    cookieDomainProvider = new Provider<String>() {
                        @Override
                        public String get() {
                            H.Request req = ActionContext.current().req();
                            return req.domain();
                        }
                    };
                    return cookieDomainProvider;
                }
                throw e;
            }
            if (null == cookieDomainProvider) {
                cookieDomainProvider = new Provider<String>() {
                    @Override
                    public String get() {
                        return host();
                    }
                };
            }
        }
        return cookieDomainProvider;
    }

    private void _mergeCookieDomain(AppConfig config) {
        if (!hasConfiguration(COOKIE_DOMAIN_PROVIDER)) {
            cookieDomainProvider = config.cookieDomainProvider;
        }
    }

    private int maxCliSession = -1;

    protected T maxCliSession(int size) {
        E.illegalArgumentIf(size < 1, "max cli session number cannot be zero or negative number: %s", size);
        this.maxCliSession = size;
        return me();
    }

    public int maxCliSession() {
        if (-1 == maxCliSession) {
            Integer I = getInteger(CLI_SESSION_MAX);
            if (null == I) {
                I = 3;
            }
            maxCliSession = I;
        }
        return maxCliSession;
    }

    private void _mergeMaxCliSession(AppConfig conf) {
        if (!hasConfiguration(CLI_SESSION_MAX)) {
            maxCliSession = conf.maxCliSession;
        }
    }

    private Boolean enumResolvingCaseSensitive;
    protected T enumResolvingCaseSensitive(boolean b) {
        enumResolvingCaseSensitive = b;
        return me();
    }
    public boolean enumResolvingCaseSensitive() {
        synchronized (ENUM_RESOLVING_CASE_SENSITIVE) {
            if (null == enumResolvingCaseSensitive) {
                enumResolvingCaseSensitive = get(ENUM_RESOLVING_CASE_SENSITIVE);
                if (null == enumResolvingCaseSensitive) {
                    enumResolvingCaseSensitive = false;
                }
            }
            return enumResolvingCaseSensitive;
        }
    }
    private void _mergeEnumResolvingCaseSensitive(AppConfig conf) {
        if (!hasConfiguration(ENUM_RESOLVING_CASE_SENSITIVE)) {
            enumResolvingCaseSensitive = conf.enumResolvingCaseSensitive;
        }
    }

    private String defViewName = null;
    private View defView = null;

    protected T defaultView(View view) {
        E.NPE(view);
        defView = view;
        return me();
    }

    public View defaultView() {
        if (null == defViewName) {
            defViewName = get(AppConfigKey.VIEW_DEFAULT);
            if (null == defViewName) {
                defViewName = "rythm";
            }
            defView = Act.viewManager().view(defViewName);
        }
        return defView;
    }

    private void _mergeDefaultView(AppConfig conf) {
        if (!hasConfiguration(AppConfigKey.VIEW_DEFAULT)) {
            defViewName = conf.defViewName;
            defView = conf.defView;
        }
    }

    private String xForwardedProtocol = null;

    protected T forceHttps() {
        xForwardedProtocol = "https";
        return me();
    }

    public String xForwardedProtocol() {
        if (null == xForwardedProtocol) {
            xForwardedProtocol = get(X_FORWARD_PROTOCOL);
            if (null == xForwardedProtocol) {
                xForwardedProtocol = "http";
            }
        }
        return xForwardedProtocol;
    }

    private void _mergeXForwardedProtocol(AppConfig conf) {
        if (!hasConfiguration(X_FORWARD_PROTOCOL)) {
            xForwardedProtocol = conf.xForwardedProtocol;
        }
    }


    private Boolean contentSuffixAware = null;

    protected T contentSuffixAware(boolean b) {
        contentSuffixAware = b;
        return me();
    }

    public Boolean contentSuffixAware() {
        if (null == contentSuffixAware) {
            contentSuffixAware = get(CONTENT_SUFFIX_AWARE);
            if (null == contentSuffixAware) {
                contentSuffixAware = false;
            }
        }
        return contentSuffixAware;
    }

    private void _mergeContentSuffixAware(AppConfig conf) {
        if (!hasConfiguration(CONTENT_SUFFIX_AWARE)) {
            contentSuffixAware = conf.contentSuffixAware;
        }
    }

    private _SequenceNumberGenerator seqGen = null;

    protected T sequenceNumberGenerator(_SequenceNumberGenerator seqGen) {
        this.seqGen = seqGen;
        return me();
    }

    public _SequenceNumberGenerator sequenceNumberGenerator() {
        if (null == seqGen) {
            seqGen = get(DB_SEQ_GENERATOR);
            if (null == seqGen) {
                javax.inject.Provider<_SequenceNumberGenerator> provider = app().getInstance(SequenceNumberGenerator.Provider.class);
                seqGen = provider.get();
                logger.debug("Sequence number generator loaded: %s", seqGen.getClass().getName());
            }
        }
        return seqGen;
    }

    private void _mergeSequenceNumberGenerator(AppConfig conf) {
        if (!hasConfiguration(DB_SEQ_GENERATOR)) {
            seqGen = conf.seqGen;
        }
    }

    private ErrorTemplatePathResolver errorTemplatePathResolver = null;

    protected T errorTemplatePathResolver(ErrorTemplatePathResolver resolver) {
        errorTemplatePathResolver = resolver;
        return me();
    }

    public ErrorTemplatePathResolver errorTemplatePathResolver() {
        if (null == errorTemplatePathResolver) {
            errorTemplatePathResolver = get(RESOLVER_ERROR_TEMPLATE_PATH);
            if (null == errorTemplatePathResolver) {
                errorTemplatePathResolver = new ErrorTemplatePathResolver.DefaultErrorTemplatePathResolver();
            }
        }
        return errorTemplatePathResolver;
    }

    private void _mergeErrorTemplatePathResolver(AppConfig conf) {
        if (!hasConfiguration(RESOLVER_ERROR_TEMPLATE_PATH)) {
            errorTemplatePathResolver = conf.errorTemplatePathResolver;
        }
    }

    private String host = null;

    protected T host(String hostname) {
        hostname = hostname.trim();
        E.illegalArgumentIf(hostname.length() == 0, "hostname cannot be empty");
        host = hostname;
        return me();
    }

    public String host() {
        if (null == host) {
            host = get(HOST);
            if (null == host) {
                logger.warn("host is not configured. Use localhost as hostname");
                host = "localhost";
            }
        }
        return host;
    }

    private void _mergeHost(AppConfig conf) {
        if (!hasConfiguration(HOST)) {
            host = conf.host;
        }
    }
    
    private Boolean i18nEnabled;
    protected T i18n(boolean enabled) {
        i18nEnabled = enabled;
        return me();
    }
    public boolean i18nEnabled() {
        if (null == i18nEnabled) {
            Boolean b = get(I18N);
            if (null == b) {
                b = false;
            }
            i18nEnabled = b;
        }
        return i18nEnabled;
    }
    private void _mergeI18nEnabled(AppConfig conf) {
        if (!hasConfiguration(I18N)) {
            i18nEnabled = conf.i18nEnabled;
        }
    }
    
    private String localeParamName;
    protected T localeParamName(String name) {
        E.illegalArgumentIf(S.blank(name), "locale param name must not be empty");
        localeParamName = name;
        return me();
    }
    public String localeParamName() {
        if (null == localeParamName) {
            String s = get(I18N_LOCALE_PARAM_NAME);
            if (S.blank(s)) {
                s = "act_locale";
            }
            localeParamName = s;
        }
        return localeParamName;
    }
    private void _mergeLocaleParamName(AppConfig conf) {
        if (!hasConfiguration(I18N_LOCALE_PARAM_NAME)) {
            localeParamName = conf.localeParamName;
        }
    }


    private String localeCookieName;
    protected T localeCookieName(String name) {
        E.illegalArgumentIf(S.blank(name), "locale Cookie name must not be empty");
        localeCookieName = name;
        return me();
    }
    public String localeCookieName() {
        if (null == localeCookieName) {
            String s = get(I18N_LOCALE_COOKIE_NAME);
            if (S.blank(s)) {
                s = cookieName("locale");
            }
            localeCookieName = s;
        }
        return localeCookieName;
    }
    private void _mergeLocaleCookieName(AppConfig conf) {
        if (!hasConfiguration(I18N_LOCALE_COOKIE_NAME)) {
            localeCookieName = conf.localeCookieName;
        }
    }

    Integer ipEffectiveBytes;
    protected T ipEffectiveBytes(int n) {
        E.illegalArgumentIf(n < 1 || n > 4, "integer from 1 to 4 (inclusive) expected");
        ipEffectiveBytes = n;
        return me();
    }
    public int ipEffectiveBytes() {
        if (null == ipEffectiveBytes) {
            ipEffectiveBytes = getInteger(ID_GEN_NODE_ID_EFFECTIVE_IP_BYTES);
            if (null == ipEffectiveBytes) {
                ipEffectiveBytes = 4;
            }
        }
        return ipEffectiveBytes;
    }
    private void _mergeIpEffectiveBytes(AppConfig conf) {
        if (!hasConfiguration(ID_GEN_NODE_ID_EFFECTIVE_IP_BYTES)) {
            ipEffectiveBytes = conf.ipEffectiveBytes;
        }
    }

    private IdGenerator.NodeIdProvider nodeIdProvider;
    protected T nodeIdProvider(IdGenerator.NodeIdProvider provider) {
        this.nodeIdProvider = $.NPE(provider);
        return me();
    }
    public IdGenerator.NodeIdProvider nodeIdProvider() {
        if (null == nodeIdProvider) {
            nodeIdProvider = get(ID_GEN_NODE_ID_PROVIDER);
            if (null == nodeIdProvider) {
                nodeIdProvider = new IdGenerator.NodeIdProvider.IpProvider(ipEffectiveBytes());
            }
        }
        return nodeIdProvider;
    }
    private void _mergeNodeIdProvider(AppConfig conf) {
        if (!hasConfiguration(ID_GEN_NODE_ID_PROVIDER)) {
            nodeIdProvider = conf.nodeIdProvider;
        }
    }

    private String startIdFile;
    protected T startIdFile(String file) {
        E.illegalArgumentIf(S.blank(file));
        startIdFile = file;
        return me();
    }
    public String startIdFile() {
        if (null == startIdFile) {
            startIdFile = get(ID_GEN_START_ID_FILE);
            if (null == startIdFile) {
                startIdFile = ".act.id-app";
            }
        }
        return startIdFile;
    }
    private void _mergeStartIdFile(AppConfig conf) {
        if (!hasConfiguration(ID_GEN_START_ID_FILE)) {
            startIdFile = conf.startIdFile;
        }
    }

    private IdGenerator.StartIdProvider startIdProvider;
    protected T startIdProvider(IdGenerator.StartIdProvider provider) {
        startIdProvider = $.NPE(provider);
        return me();
    }
    public IdGenerator.StartIdProvider startIdProvider() {
        if (null == startIdProvider) {
            startIdProvider = get(ID_GEN_START_ID_PROVIDER);
            if (null == startIdProvider) {
                startIdProvider = new IdGenerator.StartIdProvider.DefaultStartIdProvider(startIdFile());
            }
        }
        return startIdProvider;
    }
    private void _mergeStartIdProvider(AppConfig conf) {
        if (!hasConfiguration(ID_GEN_START_ID_PROVIDER)) {
            startIdProvider = conf.startIdProvider;
        }
    }

    private IdGenerator.SequenceProvider sequenceProvider;
    protected T sequenceProvider(IdGenerator.SequenceProvider provider) {
        this.sequenceProvider = $.NPE(provider);
        return me();
    }
    public IdGenerator.SequenceProvider sequenceProvider() {
        if (null == sequenceProvider) {
            sequenceProvider = get(ID_GEN_SEQ_ID_PROVIDER);
            if (null == sequenceProvider) {
                sequenceProvider = new IdGenerator.SequenceProvider.AtomicLongSeq();
            }
        }
        return sequenceProvider;
    }
    private void _mergeSequenceProvider(AppConfig conf) {
        if (!hasConfiguration(ID_GEN_SEQ_ID_PROVIDER)) {
            sequenceProvider = conf.sequenceProvider;
        }
    }


    private IdGenerator.LongEncoder longEncoder;
    protected T longEncoder(IdGenerator.LongEncoder longEncoder) {
        this.longEncoder = $.NPE(longEncoder);
        return me();
    }
    public IdGenerator.LongEncoder longEncoder() {
        if (null == longEncoder) {
            longEncoder = get(ID_GEN_LONG_ENCODER);
            if (null == longEncoder) {
                longEncoder = IdGenerator.SAFE_ENCODER;
            }
        }
        return longEncoder;
    }
    private void _mergeLongEncoder(AppConfig conf) {
        if (!hasConfiguration(ID_GEN_LONG_ENCODER)) {
            longEncoder = conf.longEncoder;
        }
    }

    private String loginUrl = null;
    protected T loginUrl(String url) {
        E.illegalArgumentIf(!url.startsWith("/"), "login URL shall start with '/'");
        this.loginUrl = url;
        return me();
    }
    public String loginUrl() {
        if (null == loginUrl) {
            loginUrl = get(URL_LOGIN);
            if (null == loginUrl) {
                loginUrl = "/login";
            }
        }
        ActionContext context = ActionContext.current();
        if (null != context && context.isAjax()) {
            return ajaxLoginUrl();
        }
        return loginUrl;
    }
    private void _mergeLoginUrl(AppConfig conf) {
        if (!hasConfiguration(URL_LOGIN)) {
            loginUrl = conf.loginUrl;
        }
    }

    private String ajaxLoginUrl = null;
    protected T ajaxLoginUrl(String url) {
        E.illegalArgumentIf(!url.startsWith("/"), "login URL shall start with '/'");
        this.ajaxLoginUrl = url;
        return me();
    }
    public String ajaxLoginUrl() {
        if (null == ajaxLoginUrl) {
            ajaxLoginUrl = get(URL_LOGIN_AJAX);
            if (null == ajaxLoginUrl) {
                ajaxLoginUrl = loginUrl;
            }
            if (null == ajaxLoginUrl) {
                ajaxLoginUrl = get(URL_LOGIN);
            }
            if (null == ajaxLoginUrl) {
                ajaxLoginUrl = "/login";
            }
        }
        return ajaxLoginUrl;
    }
    private void _mergeAjaxLoginUrl(AppConfig conf) {
        if (!hasConfiguration(URL_LOGIN_AJAX)) {
            ajaxLoginUrl = conf.ajaxLoginUrl;
        }
    }

    private boolean urlContextInitialized;
    private String urlContext;
    protected T urlContext(String context) {
        this.urlContext = context;
        urlContextInitialized = S.notBlank(context);
        return me();
    }
    public String urlContext() {
        if (!urlContextInitialized) {
            urlContext = get(URL_CONTEXT);
            if (null != urlContext) {
                urlContext = urlContext.trim();
                if (urlContext.length() == 0) {
                    urlContext = null;
                } else {
                    while (urlContext.endsWith("/")) {
                        urlContext = urlContext.substring(0, urlContext.length() - 1);
                    }
                    if (urlContext.length() == 0) {
                        urlContext = null;
                    } else if (!urlContext.startsWith("/")) {
                        urlContext = S.concat("/", urlContext);
                    }
                }
                if (null != urlContext && urlContext.contains(" ")) {
                    throw E.invalidConfiguration("url context shall not contains white space");
                }
            }
            urlContextInitialized = true;
        }
        return urlContext;
    }
    private void _mergeUrlContext(AppConfig conf) {
        if (!hasConfiguration(URL_CONTEXT)) {
            urlContext = conf.urlContext;
            urlContextInitialized = conf.urlContextInitialized;
        }
    }


    private int httpMaxParams = -1;
    protected T httpMaxParams(int max) {
        E.illegalArgumentIf(max < 0, "max params cannot be negative number: %s", max);
        this.httpMaxParams = max;
        return me();
    }
    public int httpMaxParams() {
        if (-1 == httpMaxParams) {
            Integer I = getInteger(HTTP_MAX_PARAMS);
            if (null == I) {
                I = 128;
            }
            if (I < 0) {
                throw new ConfigurationException("http.params.max setting cannot be negative number. Found: %s", I);
            }
            httpMaxParams = I;
        }
        return httpMaxParams;
    }
    private void _mergeHttpMaxParams(AppConfig conf) {
        if (!hasConfiguration(HTTP_MAX_PARAMS)) {
            httpMaxParams = conf.httpMaxParams;
        }
    }

    private int jobPoolSize = -1;

    protected T jobPoolSize(int size) {
        E.illegalArgumentIf(size < 1, "job pool size cannot be zero or negative number: %s", size);
        this.jobPoolSize = size;
        return me();
    }

    public int jobPoolSize() {
        if (-1 == jobPoolSize) {
            Integer I = getInteger(JOB_POOL_SIZE);
            if (null == I) {
                I = 10;
            }
            jobPoolSize = I;
        }
        return jobPoolSize;
    }

    private void _mergeJobPoolSize(AppConfig conf) {
        if (!hasConfiguration(JOB_POOL_SIZE)) {
            jobPoolSize = conf.jobPoolSize;
        }
    }
    
    private int httpExternalPort = -1;
    
    protected T httpExternalPort(int port) {
        E.illegalArgumentIf(port < 1, "port value not valid: %s", port);
        this.httpExternalPort = port;
        return me();
    }
    
    public int httpExternalPort() {
        if (-1 == httpExternalPort) {
            String s = get(HTTP_EXTERNAL_PORT);
            httpExternalPort = null == s ? httpExternal() ? 80 : httpPort() : Integer.parseInt(s);
        }
        return httpExternalPort;
    }
    
    private void _mergeHttpExternalPort(AppConfig conf) {
        if (!hasConfiguration(HTTP_EXTERNAL_PORT)) {
            this.httpExternalPort = conf.httpExternalPort();
        }
    }

    private Boolean httpExternal = null;

    protected T httpExternal(boolean setting) {
        this.httpExternal = setting;
        return me();
    }

    public boolean httpExternal() {
        if (null == httpExternal) {
            Boolean b = get(HTTP_EXTERNAL_SERVER);
            httpExternal = null == b ? Act.mode().isProd() : b;
        }
        return httpExternal;
    }

    private void _mergeHttpExternal(AppConfig conf) {
        if (!hasConfiguration(HTTP_EXTERNAL_SERVER)) {
            httpExternal = conf.httpExternal;
        }
    }

    private int httpExternalSecurePort = -1;

    protected T httpExternalSecurePort(int port) {
        E.illegalArgumentIf(port < 1, "port value not valid: %s", port);
        this.httpExternalSecurePort = port;
        return me();
    }

    public int httpExternalSecurePort() {
        if (-1 == httpExternalSecurePort) {
            String s = get(HTTP_EXTERNAL_SECURE_PORT);
            httpExternalSecurePort = null == s ? httpExternal() ? 443 : httpPort() : Integer.parseInt(s);
        }
        return httpExternalSecurePort;
    }

    private void _mergeHttpExternalSecurePort(AppConfig conf) {
        if (!hasConfiguration(HTTP_EXTERNAL_PORT)) {
            this.httpExternalSecurePort = conf.httpExternalSecurePort;
        }
    }

    private int httpPort = -1;

    protected T httpPort(int port) {
        E.illegalArgumentIf(port < 1, "port value not valid: %s", port);
        this.httpPort = port;
        return me();
    }

    public int httpPort() {
        if (-1 == httpPort) {
            String s = get(HTTP_PORT);
            httpPort = null == s ? 5460 : Integer.parseInt(s);
        }
        return httpPort;
    }

    private void _mergeHttpPort(AppConfig conf) {
        if (!hasConfiguration(HTTP_PORT)) {
            httpPort = conf.httpPort;
        }
    }

    private Boolean httpSecure = null;

    protected T httpSecure(boolean secure) {
        this.httpSecure = secure;
        return me();
    }

    public boolean httpSecure() {
        if (null == httpSecure) {
            Boolean B = get(HTTP_SECURE);
            if (null == B) {
                B = !Act.isDev() && S.neq(Act.profile(), "dev", S.IGNORECASE);
            }
            httpSecure = B;
        }
        return httpSecure;
    }

    private void _mergeHttpSecure(AppConfig conf) {
        if (!hasConfiguration(HTTP_SECURE)) {
            httpSecure = conf.httpSecure;
        }
    }


    private int httpsPort = -1;

    protected T httpsPort(int port) {
        E.illegalArgumentIf(port < 1, "port value not valid: %s", port);
        this.httpsPort = port;
        return me();
    }

    public int httpsPort() {
        if (-1 == httpsPort) {
            String s = get(HTTPS_PORT);
            httpsPort = null == s ? 5443 : Integer.parseInt(s);
        }
        return httpsPort;
    }

    private void _mergeHttpsPort(AppConfig conf) {
        if (!hasConfiguration(HTTPS_PORT)) {
            httpsPort = conf.httpsPort;
        }
    }

    private MissingAuthenticationHandler mah = null;
    protected T missingAuthenticationHandler(MissingAuthenticationHandler handler) {
        E.NPE(handler);
        mah = handler;
        return me();
    }
    public MissingAuthenticationHandler missingAuthenticationHandler() {
        if (null == mah) {
            mah = get(HANDLER_MISSING_AUTHENTICATION);
            if (null == mah) {
                mah = app.getInstance(RedirectToLoginUrl.class);
            }
        }
        return mah;
    }
    private void _mergeMissingAuthenticationHandler(AppConfig config) {
        if (!hasConfiguration(HANDLER_MISSING_AUTHENTICATION)) {
            mah = config.mah;
        }
    }

    private MissingAuthenticationHandler ajaxMah = null;
    protected T ajaxMissingAuthenticationHandler(MissingAuthenticationHandler handler) {
        E.NPE(handler);
        ajaxMah = handler;
        return me();
    }
    public MissingAuthenticationHandler ajaxMissingAuthenticationHandler() {
        if (null == ajaxMah) {
            ajaxMah = get(HANDLER_MISSING_AUTHENTICATION_AJAX);
            if (null == ajaxMah) {
                ajaxMah = missingAuthenticationHandler();
            }
        }
        return ajaxMah;
    }
    private void _mergeAjaxMissingAuthenticationHandler(AppConfig config) {
        if (!hasConfiguration(HANDLER_MISSING_AUTHENTICATION_AJAX)) {
            ajaxMah = config.ajaxMah;
        }
    }

    private MissingAuthenticationHandler csrfCheckFailureHandler = null;
    protected T csrfCheckFailureHandler(MissingAuthenticationHandler handler) {
        E.NPE(handler);
        csrfCheckFailureHandler = handler;
        return me();
    }
    public MissingAuthenticationHandler csrfCheckFailureHandler() {
        if (null == csrfCheckFailureHandler) {
            csrfCheckFailureHandler = get(HANDLER_CSRF_CHECK_FAILURE);
            if (null == csrfCheckFailureHandler) {
                csrfCheckFailureHandler = missingAuthenticationHandler();
            }
        }
        return csrfCheckFailureHandler;
    }
    private void _mergeCsrfCheckFailureHandler(AppConfig config) {
        if (!hasConfiguration(HANDLER_CSRF_CHECK_FAILURE)) {
            csrfCheckFailureHandler = config.csrfCheckFailureHandler;
        }
    }

    private MissingAuthenticationHandler ajaxCsrfCheckFailureHandler = null;
    protected T ajaxCsrfCheckFailureHandler(MissingAuthenticationHandler handler) {
        E.NPE(handler);
        ajaxCsrfCheckFailureHandler = handler;
        return me();
    }
    public MissingAuthenticationHandler ajaxCsrfCheckFailureHandler() {
        if (null == ajaxCsrfCheckFailureHandler) {
            ajaxCsrfCheckFailureHandler = get(HANDLER_AJAX_CSRF_CHECK_FAILURE);
            if (null == ajaxCsrfCheckFailureHandler) {
                ajaxCsrfCheckFailureHandler = csrfCheckFailureHandler();
            }
        }
        return ajaxCsrfCheckFailureHandler;
    }
    private void _mergeAjaxCsrfCheckFailureHandler(AppConfig config) {
        if (!hasConfiguration(HANDLER_AJAX_CSRF_CHECK_FAILURE)) {
            csrfCheckFailureHandler = config.csrfCheckFailureHandler;
        }
    }

    private List<NamedPort> namedPorts = null;

    protected T namedPorts(NamedPort... namedPorts) {
        this.namedPorts = C.listOf(namedPorts);
        return me();
    }

    public List<NamedPort> namedPorts() {
        if (null == namedPorts) {
            String s = get(NAMED_PORTS);
            if (null == s) {
                namedPorts = cliOverHttp() ? C.list(new NamedPort(PORT_CLI_OVER_HTTP, cliOverHttpPort())) : C.<NamedPort>list();
            } else {
                String[] sa = (s.split("[,;]+"));
                ListBuilder<NamedPort> builder = ListBuilder.create();
                for (String s0 : sa) {
                    String[] sa0 = s0.split(":");
                    E.invalidConfigurationIf(2 != sa0.length, "Unknown named port configuration: %s", s);
                    String name = sa0[0].trim();
                    String val = sa0[1].trim();
                    NamedPort port = new NamedPort(name, Integer.parseInt(val));
                    if (!builder.contains(port)) {
                        builder.add(port);
                    } else {
                        throw E.invalidConfiguration("port[%s] already configured", name);
                    }
                }
                if (cliOverHttp()) {
                    builder.add(new NamedPort(PORT_CLI_OVER_HTTP, cliOverHttpPort()));
                }
                namedPorts = builder.toList();
            }
        }
        return namedPorts;
    }

    public NamedPort namedPort(String portId) {
        for (NamedPort np : namedPorts()) {
            if (np.name().equalsIgnoreCase(portId)) {
                return np;
            }
        }
        return null;
    }

    private void _mergePorts(AppConfig config) {
        if (!hasConfiguration(NAMED_PORTS)) {
            namedPorts = config.namedPorts;
        }
    }

    private String encoding = null;

    protected T encoding(String encoding) {
        encoding = encoding.trim();
        E.illegalArgumentIf(encoding.length() == 0, "encoding cannot be empty");
        this.encoding = encoding;
        return me();
    }

    public String encoding() {
        if (null == encoding) {
            encoding = get(ENCODING);
            if (null == encoding) {
                encoding = StandardCharsets.UTF_8.name();
            }
        }
        return encoding;
    }

    private void _mergeEncoding(AppConfig conf) {
        if (!hasConfiguration(ENCODING)) {
            encoding = conf.encoding;
        }
    }

    private String dateFmt = null;

    protected T dateFormat(String fmt) {
        E.illegalArgumentIf(S.blank(fmt), "Date format cannot be empty");
        this.dateFmt = fmt;
        return me();
    }

    public String dateFormat() {
        if (null == dateFmt) {
            dateFmt = get(FORMAT_DATE);
            if (null == dateFmt) {
                DateFormat formatter = DateFormat.getDateInstance();
                dateFmt = ((SimpleDateFormat) formatter).toPattern();
            }
        }
        return dateFmt;
    }

    private void _mergeDateFmt(AppConfig conf) {
        if (!hasConfiguration(FORMAT_DATE)) {
            dateFmt = conf.dateFmt;
        }
    }

    private String timeFmt = null;

    protected T timeFormat(String fmt) {
        E.illegalArgumentIf(S.blank(fmt), "Time format cannot be empty");
        this.timeFmt = fmt;
        return me();
    }

    public String timeFormat() {
        if (null == timeFmt) {
            timeFmt = get(FORMAT_TIME);
            if (null == timeFmt) {
                DateFormat formatter = DateFormat.getTimeInstance();
                timeFmt = ((SimpleDateFormat) formatter).toPattern();
            }
        }
        return timeFmt;
    }

    private void _mergeTimeFmt(AppConfig conf) {
        if (!hasConfiguration(FORMAT_TIME)) {
            timeFmt = conf.timeFmt;
        }
    }

    private String dateDateTimeFmt = null;

    protected T dateTimeFormat(String fmt) {
        E.illegalArgumentIf(S.blank(fmt), "Date time format cannot be empty");
        this.dateDateTimeFmt = fmt;
        return me();
    }

    public String dateTimeFormat() {
        if (null == dateDateTimeFmt) {
            dateDateTimeFmt = get(FORMAT_DATE_TIME);
            if (null == dateDateTimeFmt) {
                DateFormat formatter = DateFormat.getDateTimeInstance();
                dateDateTimeFmt = ((SimpleDateFormat) formatter).toPattern();
            }
        }
        return dateDateTimeFmt;
    }

    private void _mergeDateTimeFmt(AppConfig conf) {
        if (!hasConfiguration(FORMAT_DATE_TIME)) {
            dateDateTimeFmt = conf.dateDateTimeFmt;
        }
    }

    private Locale locale = null;

    protected T locale(Locale locale) {
        E.NPE(locale);
        this.locale = locale;
        return me();
    }

    public Locale locale() {
        if (null == locale) {
            locale = get(LOCALE);
            if (null == locale) {
                locale = Locale.getDefault();
            }
        }
        return locale;
    }

    private void _mergeLocale(AppConfig conf) {
        if (!hasConfiguration(LOCALE)) {
            locale = conf.locale;
        }
    }

    private String sourceVersion = null;

    protected T sourceVersion(JavaVersion version) {
        sourceVersion = FastStr.of(version.name()).substr(1).replace('_', '.').toString();
        return me();
    }

    public String sourceVersion() {
        if (null == sourceVersion) {
            sourceVersion = get(AppConfigKey.SOURCE_VERSION);
            if (null == sourceVersion) {
                sourceVersion = "1." + $.JAVA_VERSION;
            } else if (sourceVersion.startsWith("1.")) {
                sourceVersion = sourceVersion.substring(0, 3);
            }
        }
        return sourceVersion;
    }

    private void _mergeSourceVersion(AppConfig conf) {
        if (!hasConfiguration(SOURCE_VERSION)) {
            sourceVersion = conf.sourceVersion;
        }
    }

    private String targetVersion = null;

    protected T targetVersion(JavaVersion version) {
        targetVersion = FastStr.of(version.name()).substr(1).replace('_', '.').toString();
        return me();
    }

    public String targetVersion() {
        if (null == targetVersion) {
            targetVersion = get(TARGET_VERSION);
            if (null == targetVersion) {
                targetVersion = "1." + $.JAVA_VERSION;
            } else if (targetVersion.startsWith("1.")) {
                targetVersion = targetVersion.substring(0, 3);
            }
        }
        return targetVersion;
    }

    private void _mergeTargetVersion(AppConfig conf) {
        if (!hasConfiguration(TARGET_VERSION)) {
            targetVersion = conf.targetVersion;
        }
    }

    private $.Predicate<String> APP_CLASS_TESTER = null;
    private final $.Predicate<String> SYSTEM_SCAN_LIST = new $.Predicate<String>() {
        @Override
        public boolean test(String s) {
            final Set<String> scanList = app().scanList();
            if (scanList.contains(s)) {
                return true;
            }
            for (String scan: scanList) {
                if (s.matches(scan)) {
                    return true;
                }
            }
            return false;
        }
    };

    public $.Predicate<String> appClassTester() {
        if (null == APP_CLASS_TESTER) {
            String scanPackage = get(AppConfigKey.SCAN_PACKAGE);
            if (S.isBlank(scanPackage)) {
                APP_CLASS_TESTER = SYSTEM_SCAN_LIST;
            } else {
                final String[] sp = scanPackage.trim().split(Constants.LIST_SEPARATOR);
                final int len = sp.length;
                if (1 == len) {
                    APP_CLASS_TESTER = S.F.startsWith(sp[0]).or(SYSTEM_SCAN_LIST);
                } else {
                    APP_CLASS_TESTER = new $.Predicate<String>() {
                        @Override
                        public boolean test(String className) {
                            for (int i = 0; i < len; ++i) {
                                if (className.startsWith(sp[i])) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    }.or(SYSTEM_SCAN_LIST);
                }
            }
        }
        return APP_CLASS_TESTER;
    }

    public boolean needEnhancement(String className) {
        return appClassTester().test(className) || controllerNameTester().test(className);
    }

    private $.Predicate<String> CONTROLLER_CLASS_TESTER = null;

    private $.Predicate<String> controllerNameTester() {
        if (null == CONTROLLER_CLASS_TESTER) {
            $.Predicate<String> f = $.F.no();
            CONTROLLER_CLASS_TESTER = f.or(app().router().f.IS_CONTROLLER);
        }
        return CONTROLLER_CLASS_TESTER;
    }

    private TemplatePathResolver templatePathResolver = null;

    protected T templatePathResolver(TemplatePathResolver resolver) {
        E.NPE(resolver);
        templatePathResolver = resolver;
        return me();
    }

    public TemplatePathResolver templatePathResolver() {
        if (null == templatePathResolver) {
            templatePathResolver = get(AppConfigKey.RESOLVER_TEMPLATE_PATH);
            if (null == templatePathResolver) {
                templatePathResolver = new TemplatePathResolver();
            }
        }
        return templatePathResolver;
    }

    private void _mergeTemplatePathResolver(AppConfig conf) {
        if (!hasConfiguration(AppConfigKey.RESOLVER_TEMPLATE_PATH)) {
            templatePathResolver = conf.templatePathResolver;
        }
    }

    private String templateHome = null;

    protected T templateHome(String home) {
        home = home.trim();
        E.illegalArgumentIf(home.length() == 0, "template home cannot be empty");
        templateHome = home;
        return me();
    }

    public String templateHome() {
        if (null == templateHome) {
            templateHome = get(AppConfigKey.TEMPLATE_HOME);
            if (null == templateHome) {
                templateHome = "default";
            }
        }
        return templateHome;
    }

    private void _mergeTemplateHome(AppConfig conf) {
        if (!hasConfiguration(AppConfigKey.TEMPLATE_HOME)) {
            templateHome = conf.templateHome;
        }
    }

    private boolean pingPathResolved = false;
    private String pingPath = null;

    protected T pingPath(String path) {
        pingPathResolved = true;
        pingPath = path.trim();
        return me();
    }

    public String pingPath() {
        if (!pingPathResolved) {
            pingPath = get(AppConfigKey.PING_PATH);
            pingPathResolved = true;
        }
        return pingPath;
    }

    private void _mergePingPath(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.PING_PATH)) {
            pingPath = config.pingPath;
            pingPathResolved = config.pingPathResolved;
        }
    }

    private Osgl.Func0<H.Format> jsonContentTypeProvider = null;
    private Boolean renderJsonIeFix = null;
    private H.Format jsonIE;
    protected T renderJsonContentTypeIE(final String contentType) {
        setRenderJsonContenTypeIE(contentType);
        return me();
    }
    private void setRenderJsonContenTypeIE(final String contentType) {
        if (H.Format.JSON.contentType().equalsIgnoreCase(contentType)) {
            renderJsonIeFix = false;
            return;
        }
        renderJsonIeFix = true;
        jsonIE = H.Format.of("json_ie", contentType);
        jsonContentTypeProvider = new Osgl.Func0<H.Format>() {
            @Override
            public H.Format apply() throws NotAppliedException, Osgl.Break {
                ActionContext context = ActionContext.current();
                if (null != context) {
                    UserAgent ua = context.userAgent();
                    if (ua.isIE()) {
                        return jsonIE;
                    }
                }
                return H.Format.JSON;
            }
        };
    }
    public Osgl.Func0<H.Format> jsonContentTypeProvider() {
        if (null == renderJsonIeFix) {
            String contentType = get(RENDER_JSON_CONTENT_TYPE_IE);
            if (null != contentType) {
                setRenderJsonContenTypeIE(contentType);
            } else {
                renderJsonIeFix = false;
            }
        }
        return renderJsonIeFix ? jsonContentTypeProvider : null;
    }
    private void _mergeRenderJsonContentTypeIE(AppConfig conf) {
        if (!hasConfiguration(RENDER_JSON_CONTENT_TYPE_IE)) {
            jsonContentTypeProvider = conf.jsonContentTypeProvider;
            renderJsonIeFix = conf.renderJsonIeFix;
            jsonIE = conf.jsonIE;
        }
    }

    private Boolean renderJsonOutputCharset;
    protected T renderJsonOutputCharset(boolean outputCharset) {
        this.renderJsonOutputCharset = outputCharset;
        return me();
    }
    public boolean renderJsonOutputCharset() {
        if (null == renderJsonOutputCharset) {
            renderJsonOutputCharset = get(RENDER_JSON_OUTPUT_CHARSET);
            if (null == renderJsonOutputCharset) {
                renderJsonOutputCharset = false;
            }
        }
        return renderJsonOutputCharset;
    }
    private void _mergeRenderJsonOutputCharset(AppConfig config) {
        if (!hasConfiguration(RENDER_JSON_OUTPUT_CHARSET)) {
            renderJsonOutputCharset = config.renderJsonOutputCharset;
        }
    }

    private String serverHeader;

    protected T serverHeader(String header) {
        serverHeader = header;
        return me();
    }

    public String serverHeader() {
        if (null == serverHeader) {
            serverHeader = get(AppConfigKey.SERVER_HEADER);
            if (null == serverHeader) {
                serverHeader = "act";
            }
        }
        return serverHeader;
    }

    private void _mergeServerHeader(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SERVER_HEADER)) {
            serverHeader = config.serverHeader;
        }
    }

    private String sessionKeyUsername = null;

    protected T sessionKeyUsername(String name) {
        name = name.trim().toLowerCase();
        E.illegalArgumentIf(name.length() == 0, "session cookie name cannot be blank");
        sessionKeyUsername = name;
        return me();
    }

    public String sessionKeyUsername() {
        if (null == sessionKeyUsername) {
            String username = get(SESSION_KEY_USERNAME);
            if (S.blank(username)) {
                username = "username";
            }
            sessionKeyUsername = username;
        }
        return sessionKeyUsername;
    }

    private void _mergeSessionKeyUsername(AppConfig config) {
        if (!hasConfiguration(SESSION_KEY_USERNAME)) {
            sessionKeyUsername = config.sessionKeyUsername;
        }
    }

    private String cookiePrefix;

    protected T cookiePrefix(String prefix) {
        prefix = prefix.trim().toLowerCase();
        E.illegalArgumentIf(prefix.length() == 0, "cookie prefix cannot be blank");
        cookiePrefix = prefix;
        return me();
    }

    private String cookiePrefix() {
        if (null == cookiePrefix) {
            cookiePrefix = get(COOKIE_PREFIX);
            if (null == cookiePrefix) {
                cookiePrefix = S.concat(app().id(), "-");
            }
            cookiePrefix = cookiePrefix.trim().toLowerCase();
        }
        return cookiePrefix;
    }

    private void _mergeCookiePrefix(AppConfig config) {
        if (!hasConfiguration(COOKIE_PREFIX)) {
            cookiePrefix = config.cookiePrefix;
        }
    }

    private String sessionCookieName = null;

    protected T sessionCookieName(String name) {
        name = name.trim().toLowerCase();
        E.illegalArgumentIf(name.length() == 0, "session cookie name cannot be blank");
        E.illegalArgumentIf(S.eq(name, flashCookieName), "session cookie name cannot be the same with flash cookie name");
        sessionCookieName = name;
        return me();
    }

    public String sessionCookieName() {
        if (null == sessionCookieName) {
            sessionCookieName = cookieName("session");
        }
        return sessionCookieName;
    }

    private void _mergeSessionCookieName(AppConfig config) {
        if (null != config.sessionCookieName) {
            sessionCookieName = config.sessionCookieName;
        }
    }

    private String flashCookieName = null;

    protected T flashCookieName(String name) {
        name = name.trim().toLowerCase();
        E.illegalArgumentIf(name.length() == 0, "flash cookie name cannot be blank");
        E.illegalArgumentIf(S.eq(name, sessionCookieName), "flash cookie name cannot be the same with session cookie name");
        flashCookieName = name;
        return me();
    }

    public String flashCookieName() {
        if (null == flashCookieName) {
            flashCookieName = cookieName("flash");
        }
        return flashCookieName;
    }

    private void _mergeFlashCookieName(AppConfig config) {
        if (null != config.flashCookieName) {
            flashCookieName = config.flashCookieName;
        }
    }

    private Integer sessionTtl = null;

    protected T sessionTtl(int seconds) {
        sessionTtl = seconds;
        return me();
    }

    public int sessionTtl() {
        if (null == sessionTtl) {
            sessionTtl = getInteger(AppConfigKey.SESSION_TTL);
            if (null == sessionTtl) {
                sessionTtl = 60 * 30;
            }
        }
        return sessionTtl;
    }

    private void _mergeSessionTtl(AppConfig conf) {
        if (!hasConfiguration(AppConfigKey.SESSION_TTL)) {
            sessionTtl = conf.sessionTtl;
        }
    }

    private Boolean sessionPersistent = null;

    protected T sessionPersistent(boolean persistenSession) {
        sessionPersistent = persistenSession;
        return me();
    }

    public boolean persistSession() {
        if (null == sessionPersistent) {
            sessionPersistent = get(AppConfigKey.SESSION_PERSISTENT_ENABLED);
            if (null == sessionPersistent) {
                sessionPersistent = false;
            }
        }
        return sessionPersistent;
    }

    private void _mergeSessionPersistent(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SESSION_PERSISTENT_ENABLED)) {
            sessionPersistent = config.sessionPersistent;
        }
    }

    private Boolean sessionEncrypt = null;

    protected T sessionEncrypt(boolean encryptSession) {
        sessionEncrypt = encryptSession;
        return me();
    }

    public boolean encryptSession() {
        if (null == sessionEncrypt) {
            sessionEncrypt = get(AppConfigKey.SESSION_ENCRYPT_ENABLED);
            if (null == sessionEncrypt) {
                sessionEncrypt = false;
            }
        }
        return sessionEncrypt;
    }

    private void _mergeSessionEncrpt(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SESSION_ENCRYPT_ENABLED)) {
            sessionEncrypt = config.sessionEncrypt;
        }
    }

    private SessionMapper sessionMapper = null;

    protected T sessionMapper(SessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
        return me();
    }

    public SessionMapper sessionMapper() {
        if (null == sessionMapper) {
            Object o = get(SESSION_MAPPER);
            if (null == o) {
                // we might set header session mapper prefix
                sessionMapperHeaderPrefix();
            }
            if (null == sessionMapper) {
                sessionMapper = SessionMapper.DefaultSessionMapper.wrap((SessionMapper) o);
            }
        }
        return sessionMapper;
    }

    private void _mergeSessionMapper(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SESSION_MAPPER)) {
            sessionMapper = config.sessionMapper;
        }
    }

    private String sessionMapperHeaderPrefix = null;
    private boolean sessionMapperHeaderPrefixSet = false;
    protected T sessionMapperHeaderPrefix(String prefix) {
        this.sessionMapperHeaderPrefix = prefix;
        return me();
    }
    public String sessionMapperHeaderPrefix() {
        if (!sessionMapperHeaderPrefixSet) {
            sessionMapperHeaderPrefix = get(SESSION_MAPPER_HEADER_PREFIX);
            sessionMapperHeaderPrefixSet = true;
            if (null != sessionMapperHeaderPrefix) {
                this.sessionMapper = SessionMapper.DefaultSessionMapper.wrap(new SessionMapper.HeaderSessionMapper(sessionMapperHeaderPrefix));
            }
        }
        return sessionMapperHeaderPrefix;
    }
    private void _mergeSessionMapperHeaderPrefix(AppConfig config) {
        if (!hasConfiguration(SESSION_MAPPER_HEADER_PREFIX)) {
            this.sessionMapperHeaderPrefix = config.sessionMapperHeaderPrefix;
            this.sessionMapperHeaderPrefixSet = config.sessionMapperHeaderPrefixSet;
        }
    }

    private Boolean sessionSecure = null;

    protected T sessionSecure(boolean secure) {
        sessionSecure = secure;
        return me();
    }

    public boolean sessionSecure() {
        if (null == sessionSecure) {
            sessionSecure = get(AppConfigKey.SESSION_SECURE);
            if (null == sessionSecure) {
                sessionSecure = httpSecure();
            }
        }
        return sessionSecure;
    }

    private void _mergeSessionSecure(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SESSION_SECURE)) {
            sessionSecure = config.sessionSecure;
        }
    }

    private volatile String secret = null;
    protected T secret(String secret) {
        E.illegalArgumentIf(S.blank(secret));
        this.secret = secret;
        return me();
    }
    public String secret() {
        if (null == secret) {
            secret = get(AppConfigKey.SECRET);
            if (null == secret) {
                secret = "myawesomeapp";
                logger.warn("Application secret key not set! You are in the dangerous zone!!!");
            }
        }
        return secret;
    }
    private void _mergeSecret(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SECRET)) {
            secret = config.secret;
        }
    }

    private volatile SecureTicketCodec secureTicketCodec;
    private String secureTicketCodecClass;
    protected T secureTicketCodec(String secureTicketCodecClass) {
        this.secureTicketCodecClass = $.notNull(secureTicketCodecClass);
        return me();
    }
    protected T secureTicketCodec(SecureTicketCodec codec) {
        this.secureTicketCodec = $.notNull(codec);
        return me();
    }
    public SecureTicketCodec secureTicketCodec() {
        if (null != secureTicketCodec) {
            return secureTicketCodec;
        }
        synchronized (this) {
            if (null != secureTicketCodec) {
                return secureTicketCodec;
            }
            if (null == secureTicketCodecClass) {
                secureTicketCodecClass = get(SECURE_TICKET_CODEC);
                if (null == secureTicketCodecClass) {
                    secureTicketCodec = app().getInstance(DefaultSecureTicketCodec.class);
                    return secureTicketCodec;
                }
                if ("username".equalsIgnoreCase(secureTicketCodecClass)) {
                    secureTicketCodec = app().getInstance(UsernameSecureTicketCodec.class);
                    return secureTicketCodec;
                }
                secureTicketCodec = app().getInstance(secureTicketCodecClass);
            }
        }
        return secureTicketCodec;
    }
    private void _mergeSecureTicketCodec(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SECURE_TICKET_CODEC)) {
            secureTicketCodec = config.secureTicketCodec;
            secureTicketCodecClass = config.secureTicketCodecClass;
        }
    }
    private List<File> moduleBases;
    public List<File> moduleBases() {
        if (null == moduleBases) {
            String v = get(AppConfigKey.MODULES);
            moduleBases = processModules(v);
        }
        return moduleBases;
    }

    private List<File> processModules(String v) {
        if (S.blank(v)) {
            return C.list();
        } else {
            List<File> files = C.newList();
            File base = app.base();
            for (String s: v.trim().split("[;:]+")) {
                s = s.trim();
                File file;
                if (s.startsWith("/") || s.startsWith("\\")) {
                    file = new File(s);
                } else {
                    file = ProjectLayout.Utils.file(base, s);
                }
                if (!file.isDirectory()) {
                    logger.warn("Cannot locate extra source dir: %s", s);
                } else {
                    files.add(file);
                }
            }
            return C.list(files);
        }
    }

    private Boolean metricEnabled;

    protected T metricEnable(boolean enable) {
        this.metricEnabled = enable;
        return me();
    }

    public boolean metricEnabled() {
        if (null == metricEnabled) {
            metricEnabled = get(METRIC_ENABLED);
            if (null == metricEnabled) {
                metricEnabled = true;
            }
        }
        return metricEnabled;
    }

    private void _mergeMetricEnabled(AppConfig conf) {
        if (!hasConfiguration(METRIC_ENABLED)) {
            metricEnabled = conf.metricEnabled;
        }
    }

    public boolean possibleControllerClass(String className) {
        return appClassTester().test(className);
    }

    private CacheServiceProvider csp = null;

    protected T cacheService(CacheServiceProvider csp) {
        E.NPE(csp);
        this.csp = csp;
        return me();
    }

    protected T cacheService(Class<? extends CacheServiceProvider> csp) {
        this.csp = $.newInstance(csp);
        return me();
    }

    public CacheService cacheService(String name) {
        if (null == csp) {
            try {
                csp = get(AppConfigKey.CACHE_IMPL);
            } catch (ConfigurationException e) {
                Object obj = helper.getValFromAliases(raw, AppConfigKey.CACHE_IMPL.toString(), "impl", null);
                csp = CacheServiceProvider.Impl.valueOfIgnoreCase(obj.toString());
                if (null != csp) {
                    return csp.get(name);
                }
                throw e;
            }
            if (null == csp) {
                csp = CacheServiceProvider.Impl.Auto;
            }
        }
        return csp.get(name);
    }

    private void _mergeCacheServiceProvider(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.CACHE_IMPL)) {
            csp = config.csp;
        }
    }

    private String _cacheName;

    protected T cacheName(String name) {
        this._cacheName = name;
        return me();
    }

    public String cacheName() {
        if (null == _cacheName) {
            _cacheName = get(AppConfigKey.SESSION_KEY_USERNAME);
            if (null == _cacheName) {
                _cacheName = "_act_app_";
            }
        }
        return _cacheName;
    }

    private void _mergeCacheName(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.CACHE_NAME)) {
            _cacheName = config._cacheName;
        }
    }

    private String _cacheNameSession;

    protected T cacheNameSession(String name) {
        this._cacheNameSession = name;
        return me();
    }

    public String cacheNameSession() {
        if (null == _cacheNameSession) {
            _cacheNameSession = get(AppConfigKey.CACHE_NAME_SESSION);
            if (null == _cacheNameSession) {
                _cacheNameSession = cacheName();
            }
        }
        return _cacheNameSession;
    }

    private UnknownHttpMethodProcessor _unknownHttpMethodProcessor = null;

    protected T unknownHttpMethodProcessor(UnknownHttpMethodProcessor handler) {
        this._unknownHttpMethodProcessor = $.notNull(handler);
        return me();
    }

    public UnknownHttpMethodProcessor unknownHttpMethodProcessor() {
        if (null == _unknownHttpMethodProcessor) {
            _unknownHttpMethodProcessor = get(AppConfigKey.HANDLER_UNKNOWN_HTTP_METHOD);
            if (null == _unknownHttpMethodProcessor) {
                _unknownHttpMethodProcessor = UnknownHttpMethodProcessor.METHOD_NOT_ALLOWED;
            }
        }
        return _unknownHttpMethodProcessor;
    }

    private void _mergeUnknownHttpMethodHandler(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.HANDLER_UNKNOWN_HTTP_METHOD)) {
            this._unknownHttpMethodProcessor = config._unknownHttpMethodProcessor;
        }
    }

    private Integer resourcePreloadSizeLimit;
    protected T resourcePreloadSizeLimit(int limit) {
        resourcePreloadSizeLimit = limit;
        return me();
    }
    public int resourcePreloadSizeLimit() {
        if (null == resourcePreloadSizeLimit) {
            resourcePreloadSizeLimit = getInteger(RESOURCE_PRELOAD_SIZE_LIMIT);
            if (null == resourcePreloadSizeLimit) {
                resourcePreloadSizeLimit = 1024 * 10;
            } else if (resourcePreloadSizeLimit <= 0) {
                logger.warn("resource.preload.size.limit is set to zero or below, resource preload is disabled!");
            }
        }
        return resourcePreloadSizeLimit;
    }
    private void _mergeResourcePreloadSizeLimit(AppConfig conf) {
        if (!hasConfiguration(RESOURCE_PRELOAD_SIZE_LIMIT)) {
            this.resourcePreloadSizeLimit = conf.resourcePreloadSizeLimit;
        }
    }

    private Boolean uploadFileDownload;
    protected T enableUploadFileDownload(boolean b) {
        uploadFileDownload = b;
        return me();
    }
    public boolean allowDownloadUploadFile() {
        if (null == uploadFileDownload) {
            uploadFileDownload = get(UPLOAD_FILE_DOWNLOAD);
            if (null == uploadFileDownload) {
                uploadFileDownload = false;
            }
        }
        return uploadFileDownload;
    }
    private void _mergeUploadFileDownload(AppConfig config) {
        if (!hasConfiguration(UPLOAD_FILE_DOWNLOAD)) {
            uploadFileDownload = config.uploadFileDownload;
        }
    }

    private Boolean ssl;
    protected T supportSsl(boolean b) {
        ssl = b;
        return me();
    }
    public boolean supportSsl() {
        if (null == ssl) {
            ssl = get(SSL);
            if (null == ssl) {
                ssl = false;
            }
        }
        return ssl;
    }
    private void _mergeSslSupport(AppConfig config) {
        if (!hasConfiguration(SSL)) {
            ssl = config.ssl;
        }
    }

    private String wsTicketKey;

    protected T wsTicketeKey(String wsTicketKey) {
        this.wsTicketKey = wsTicketKey;
        return me();
    }

    public String wsTicketKey() {
        if (null == wsTicketKey) {
            wsTicketKey = get(WS_KEY_TICKET);
            if (null == wsTicketKey) {
                wsTicketKey = "ws_ticket";
            }
        }
        return wsTicketKey;
    }

    private void _mergeWsTicketKey(AppConfig config) {
        if (!hasConfiguration(WS_KEY_TICKET)) {
            wsTicketKey = config.wsTicketKey;
        }
    }

    private Set<AppConfigurator> mergeTracker = C.newSet();

    public void loadJarProperties(Map<String, Properties> jarProperties) {
        // load common properties
        Properties p0 = jarProperties.remove("common");
        // load app env properties
        Properties p1 = jarProperties.remove(ConfLoader.confSetName());
        if (null != p1) {
            if (null != p0) {
                p0.putAll(p1);
            } else {
                p0 = p1;
            }
        }
        if (null != p0) {
            loadJarProperties(p0);
        }
    }

    private void loadJarProperties(Properties p) {
        Enumeration<?> keys = p.propertyNames();
        while (keys.hasMoreElements()) {
            String key = S.string(keys.nextElement());
            if (!raw.containsKey(key)) {
                raw.put(key, p.getProperty(key));
            }
        }
    }


    /**
     * Merge application configurator settings. Note application configurator
     * settings has lower priority as it's hardcoded thus only when configuration file
     * does not provided the settings, the app configurator will take effect
     *
     * @param conf the application configurator
     */
    public void _merge(AppConfigurator conf) {
        app.emit(AppEventId.CONFIG_PREMERGE);
        if (mergeTracker.contains(conf)) {
            return;
        }
        mergeTracker.add(conf);
        _mergeBasicAuthentication(conf);
        _mergeCacheName(conf);
        _mergeCors(conf);
        _mergeCorsOrigin(conf);
        _mergeCorsHeaders(conf);
        _mergeCorsHeadersExpose(conf);
        _mergeCorsHeadersAllowed(conf);
        _mergeCorsMaxAge(conf);
        _mergeCorsAllowCredential(conf);
        _mergeCorsOptionCheck(conf);
        _mergeCliEnabled(conf);
        _mergeCliJSONPageSz(conf);
        _mergeCliTablePageSz(conf);
        _mergeCliOverHttp(conf);
        _mergeCliOverHttpAuthority(conf);
        _mergeCliOverHttpPort(conf);
        _mergeCliOverHttpTitle(conf);
        _mergeCliOverHttpSysCmd(conf);
        _mergeCliPort(conf);
        _mergeCliSessionExpiration(conf);
        _mergeCsrf(conf);
        _mergeCsrfParamName(conf);
        _mergeCsrfHeaderName(conf);
        _mergeCsrfCookieName(conf);
        _mergeCsrfProtector(conf);
        _mergeCsrfCheckFailureHandler(conf);
        _mergeAjaxCsrfCheckFailureHandler(conf);
        _mergeCookieDomain(conf);
        _mergeMaxCliSession(conf);
        _mergeDspToken(conf);
        _mergeEnumResolvingCaseSensitive(conf);
        _mergeXForwardedProtocol(conf);
        _mergeHost(conf);
        _mergeLoginUrl(conf);
        _mergeAjaxLoginUrl(conf);
        _mergeUrlContext(conf);
        _mergeHttpMaxParams(conf);
        _mergeJobPoolSize(conf);
        _mergeMissingAuthenticationHandler(conf);
        _mergeAjaxMissingAuthenticationHandler(conf);
        _mergeHttpExternal(conf);
        _mergeHttpExternalPort(conf);
        _mergeHttpExternalSecurePort(conf);
        _mergeHttpPort(conf);
        _mergeHttpSecure(conf);
        _mergeHttpsPort(conf);
        _mergePorts(conf);
        _mergeContentSuffixAware(conf);
        _mergeSequenceNumberGenerator(conf);
        _mergeErrorTemplatePathResolver(conf);
        _mergeDateFmt(conf);
        _mergeDateTimeFmt(conf);
        _mergeMetricEnabled(conf);
        _mergeTimeFmt(conf);
        _mergeEncoding(conf);
        _mergeNodeIdProvider(conf);
        _mergeI18nEnabled(conf);
        _mergeLocaleParamName(conf);
        _mergeLocaleCookieName(conf);
        _mergeIpEffectiveBytes(conf);
        _mergeStartIdFile(conf);
        _mergeStartIdProvider(conf);
        _mergeSequenceProvider(conf);
        _mergeLongEncoder(conf);
        _mergeLocale(conf);
        _mergeResourcePreloadSizeLimit(conf);
        _mergeSourceVersion(conf);
        _mergeTargetVersion(conf);
        _mergeTemplatePathResolver(conf);
        _mergeTemplateHome(conf);
        _mergeDefaultView(conf);
        _mergePingPath(conf);
        _mergeRenderJsonContentTypeIE(conf);
        _mergeRenderJsonOutputCharset(conf);
        _mergeServerHeader(conf);
        _mergeCookiePrefix(conf);
        _mergeSessionCookieName(conf);
        _mergeFlashCookieName(conf);
        _mergeSessionTtl(conf);
        _mergeSessionPersistent(conf);
        _mergeSessionEncrpt(conf);
        _mergeSessionSecure(conf);
        _mergeSessionKeyUsername(conf);
        _mergeSessionMapper(conf);
        _mergeSessionMapperHeaderPrefix(conf);
        _mergeSecret(conf);
        _mergeSecureTicketCodec(conf);
        _mergeCacheServiceProvider(conf);
        _mergeUnknownHttpMethodHandler(conf);
        _mergeUploadFileDownload(conf);
        _mergeSslSupport(conf);
        _mergeWsTicketKey(conf);

        Set<String> keys = conf.propKeys();
        if (!keys.isEmpty()) {
            for (String k : keys) {
                if (!raw.containsKey(k)) {
                    raw.put(k, conf.propVal(k));
                }
            }
        }
    }

    @Override
    protected void releaseResources() {
        app = null;
        super.releaseResources();
    }

    protected T me() {
        return $.cast(this);
    }

    private String cookieName(String suffix) {
        return S.concat(cookiePrefix(), suffix);
    }
}
