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

import static act.conf.AppConfigKey.*;
import static java.util.ResourceBundle.Control.FORMAT_DEFAULT;
import static org.osgl.http.H.Header.Names.X_XSRF_TOKEN;

import act.Act;
import act.Constants;
import act.act_messages;
import act.app.*;
import act.app.conf.AppConfigurator;
import act.app.event.AppClassLoaderInitialized;
import act.app.event.SysEventId;
import act.app.util.NamedPort;
import act.cli.CliOverHttpAuthority;
import act.crypto.HMAC;
import act.crypto.RotateSecretHMAC;
import act.data.DateTimeStyle;
import act.data.DateTimeType;
import act.db.util.SequenceNumberGenerator;
import act.db.util._SequenceNumberGenerator;
import act.event.EventBus;
import act.event.SysEventListenerBase;
import act.handler.*;
import act.handler.event.ResultEvent;
import act.i18n.I18n;
import act.internal.util.StrBufRetentionLimitCalculator;
import act.route.Router;
import act.security.CSRFProtector;
import act.session.*;
import act.util.*;
import act.validation.Password;
import act.validation.PasswordSpec;
import act.view.TemplatePathResolver;
import act.view.View;
import act.ws.DefaultSecureTicketCodec;
import act.ws.SecureTicketCodec;
import act.ws.UsernameSecureTicketCodec;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import me.tongfei.progressbar.ProgressBarStyle;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.osgl.*;
import org.osgl.cache.CacheService;
import org.osgl.cache.CacheServiceProvider;
import org.osgl.exception.ConfigurationException;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.mvc.MvcConfig;
import org.osgl.util.*;
import org.osgl.util.converter.TypeConverterRegistry;
import org.osgl.web.util.UserAgent;
import org.rythmengine.utils.Time;
import osgl.version.Version;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import javax.inject.Provider;

public class AppConfig<T extends AppConfig> extends Config<AppConfigKey> implements AppHolder<AppConfig<T>> {

    public static final String CONF_FILE_NAME = "app.conf";
    public static final String CSRF_TOKEN_NAME = "__csrf__";
    public static final String PORT_CLI_OVER_HTTP = Router.PORT_CLI_OVER_HTTP;
    public static final String PORT_ADMIN = Router.PORT_ADMIN;
    public static final String PORT_SYS = Router.PORT_SYS;

    private App app;

    static {
        MvcConfig.registerAlarmListener(MvcConfig.ALARM_BIG_CONTENT_ENCOUNTERED, new $.Func0() {
            @Override
            public Object apply() throws NotAppliedException, $.Break {
                ActionContext ctx = ActionContext.current();
                if (null != ctx) {
                    ctx.setLargeResponseHint();
                }
                return null;
            }
        });
        TypeConverterRegistry.INSTANCE.register(new Lang.TypeConverter<String, Class>() {
            @Override
            public Class convert(String s) {
                return Act.appClassForName(s);
            }
        });
        MvcConfig.errorPageRenderer(new ActErrorPageRender());
        MvcConfig.beforeCommitResultHandler(ResultEvent.BEFORE_COMMIT_HANDLER);
        MvcConfig.afterCommitResultHandler(ResultEvent.AFTER_COMMIT_HANDLER);
        MvcConfig.messageTranslater(new $.Transformer<String, String>() {
            @Override
            public String transform(String message) {
                String translated = I18n.i18n(message);
                if (message == translated) {
                    translated = I18n.i18n(MvcConfig.class, message);
                }
                if (message == translated) {
                    translated = I18n.i18n(act_messages.class, message);
                }
                return translated;
            }
        });
        // Refer https://cloud.tencent.com/announce/detail/1112
        ParserConfig.getGlobalInstance().setSafeMode(true);
    }

    private RouterRegexMacroLookup routerRegexMacroLookup;

    /**
     * Construct a <code>AppConfig</code> with a map. The map is copied to
     * the original map of the configuration instance
     *
     * @param configuration
     */
    public AppConfig(Map<String, ?> configuration) {
        super(configuration);
        loadFromConfServer();
        raw.putAll(extendedConfigurations());
    }

    // for unit test
    public AppConfig() {
        this((Map) System.getProperties());
        this.routerRegexMacroLookup = new RouterRegexMacroLookup(this);
    }

    private void loadFromConfServer() {
        String confServerEndpoint = get(CONF_SERVER_ENDPOINT, "");
        if (S.blank(confServerEndpoint)) {
            return;
        }
        E.invalidConfigurationIf(!confServerEndpoint.startsWith("http"), "conf-server.endpoint must be full URL, found: %s", confServerEndpoint);
        String privateKey = confPrivateKey();
        E.invalidConfigurationIf(S.blank(privateKey), "conf.private-key not configured for conf-server: " + confServerEndpoint);
        String confId = confId();
        E.invalidConfigurationIf(S.blank(confId), "conf.id not configured correctly for conf-server: " + confServerEndpoint);
        OkHttpClient http = new OkHttpClient.Builder().build();
        String url = S.concat(confServerEndpoint, "?id=", confId);
        Request req = new Request.Builder().url(url).get().addHeader("Accept", "application/json").build();
        try {
            Response resp = http.newCall(req).execute();
            if (!resp.isSuccessful()) {
                warn("Error fetching configuration from conf-server. Response code: %s; Respond body: %s", resp.code(), resp.body().string());
            } else {
                String data = Crypto.decryptRSA(resp.body().string(), privateKey);
                JSONObject json = JSON.parseObject(data);
                raw.putAll(json);
            }
        } catch (IOException e) {
            warn(e, "Error fetching configuration from conf-server: " + confServerEndpoint);
        }
    }

    public AppConfig<T> app(App app) {
        E.NPE(app);
        this.app = app;
        AppConfigKey.onApp(app);
        EventBus eventBus = app.eventBus();
        if (null != eventBus) {
            eventBus.bind(SysEventId.CLASS_LOADER_INITIALIZED, new SysEventListenerBase<AppClassLoaderInitialized>() {
                @Override
                public void on(AppClassLoaderInitialized event) throws Exception {
                    routerRegexMacroLookup = new RouterRegexMacroLookup(AppConfig.this);
                }
            });
        } // else - must be in routing benchmark unit test
        return this;
    }

    public void preloadConfigurations() {
        long ms = $.ms();
        // ensure JWT get evaluated first to set
        // default value for dependency settings
        jwtEnabled();

        for (Method method : AppConfig.class.getDeclaredMethods()) {
            boolean isPublic = Modifier.isPublic(method.getModifiers());
            if (!isPublic || method.isAnnotationPresent(Lazy.class)) {
                continue;
            }
            if (0 == method.getParameterTypes().length && !"preloadConfigurations".equals(method.getName())) {
                $.invokeVirtual(this, method);
            }
        }

        MvcConfig.renderJsonOutputCharset(renderJsonOutputCharset());
        $.Func0<H.Format> jsonContentProvider = jsonContentTypeProvider();
        if (null != jsonContentTypeProvider) {
            MvcConfig.jsonMediaTypeProvider(jsonContentProvider);
        }

        OsglConfig.setXmlRootTag(xmlRootTag());

        OsglConfig.setThreadLocalBufferLimit(threadLocalBufRetentionLimit());
        OsglConfig.registerGlobalInstanceFactory(new $.Function<Class, Object>() {
            final App app = Act.app();

            @Override
            public Object apply(Class aClass) throws NotAppliedException, $.Break {
                return app.getInstance(aClass);
            }
        });
        OsglConfig.setSingletonChecker(new $.Predicate() {
            @Override
            public boolean test(Object o) {
                return app.isSingleton(o);
            }
        });

        app().cache().setDefaultTTL(cacheTtl());
        Map<String, Object> cacheSettings = subSet("cache");
        for (Map.Entry<String, Object> entry : cacheSettings.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith(".ttl")) {
                String s = S.string(entry.getValue());
                int ttl;
                if (S.isInt(s)) {
                    ttl = Integer.parseInt(s);
                } else if (s.contains("*")) {
                    List<String> sl = S.fastSplit(s, "*");
                    int n = 1;
                    for (String sn : sl) {
                        n *= Integer.parseInt(sn.trim());
                    }
                    ttl = n;
                } else {
                    throw new ConfigurationException("Invalid cache ttl configuration: " + key);
                }
                key = S.cut(key.substring(6)).beforeLast(".");
                app().cache(key).setDefaultTTL(ttl);
            }
        }
        logger.trace("Preload Config takes: %sms", $.ms() - ms);
    }


    public App app() {
        return app;
    }

    public RouterRegexMacroLookup routerRegexMacroLookup() {
        return routerRegexMacroLookup;
    }

    @Override
    protected ConfigKey keyOf(String s) {
        return AppConfigKey.valueOfIgnoreCase(s);
    }

    private Boolean apiDoc;

    protected T enableApiDoc(boolean b) {
        this.apiDoc = b;
        return me();
    }

    public boolean apiDocEnabled() {
        if (null == apiDoc) {
            this.apiDoc = get(API_DOC_ENABLED, Act.isDev());
        }
        return this.apiDoc;
    }

    private void _mergeApiDocEnabled(AppConfig conf) {
        if (!hasConfiguration(API_DOC_ENABLED)) {
            this.apiDoc = conf.apiDoc;
        }
    }

    private Boolean apiDocBuiltInHide;

    protected T hideBuiltInEndpointsInApiDoc(boolean b) {
        this.apiDocBuiltInHide = b;
        return me();
    }

    public boolean isHideBuiltInEndpointsInApiDoc() {
        if (null == apiDocBuiltInHide) {
            apiDocBuiltInHide = get(API_DOC_HIDE_BUILT_IN_ENDPOINTS, true);
        }
        return apiDocBuiltInHide;
    }

    private void _mergeHideBuiltInEndpointsInApiDoc(AppConfig conf) {
        if (!hasConfiguration(API_DOC_HIDE_BUILT_IN_ENDPOINTS)) {
            this.apiDocBuiltInHide = conf.apiDocBuiltInHide;
        }
    }

    private String appName;

    protected T appName(String name) {
        this.appName = S.requireNotBlank(name);
        return me();
    }

    public String appName() {
        if (S.blank(appName)) {
            // - `app` not initialized yet: appName = get(APP_NAME, app.name());
            appName = get(APP_NAME, Act.app().name());
        }
        return appName;
    }

    private void _mergeAppName(AppConfig conf) {
        if (!hasConfiguration(APP_NAME)) {
            this.appName = conf.appName;
        }
    }

    private Boolean basicAuth;

    protected T enableBasicAuthentication(boolean b) {
        this.basicAuth = b;
        return me();
    }

    public boolean basicAuthenticationEnabled() {
        if (null == basicAuth) {
            this.basicAuth = get(BASIC_AUTHENTICATION, Act.isDev());
        }
        return this.basicAuth;
    }

    private void _mergeBasicAuthentication(AppConfig conf) {
        if (!hasConfiguration(BASIC_AUTHENTICATION)) {
            this.basicAuth = conf.basicAuth;
        }
    }

    private Boolean builtInReqHandlerEnabled;

    protected T disableBuiltInReqHandler() {
        builtInReqHandlerEnabled = false;
        return me();
    }

    public boolean builtInReqHandlerEnabled() {
        if (null == builtInReqHandlerEnabled) {
            builtInReqHandlerEnabled = get(BUILT_IN_REQ_HANDLER_ENABLED, true);
        }
        return builtInReqHandlerEnabled;
    }

    private void _mergeBuiltInReqHandler(AppConfig conf) {
        if (!hasConfiguration(BUILT_IN_REQ_HANDLER_ENABLED)) {
            this.builtInReqHandlerEnabled = conf.builtInReqHandlerEnabled;
        }
    }

    private Boolean cacheForOnDev;

    protected T enableCacheForOnDevMode() {
        cacheForOnDev = false;
        return me();
    }

    public boolean cacheForOnDevMode() {
        if (null == cacheForOnDev) {
            cacheForOnDev = get(CACHE_FOR_ON_DEV, false);
        }
        return cacheForOnDev;
    }

    private void _mergeCacheForOnDev(AppConfig config) {
        if (!hasConfiguration(CACHE_FOR_ON_DEV)) {
            cacheForOnDev = config.cacheForOnDev;
        }
    }

    private Integer captchaWidth;

    protected T captchaWidth(int w) {
        this.captchaWidth = w;
        return me();
    }

    public int captchaWidth() {
        if (null == captchaWidth) {
            captchaWidth = getInteger(CAPTCHA_WIDTH, 200);
        }
        return captchaWidth;
    }

    private void _mergeCaptchaWidth(AppConfig config) {
        if (!hasConfiguration(CAPTCHA_WIDTH)) {
            captchaWidth = config.captchaWidth;
        }
    }

    private Integer captchaHeight;

    protected T captchaHeight(int h) {
        this.captchaHeight = h;
        return me();
    }

    public int captchaHeight() {
        if (null == captchaHeight) {
            captchaHeight = getInteger(CAPTCHA_HEIGHT, 70);
        }
        return captchaHeight;
    }

    private void _mergeCaptchaHeight(AppConfig config) {
        if (!hasConfiguration(CAPTCHA_HEIGHT)) {
            captchaHeight = config.captchaHeight;
        }
    }

    private Color captchaBgColor;

    protected T captchaBgColor(Color color) {
        this.captchaBgColor = color;
        return me();
    }

    public Color captchaBgColor() {
        if (null == captchaBgColor) {
            String s = get(CAPTCHA_BG_COLOR, "white");
            Field f = $.fieldOf(Color.class, s, false);
            if (null == f) {
                warn("Invalid captcha.background.color found: " + s);
                info("will use white as captcha.background.color");
                captchaBgColor = Color.WHITE;
            } else {
                captchaBgColor = $.getFieldValue(null, f);
            }
        }
        return captchaBgColor;
    }

    private void _mergeCaptchaBgColor(AppConfig config) {
        if (!hasConfiguration(CAPTCHA_BG_COLOR)) {
            captchaBgColor = config.captchaBgColor;
        }
    }

    private String reCaptchaSecret;

    protected T reCaptchaSecret(String secret) {
        this.reCaptchaSecret = secret;
        return me();
    }

    public String reCaptchaSecret() {
        if (null != reCaptchaSecret) {
            reCaptchaSecret = get(CAPTCHA_RECAPTCHA_SECRET, "");
        }
        return reCaptchaSecret;
    }

    public boolean reCaptchaActivated() {
        return S.notBlank(reCaptchaSecret());
    }

    private void _mergeReCaptchaSecret(AppConfig config) {
        if (!hasConfiguration(CAPTCHA_RECAPTCHA_SECRET)) {
            reCaptchaSecret = config.reCaptchaSecret;
        }
    }

    private Map<String, Object> extendedConfigurations;

    protected T extendedConfigurations(Map<String, Object> conf) {
        this.extendedConfigurations = conf;
        return me();
    }

    private Map<String, Object> extendedConfigurations() {
        if (null == extendedConfigurations) {
            try {
                Object confLoader = get(CONF_LOADER, new ExtendedAppConfLoader.DumbLoader());
                this.extendedConfigurations = $.invokeVirtual(confLoader, "loadConfigurations");
            } catch (Exception e) {
                warn(e, "Error loading extended configurations");
                this.extendedConfigurations = C.Map();
            }
        }
        return extendedConfigurations;
    }

    private void _mergeExtendedAppConfLoader(AppConfig conf) {
        if (!hasConfiguration(CONF_LOADER)) {
            extendedConfigurations = conf.extendedConfigurations;
        }
    }

    private String confId;

    protected T confId(String key) {
        this.confId = S.requireNotBlank(key);
        return me();
    }

    public String confId() {
        if (S.blank(confId)) {
            confId = get(CONF_ID, S.concat(appName(), "-", Act.profile()));
        }
        return confId;
    }

    private void _mergeConfId(AppConfig conf) {
        if (!hasConfiguration(CONF_ID)) {
            this.confId = conf.confId;
        }
    }

    private String confPrivateKey;

    protected T confPrivateKey(String key) {
        this.confPrivateKey = key;
        return me();
    }

    private String confPrivateKey() {
        if (S.blank(confPrivateKey)) {
            confPrivateKey = get(CONF_PRIVATE_KEY, "");
        }
        return confPrivateKey;
    }

    private void _mergeConfPrivateId(AppConfig conf) {
        if (!hasConfiguration(CONF_PRIVATE_KEY)) {
            confPrivateKey = conf.confPrivateKey;
        }
    }

    private Boolean cors;

    protected T enableCors(boolean b) {
        this.cors = b;
        return me();
    }

    public boolean corsEnabled() {
        if (null == cors) {
            this.cors = get(CORS, false);
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
            corsOrigin = get(CORS_ORIGIN, "*");
        }
        return corsOrigin;
    }

    private void _mergeCorsOrigin(AppConfig conf) {
        if (!hasConfiguration(CORS_ORIGIN)) {
            corsOrigin = conf.corsOrigin;
        }
    }

    private String corsHeaders;

    @Deprecated
    protected T corsHeaders(String s) {
        this.corsHeaders = s;
        return me();
    }

    private String corsHeaders() {
        if (null == corsHeaders) {
            corsHeaders = get(CORS_HEADERS, "");
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
            corsHeadersExpose = get(CORS_HEADERS_EXPOSE, "");
            if (S.blank(corsHeadersExpose)) {
                corsHeadersExpose = corsHeaders();
                if (S.notBlank(corsHeadersExpose)) {
                    warn("`cors.headers` is deprecated. Please use `cors.headers.expose` instead");
                } else {
                    corsHeadersExpose = "Act-Session-Expires, Authorization, X-XSRF-Token, X-CSRF-Token, Location, Link, Content-Disposition, Content-Length";
                }
            }
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
            corsOptionCheck = get(CORS_CHECK_OPTION_METHOD, true);
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
        this.corsHeadersAllowed = s;
        return me();
    }

    public String corsAllowHeaders() {
        if (null == corsHeadersAllowed) {
            corsHeadersAllowed = get(CORS_HEADERS_ALLOWED, "");
            if (S.isBlank(corsHeadersAllowed)) {
                corsHeadersAllowed = corsHeaders();
                if (S.notBlank(corsHeadersAllowed)) {
                    warn("`cors.headers` is deprecated. Please use `cors.headers.allowed` instead");
                } else {
                    corsHeadersAllowed = "X-HTTP-Method-Override, X-Requested-With, Authorization, X-XSRF-Token, X-CSRF-Token";
                }
            }
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
            corsMaxAge = getInteger(CORS_MAX_AGE, 30 * 60);
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
            corsAllowCredentials = get(CORS_ALLOW_CREDENTIALS, false);
        }
        return corsAllowCredentials;
    }

    private void _mergeCorsAllowCredential(AppConfig conf) {
        if (!hasConfiguration(CORS_ALLOW_CREDENTIALS)) {
            corsAllowCredentials = conf.corsAllowCredentials;
        }
    }

    private String contentSecurityPolicy;
    private boolean cspSet;

    protected T contentSecurityPolicy(String policy) {
        this.contentSecurityPolicy = policy;
        cspSet = null != policy;
        return me();
    }

    public String contentSecurityPolicy() {
        if (!cspSet) {
            contentSecurityPolicy = get(CONTENT_SECURITY_POLICY, null);
            cspSet = true;
        }
        return contentSecurityPolicy;
    }

    private void _mergeCsp(AppConfig conf) {
        if (!hasConfiguration(CONTENT_SECURITY_POLICY)) {
            contentSecurityPolicy = conf.contentSecurityPolicy;
            cspSet = null != contentSecurityPolicy;
        }
    }

    private Boolean csrf;

    protected T enableCsrf(boolean b) {
        this.csrf = b;
        return me();
    }

    public boolean csrfEnabled() {
        if (null == csrf) {
            this.csrf = get(CSRF, false);
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
            csrfParamName = get(CSRF_PARAM_NAME, CSRF_TOKEN_NAME);
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
        this.csrfProtector = $.requireNotNull(protector);
        return me();
    }

    public CSRFProtector csrfProtector() {
        if (null == csrfProtector) {
            try {
                csrfProtector = get(CSRF_PROTECTOR, CSRFProtector.Predefined.HMAC);
            } catch (ConfigurationException e) {
                Object obj = helper.getValFromAliases(raw, CSRF_PROTECTOR.key(), "impl", null);
                if (null != obj) {
                    this.csrfProtector = CSRFProtector.Predefined.valueOfIgnoreCase(obj.toString());
                    if (null != csrfProtector) {
                        set(CSRF_PROTECTOR, csrfProtector);
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
            csrfCookieName = get(CSRF_COOKIE_NAME, "XSRF-TOKEN");
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
            csrfHeaderName = get(CSRF_HEADER_NAME, X_XSRF_TOKEN);
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
            cliEnabled = get(CLI_ENABLED, true);
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
            cliTablePageSz = getInteger(CLI_PAGE_SIZE_TABLE, 22);
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
            cliJSONPageSz = getInteger(CLI_PAGE_SIZE_TABLE, 22);
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
            cliOverHttp = get(CLI_OVER_HTTP, true);
        }
        return cliOverHttp;
    }

    private void _mergeCliOverHttp(AppConfig config) {
        if (!hasConfiguration(CLI_OVER_HTTP)) {
            cliOverHttp = config.cliOverHttp;
        }
    }

    private ProgressBarStyle cliProgressBarStyle;

    protected T cliProgressBarStyle(ProgressBarStyle style) {
        this.cliProgressBarStyle = style;
        return me();
    }

    public ProgressBarStyle cliProgressBarStyle() {
        if (null == cliProgressBarStyle) {
            String s = get(CLI_PROGRESS_BAR_STYLE, "unicode");
            cliProgressBarStyle = S.eq("ascii", s) ? ProgressBarStyle.ASCII : ProgressBarStyle.UNICODE_BLOCK;
        }
        return cliProgressBarStyle;
    }

    private void _mergeCliProgressBarStyle(AppConfig config) {
        if (!hasConfiguration(CLI_PROGRESS_BAR_STYLE)) {
            cliProgressBarStyle = config.cliProgressBarStyle;
        }
    }

    private CliOverHttpAuthority cliOverHttpAuthority;

    protected T cliOverHttpAuthority(CliOverHttpAuthority authority) {
        this.cliOverHttpAuthority = authority;
        return me();
    }

    public CliOverHttpAuthority cliOverHttpAuthority() {
        if (null == cliOverHttpAuthority) {
            cliOverHttpAuthority = get(CLI_OVER_HTTP_AUTHORITY, new CliOverHttpAuthority.AllowAll());
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
            cliOverHttpPort = get(CLI_OVER_HTTP_PORT, httpPort() + 2);
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
            cliOverHttpTitle = get(CLI_OVER_HTTP_TITLE, "Cli Over Http");
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
            cliOverHttpSysCmd = get(CLI_OVER_HTTP_SYS_CMD, true);
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
            cliPort = get(CLI_PORT, httpPort() + 1);
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
            cliSessionExpiration = get(CLI_SESSION_TTL, 300);
        }
        return cliSessionExpiration;
    }

    private void _mergeCliSessionExpiration(AppConfig conf) {
        if (!hasConfiguration(CLI_SESSION_TTL)) {
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
            dspToken = get(DOUBLE_SUBMISSION_PROTECT_TOKEN, "act_dsp_token");
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
        this.cookieDomainProvider = $.requireNotNull(provider);
        return me();
    }

    public String cookieDomain() {
        String domain = cookieDomainProvider().get();
        return "localhost".equals(domain) ? null : domain;
    }

    private Provider<String> cookieDomainProvider() {
        if (null == cookieDomainProvider) {
            try {
                cookieDomainProvider = get(COOKIE_DOMAIN_PROVIDER, new Provider<String>() {
                    @Override
                    public String get() {
                        return host();
                    }
                });
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
            maxCliSession = get(CLI_SESSION_MAX, 3);
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

    @Deprecated
    public boolean enumResolvingCaseSensitive() {
        synchronized (ENUM_RESOLVING_CASE_SENSITIVE) {
            if (null == enumResolvingCaseSensitive) {
                enumResolvingCaseSensitive = get(ENUM_RESOLVING_CASE_SENSITIVE, false);
            }
            return enumResolvingCaseSensitive;
        }
    }

    private void _mergeEnumResolvingCaseSensitive(AppConfig conf) {
        if (!hasConfiguration(ENUM_RESOLVING_CASE_SENSITIVE)) {
            enumResolvingCaseSensitive = conf.enumResolvingCaseSensitive;
        }
    }

    private Boolean enumResolvingExactMatch;

    protected T enumResolvingExactMatch(boolean b) {
        enumResolvingExactMatch = b;
        return me();
    }

    public boolean enumResolvingExactMatch() {
        synchronized (ENUM_RESOLVING_EXACT_MATCH) {
            if (null == enumResolvingExactMatch) {
                enumResolvingExactMatch = get(ENUM_RESOLVING_EXACT_MATCH, enumResolvingCaseSensitive());
            }
            return enumResolvingExactMatch;
        }
    }

    private void _mergeEnumResolvingExactMatch(AppConfig config) {
        if (!hasConfiguration(ENUM_RESOLVING_EXACT_MATCH)) {
            enumResolvingExactMatch = config.enumResolvingExactMatch;
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
            defViewName = get(AppConfigKey.VIEW_DEFAULT, "rythm");
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
            xForwardedProtocol = get(X_FORWARD_PROTOCOL, "http");
        }
        return xForwardedProtocol;
    }

    private void _mergeXForwardedProtocol(AppConfig conf) {
        if (!hasConfiguration(X_FORWARD_PROTOCOL)) {
            xForwardedProtocol = conf.xForwardedProtocol;
        }
    }

    private String xmlRootTag;

    protected T xmlRootTag(String tag) {
        this.xmlRootTag = tag;
        return me();
    }

    public String xmlRootTag() {
        if (null == xmlRootTag) {
            xmlRootTag = get(XML_ROOT, "xml");
        }
        return xmlRootTag;
    }

    private void _mergeXmlRootTag(AppConfig conf) {
        if (!hasConfiguration(XML_ROOT)) {
            this.xmlRootTag = conf.xmlRootTag;
        }
    }

    private ReturnValueAdvice globalReturnValueAdvice;
    private Boolean globalReturnValueAdviceSet;

    protected T globalReturnValueAdvice(ReturnValueAdvice advice) {
        this.globalReturnValueAdvice = $.requireNotNull(advice);
        this.globalReturnValueAdviceSet = true;
        return me();
    }

    public ReturnValueAdvice globalReturnValueAdvice() {
        if (null != globalReturnValueAdviceSet) {
            return globalReturnValueAdvice;
        }
        String s = get(GLOBAL_RETURN_VALUE_ADVICE, null);
        if (null != s) {
            try {
                globalReturnValueAdvice = app.getInstance(s);
            } catch (Exception e) {
                throw new ConfigurationException("Error loading global returnValueAdvice: " + s);
            }
        }
        globalReturnValueAdviceSet = true;
        return globalReturnValueAdvice;
    }

    private void _mergeGlobalReturnValueAdvice(AppConfig conf) {
        if (!hasConfiguration(GLOBAL_RETURN_VALUE_ADVICE)) {
            globalReturnValueAdvice = conf.globalReturnValueAdvice;
            globalReturnValueAdviceSet = conf.globalReturnValueAdviceSet;
        }
    }

    private ValidateViolationAdvice globalValidateViolationAdvice;
    private Boolean globalValidateViolationAdviceSet;

    protected T globalValidateViolationAdvice(ValidateViolationAdvice advice) {
        this.globalValidateViolationAdvice = $.requireNotNull(advice);
        this.globalValidateViolationAdviceSet = true;
        return me();
    }

    public ValidateViolationAdvice globalValidateViolationAdvice() {
        if (null != globalValidateViolationAdviceSet) {
            return globalValidateViolationAdvice;
        }
        String s = get(GLOBAL_VALIDATE_VIOLATION_ADVICE, null);
        if (null != s) {
            try {
                globalValidateViolationAdvice = app.getInstance(s);
            } catch (Exception e) {
                throw new ConfigurationException("Error loading global returnValueAdvice: " + s);
            }
        }
        globalValidateViolationAdviceSet = true;
        return globalValidateViolationAdvice;
    }

    private void _mergeGlobalValidateViolationAdvice(AppConfig conf) {
        if (!hasConfiguration(GLOBAL_VALIDATE_VIOLATION_ADVICE)) {
            globalValidateViolationAdvice = conf.globalValidateViolationAdvice;
            globalValidateViolationAdviceSet = conf.globalValidateViolationAdviceSet;
        }
    }


    private Boolean contentSuffixAware = null;

    protected T contentSuffixAware(boolean b) {
        contentSuffixAware = b;
        return me();
    }

    public Boolean contentSuffixAware() {
        if (null == contentSuffixAware) {
            contentSuffixAware = get(CONTENT_SUFFIX_AWARE, false);
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
            javax.inject.Provider<_SequenceNumberGenerator> provider = app().getInstance(SequenceNumberGenerator.Provider.class);
            seqGen = get(DB_SEQ_GENERATOR, provider.get());
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
            errorTemplatePathResolver = get(RESOLVER_ERROR_TEMPLATE_PATH, new ErrorTemplatePathResolver.DefaultErrorTemplatePathResolver());
        }
        return errorTemplatePathResolver;
    }

    private void _mergeErrorTemplatePathResolver(AppConfig conf) {
        if (!hasConfiguration(RESOLVER_ERROR_TEMPLATE_PATH)) {
            errorTemplatePathResolver = conf.errorTemplatePathResolver;
        }
    }

    private Boolean headerOverwrite;

    protected T allowHeaderOverwrite(boolean b) {
        headerOverwrite = b;
        return me();
    }

    public boolean allowHeaderOverwrite() {
        if (null == headerOverwrite) {
            headerOverwrite = get(HEADER_OVERWRITE, false);
        }
        return headerOverwrite;
    }

    private void _mergeHeaderOverwrite(AppConfig config) {
        if (!hasConfiguration(HEADER_OVERWRITE)) {
            headerOverwrite = config.headerOverwrite;
        }
    }


    private String headerSessionExpiration;

    protected T headerSessionExpiration(String headerName) {
        headerSessionExpiration = headerName.trim();
        return me();
    }

    public String headerSessionExpiration() {
        if (null == headerSessionExpiration) {
            headerSessionExpiration = get(AppConfigKey.HEADER_SESSION_EXPIRATION, "Act-Session-Expires");
        }
        return headerSessionExpiration;
    }

    private void _mergeHeaderSessionExpiration(AppConfig config) {
        if (!hasConfiguration(HEADER_SESSION_EXPIRATION)) {
            headerSessionExpiration = config.headerSessionExpiration;
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
            host = get(HOST, "localhost");
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
            i18nEnabled = get(I18N, false);
        }
        return i18nEnabled;
    }

    private void _mergeI18nEnabled(AppConfig conf) {
        if (!hasConfiguration(I18N)) {
            i18nEnabled = conf.i18nEnabled;
        }
    }

    private Boolean jwtEnabled;

    protected T jwtEnabled(boolean enabled) {
        jwtEnabled = enabled;
        return me();
    }

    public boolean jwtEnabled() {
        if (null == jwtEnabled) {
            jwtEnabled = get(JWT, false);
            if (jwtEnabled) {
                if (!hasConfiguration(SESSION_HEADER)) {
                    sessionHeader("Authorization");
                }
                if (!hasConfiguration(SESSION_HEADER_PAYLOAD_PREFIX)) {
                    sessionHeaderPayloadPrefix("Bearer ");
                }
                if (!hasConfiguration(SESSION_MAPPER)) {
                    sessionMapper(new HeaderTokenSessionMapper(this));
                }
                if (!hasConfiguration(SESSION_CODEC)) {
                    sessionCodec(app().getInstance(JsonWebTokenSessionCodec.class));
                }
            }
        }
        return jwtEnabled;
    }

    private void _mergeJWT(AppConfig config) {
        if (!hasConfiguration(JWT)) {
            jwtEnabled = config.jwtEnabled;
        }
    }

    private HMAC jwtAlgo;

    protected T jwtArgo(HMAC.Algorithm algo) {
        jwtAlgo = new HMAC(secret(), algo);
        return me();
    }

    public HMAC jwtAlgo() {
        if (null == jwtAlgo) {
            String algoKey = get(JWT_ALGO, "SHA256");
            if (rotateSecret()) {
                jwtAlgo = new RotateSecretHMAC(algoKey, app.getInstance(RotationSecretProvider.class));
            } else {
                jwtAlgo = new HMAC(secret(), algoKey);
            }
        }
        return jwtAlgo;
    }

    private void _mergeJWTAlgo(AppConfig config) {
        if (!hasConfiguration(JWT_ALGO)) {
            jwtAlgo = config.jwtAlgo;
        }
    }

    private String jwtIssuer;

    protected T jwtIssuer(String issuer) {
        E.illegalArgumentIf(S.blank(issuer), "issuer cannot be empty");
        jwtIssuer = issuer;
        return me();
    }

    public String jwtIssuer() {
        if (null == jwtIssuer) {
            jwtIssuer = get(AppConfigKey.JWT_ISSUER, cookiePrefix().substring(0, cookiePrefix().length() - 1));
        }
        return jwtIssuer;
    }

    private void _mergeJwtIssuer(AppConfig config) {
        if (!hasConfiguration(JWT_ISSUER)) {
            jwtIssuer = config.jwtIssuer;
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
            localeParamName = get(I18N_LOCALE_PARAM_NAME, "act_locale");
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
            localeCookieName = get(I18N_LOCALE_COOKIE_NAME, "locale");
        }
        return localeCookieName;
    }

    private void _mergeLocaleCookieName(AppConfig conf) {
        if (!hasConfiguration(I18N_LOCALE_COOKIE_NAME)) {
            localeCookieName = conf.localeCookieName;
        }
    }

    private Boolean mockServer;

    protected T mockServer(boolean enabled) {
        mockServer = enabled;
        return me();
    }

    public boolean mockServer() {
        if (null == mockServer) {
            mockServer = get(MOCK_SERVER_ENABLED, app.isDev());
        }
        return mockServer;
    }

    private void _mergeMockServer(AppConfig config) {
        if (!hasConfiguration(MOCK_SERVER_ENABLED)) {
            mockServer = config.mockServer;
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
            ipEffectiveBytes = getInteger(ID_GEN_NODE_ID_EFFECTIVE_IP_BYTES, 4);
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
            nodeIdProvider = get(ID_GEN_NODE_ID_PROVIDER, new IdGenerator.NodeIdProvider.IpProvider(ipEffectiveBytes()));
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
            startIdFile = get(ID_GEN_START_ID_FILE, ".act.id-app");
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
            startIdProvider = get(ID_GEN_START_ID_PROVIDER, new IdGenerator.StartIdProvider.DefaultStartIdProvider(startIdFile()));
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
            sequenceProvider = get(ID_GEN_SEQ_ID_PROVIDER, new IdGenerator.SequenceProvider.AtomicLongSeq());
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
            longEncoder = get(ID_GEN_LONG_ENCODER, IdGenerator.SAFE_ENCODER);
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
        ActionContext context = ActionContext.current();
        if (null != context && context.isAjax()) {
            return ajaxLoginUrl();
        }
        return loginUrl0();
    }

    private String loginUrl0() {
        if (null == loginUrl) {
            loginUrl = get(URL_LOGIN, "/login");
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
            ajaxLoginUrl = get(URL_LOGIN_AJAX, loginUrl0());
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
            urlContext = get(URL_CONTEXT, null);
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
            httpMaxParams = getInteger(HTTP_MAX_PARAMS, 128);
            if (httpMaxParams < 0) {
                throw new ConfigurationException("http.params.max setting cannot be negative number. Found: %s", httpMaxParams);
            }
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
            jobPoolSize = getInteger(JOB_POOL_SIZE, 10);
        }
        return jobPoolSize;
    }

    private void _mergeJobPoolSize(AppConfig conf) {
        if (!hasConfiguration(JOB_POOL_SIZE)) {
            jobPoolSize = conf.jobPoolSize;
        }
    }

    private Boolean jsonBodyPatch;

    protected T jsonBodyPatch(boolean enabled) {
        jsonBodyPatch = enabled;
        return me();
    }

    public boolean allowJsonBodyPatch() {
        if (null == jsonBodyPatch) {
            jsonBodyPatch = get(JSON_BODY_PATCH, true);
        }
        return jsonBodyPatch;
    }

    private void _mergeJsonBodyPatch(AppConfig conf) {
        if (!hasConfiguration(JSON_BODY_PATCH)) {
            jsonBodyPatch = conf.jsonBodyPatch;
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
            httpExternalPort = getInteger(HTTP_EXTERNAL_PORT, httpExternal() ? 80 : httpPort());
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
            httpExternal = get(HTTP_EXTERNAL_SERVER, Act.isProd());
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
            httpExternalSecurePort = get(HTTP_EXTERNAL_SECURE_PORT, httpExternal() ? 443 : httpPort());
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
            if (Act.isTest()) {
                httpPort = chooseRandomDefaultHttpPort();
            } else {
                httpPort = get(HTTP_PORT, 5460);
            }
        }
        return httpPort;
    }

    private void _mergeHttpPort(AppConfig conf) {
        if (!hasConfiguration(HTTP_PORT)) {
            httpPort = conf.httpPort;
        }
    }

    private static void clearRandomServerSockets() {
        for (ServerSocket ss : randomServerSockets.values()) {
            IO.close(ss);
        }
        randomServerSockets.clear();
    }

    public static void clearRandomServerSocket(int port) {
        ServerSocket ss = randomServerSockets.remove(port);
        IO.close(ss);
    }

    private static Map<Integer, ServerSocket> randomServerSockets = new HashMap<>();

    private static int chooseRandomDefaultHttpPort() {
        int maxTry = 10;
        while (maxTry-- > 0) {
            clearRandomServerSockets();
            boolean ok = true;
            int httpPort = randomPort();
            Act.LOGGER.debug("Random port detected: " + httpPort);
            for (int i = 1; i < 4; ++i) {
                int port = httpPort + i;
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket(port);
                    randomServerSockets.put(port, ss);
                    Act.LOGGER.debug("Successfully bind to port: " + port);
                } catch (IOException e) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                Act.LOGGER.info("Default port allocated for testing: " + httpPort);
                return httpPort;
            }
        }
        throw new IllegalStateException("Unable to find random HTTP port");
    }

    private static int randomPort() {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(0);
            return ss.getLocalPort();
        } catch (IOException e) {
            throw E.ioException(e);
        } finally {
            IO.close(ss);
        }
    }

    private Boolean httpSecure = null;

    protected T httpSecure(boolean secure) {
        this.httpSecure = secure;
        return me();
    }

    public boolean httpSecure() {
        if (null == httpSecure) {
            httpSecure = get(HTTP_SECURE, !Act.isDev() && S.neq(Act.profile(), "dev", S.IGNORECASE));
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
            httpsPort = get(HTTPS_PORT, 5443);
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
            RedirectToLoginUrl redirectToLoginUrl = app.getInstance(RedirectToLoginUrl.class);
            MissingAuthenticationHandler defHandler = redirectToLoginUrl.hasLoginUrl() ? redirectToLoginUrl : app.getInstance(ReturnUnauthorized.class);
            mah = get(HANDLER_MISSING_AUTHENTICATION, defHandler);
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
            ajaxMah = get(HANDLER_MISSING_AUTHENTICATION_AJAX, missingAuthenticationHandler());
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
            csrfCheckFailureHandler = get(HANDLER_CSRF_CHECK_FAILURE, missingAuthenticationHandler());
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
            ajaxCsrfCheckFailureHandler = get(HANDLER_AJAX_CSRF_CHECK_FAILURE, csrfCheckFailureHandler());
        }
        return ajaxCsrfCheckFailureHandler;
    }

    private void _mergeAjaxCsrfCheckFailureHandler(AppConfig config) {
        if (!hasConfiguration(HANDLER_AJAX_CSRF_CHECK_FAILURE)) {
            csrfCheckFailureHandler = config.csrfCheckFailureHandler;
        }
    }

    private Integer threadlocalBufRetentionLimit;

    protected T threadLocalBufRetentionLimit(int limit) {
        threadlocalBufRetentionLimit = limit;
        return me();
    }

    public int threadLocalBufRetentionLimit() {
        if (null == threadlocalBufRetentionLimit) {
            StrBufRetentionLimitCalculator calc = new StrBufRetentionLimitCalculator();
            threadlocalBufRetentionLimit = get(OSGL_THREADLOCAL_BUF_LIMIT, 1024 * calc.calculate());
        }
        return threadlocalBufRetentionLimit;
    }

    private void _mergeStrBufRetentionLimit(AppConfig config) {
        if (!hasConfiguration(OSGL_THREADLOCAL_BUF_LIMIT)) {
            threadlocalBufRetentionLimit = config.threadlocalBufRetentionLimit;
        }
    }

    private Password.Validator defPasswordValidator;

    protected T defPasswordValidator(Password.Validator validator) {
        defPasswordValidator = $.requireNotNull(validator);
        return me();
    }

    protected T defPasswordSpec(String spec) {
        _defPasswordSpec(spec);
        return me();
    }

    public Password.Validator defPasswordValidator() {
        if (null == defPasswordValidator) {
            String s = get(PASSWORD_DEF_SPEC, Act.isDev() ? "a[3,]" : "aA0[6,]");
            _defPasswordSpec(s);
        }
        return defPasswordValidator;
    }

    private void _defPasswordSpec(String spec) {
        try {
            defPasswordValidator = PasswordSpec.parse(spec);
        } catch (IllegalArgumentException e) {
            // try to check if the spec is a PasswordValidator
            try {
                defPasswordValidator = app.getInstance(spec);
            } catch (Exception e2) {
                throw new ConfigurationException("Password spec unrecognized: " + spec);
            }
        }
    }

    private Boolean monitorEnabled;

    protected T enableMonitor(boolean enable) {
        this.monitorEnabled = enable;
        return me();
    }

    public boolean monitorEnabled() {
        if (null == monitorEnabled) {
            monitorEnabled = $.bool(get(MONITOR, false));
        }
        return monitorEnabled;
    }

    private void _mergeMonitorEnabled(AppConfig config) {
        if (!hasConfiguration(MONITOR)) {
            this.monitorEnabled = config.monitorEnabled;
        }
    }

    private Map<String, NamedPort> namedPorts = null;

    protected T namedPorts(NamedPort... namedPorts) {
        this.namedPorts = new HashMap<>();
        for (NamedPort port : namedPorts) {
            this.namedPorts.put(port.name(), port);
        }
        return me();
    }

    public Collection<NamedPort> namedPorts() {
        if (null == namedPorts) {
            String s = get(NAMED_PORTS, null);
            if (null == s) {
                namedPorts = new HashMap<>();
            } else {
                String[] sa = (s.split("[,;]+"));
                Map<String, NamedPort> builder = new HashMap<>();
                for (String s0 : sa) {
                    String[] sa0 = s0.split(":");
                    E.invalidConfigurationIf(2 != sa0.length, "Unknown named port configuration: %s", s);
                    String name = sa0[0].trim();
                    String val = sa0[1].trim();
                    NamedPort port = new NamedPort(name, Integer.parseInt(val));
                    if (!builder.containsKey(port.name())) {
                        builder.put(port.name(), port);
                    } else {
                        throw E.invalidConfiguration("port[%s] already configured", name);
                    }
                }
                namedPorts = builder;
            }
            if (cliOverHttp() && !namedPorts.containsKey(PORT_CLI_OVER_HTTP)) {
                namedPorts.put(PORT_CLI_OVER_HTTP, new NamedPort(PORT_CLI_OVER_HTTP, cliOverHttpPort()));
            }
            if (!namedPorts.containsKey(PORT_SYS)) {
                namedPorts.put(PORT_SYS, new NamedPort(PORT_SYS, httpPort() + 3));
            }

        }
        return namedPorts.values();
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
            encoding = get(ENCODING, StandardCharsets.UTF_8.name());
        }
        return encoding;
    }

    private void _mergeEncoding(AppConfig conf) {
        if (!hasConfiguration(ENCODING)) {
            encoding = conf.encoding;
        }
    }

    private volatile String datePattern = null;
    private SimpleDateFormat dateFormat = null;
    private final ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            if (null == dateFormat) {
                String pattern = datePattern();
                if (null == dateFormat) {
                    dateFormat = new SimpleDateFormat(pattern);
                }
            }
            return (SimpleDateFormat) dateFormat.clone();
        }
    };

    protected T datePattern(String fmt) {
        E.illegalArgumentIf(S.blank(fmt), "Date format pattern cannot be empty");
        this.datePattern = fmt;
        return me();
    }

    public SimpleDateFormat dateFormat() {
        return dateFormatThreadLocal.get();
    }

    public String datePattern() {
        if (null == datePattern) {
            synchronized (this) {
                if (null == datePattern) {
                    dateStyle = DateTimeStyle.MEDIUM;
                    datePattern = get(FORMAT_DATE, null);
                    if (null == datePattern) {
                        dateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.DEFAULT, locale());
                    } else if (S.eq(datePattern, "long", S.IGNORECASE)) {
                        dateStyle = DateTimeStyle.LONG;
                        dateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.LONG, locale());
                    } else if (S.eq(datePattern, "medium", S.IGNORECASE)) {
                        dateStyle = DateTimeStyle.MEDIUM;
                        dateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM, locale());
                    } else if (S.eq(datePattern, "short", S.IGNORECASE)) {
                        dateStyle = DateTimeStyle.SHORT;
                        dateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, locale());
                    }
                    if (null != dateFormat) {
                        datePattern = dateFormat.toPattern();
                    }
                }
            }
        }
        return datePattern;
    }

    private void _mergeDatePattern(AppConfig conf) {
        if (!hasConfiguration(FORMAT_DATE)) {
            datePattern = conf.datePattern;
            dateStyle = conf.dateStyle;
        }
    }

    private Map<Locale, String> localizedDatePatterns = new HashMap<>();

    public String localizedDatePattern(Locale locale) {
        String s = localizedDatePatterns.get(locale);
        if (null == s) {
            s = getLocalizedDateTimePattern(locale, DateTimeType.DATE);
            localizedDatePatterns.put(locale, s);
        }
        return s;
    }


    private volatile String timePattern = null;
    private SimpleDateFormat timeFormat = null;
    private final ThreadLocal<SimpleDateFormat> timeFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            if (null == timeFormat) {
                String pattern = timePattern();
                if (null == timeFormat) {
                    timeFormat = new SimpleDateFormat(pattern);
                }
            }
            return (SimpleDateFormat) timeFormat.clone();
        }
    };

    protected T timePattern(String pattern) {
        E.illegalArgumentIf(S.blank(pattern), "Time format pattern cannot be empty");
        this.timePattern = pattern;
        return me();
    }

    public SimpleDateFormat timeFormat() {
        return timeFormatThreadLocal.get();
    }

    public String timePattern() {
        if (null == timePattern) {
            synchronized (this) {
                if (null == timePattern) {
                    timeStyle = DateTimeStyle.MEDIUM;
                    timePattern = get(FORMAT_TIME, null);
                    if (null == timePattern) {
                        timeFormat = (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.DEFAULT, locale());
                    } else if (S.eq(timePattern, "long", S.IGNORECASE)) {
                        timeStyle = DateTimeStyle.LONG;
                        timeFormat = (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.LONG, locale());
                    } else if (S.eq(timePattern, "medium", S.IGNORECASE)) {
                        timeStyle = DateTimeStyle.MEDIUM;
                        timeFormat = (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.MEDIUM, locale());
                    } else if (S.eq(timePattern, "short", S.IGNORECASE)) {
                        timeStyle = DateTimeStyle.SHORT;
                        timeFormat = (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT, locale());
                    }

                    if (null != timeFormat) {
                        timePattern = timeFormat.toPattern();
                    }
                }
            }
        }
        return timePattern;
    }

    private void _mergeTimePattern(AppConfig conf) {
        if (!hasConfiguration(FORMAT_TIME)) {
            timePattern = conf.timePattern;
            timeStyle = conf.timeStyle;
        }
    }

    private Map<Locale, String> localizedTimePattern = new HashMap<>();

    public String localizedTimePattern(Locale locale) {
        String s = localizedTimePattern.get(locale);
        if (null == s) {
            s = getLocalizedDateTimePattern(locale, DateTimeType.TIME);
            localizedTimePattern.put(locale, s);
        }
        return s;
    }


    private volatile String dateTimePattern = null;
    private SimpleDateFormat dateTimeFormat = null;
    private final ThreadLocal<SimpleDateFormat> dateTimeFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            if (null == dateTimeFormat) {
                String pattern = dateTimePattern();
                if (null == dateTimeFormat) {
                    dateTimeFormat = new SimpleDateFormat(pattern);
                }
            }
            return (SimpleDateFormat) dateTimeFormat.clone();
        }
    };

    protected T dateTimePattern(String pattern) {
        E.illegalArgumentIf(S.blank(pattern), "Date time format pattern cannot be empty");
        this.dateTimePattern = pattern;
        return me();
    }

    public SimpleDateFormat dateTimeFormat() {
        return dateTimeFormatThreadLocal.get();
    }

    public String dateTimePattern() {
        if (null == dateTimePattern) {
            synchronized (this) {
                if (null == dateTimePattern) {
                    dateTimeStyle = DateTimeStyle.MEDIUM;
                    dateTimePattern = get(FORMAT_DATE_TIME, null);
                    if (null == dateTimePattern) {
                        dateTimeFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, locale());
                    } else if (S.eq(dateTimePattern, "long", S.IGNORECASE)) {
                        dateTimeStyle = DateTimeStyle.LONG;
                        dateTimeFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale());
                    } else if (S.eq(dateTimePattern, "medium", S.IGNORECASE)) {
                        dateTimeStyle = DateTimeStyle.MEDIUM;
                        dateTimeFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale());
                    } else if (S.eq(dateTimePattern, "short", S.IGNORECASE)) {
                        dateTimeStyle = DateTimeStyle.SHORT;
                        dateTimeFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale());
                    }
                    if (null != dateTimeFormat) {
                        dateTimePattern = dateTimeFormat.toPattern();
                    }
                }
            }
        }
        return dateTimePattern;
    }

    private void _mergeDateTimeFmt(AppConfig conf) {
        if (!hasConfiguration(FORMAT_DATE_TIME)) {
            dateTimePattern = conf.dateTimePattern;
        }
    }

    private Map<Locale, String> localizedDateTimePatterns = new HashMap<>();

    public String localizedDateTimePattern(Locale locale) {
        String s = localizedDateTimePatterns.get(locale);
        if (null == s) {
            s = getLocalizedDateTimePattern(locale, DateTimeType.DATE_TIME);
            localizedDateTimePatterns.put(locale, s);
        }
        return s;
    }

    private boolean isLocaleMatchesDefault(Locale locale) {
        return locale().equals(locale) || baseLocale.equals(locale);
    }

    private Locale locale = null;
    private Locale baseLocale = null;

    protected T locale(Locale locale) {
        this.baseLocale = baseLocaleOf(locale);
        this.locale = locale;
        return me();
    }

    public Locale locale() {
        if (null == locale) {
            locale(get(LOCALE, Locale.getDefault()));
        }
        return locale;
    }

    private void _mergeLocale(AppConfig conf) {
        if (!hasConfiguration(LOCALE)) {
            locale = conf.locale;
            baseLocale = conf.baseLocale;
        }
    }

    private Locale baseLocaleOf(Locale locale) {
        return new Locale.Builder().setLanguage(locale.getLanguage()).build();
    }

    private String sourceVersion = null;

    protected T sourceVersion(JavaVersion version) {
        sourceVersion = FastStr.of(version.name()).substr(1).replace('_', '.').toString();
        return me();
    }

    public String sourceVersion() {
        if (null == sourceVersion) {
            sourceVersion = get(SOURCE_VERSION, S.string($.JAVA_VERSION));
//            sourceVersion = get(AppConfigKey.SOURCE_VERSION, null);
//            if (null == sourceVersion) {
//                int n = $.JAVA_VERSION;
//                if (n > 8) {
//                    warn("ActFramework support compiling source code up to Java 8 only");
//                    n = 8;
//                }
//                sourceVersion = S.string(n);
//            } else {
//                if (sourceVersion.contains("1.")) {
//                    sourceVersion = sourceVersion.substring(2);
//                }
//                if (Integer.parseInt(sourceVersion) > 8) {
//                    warn("ActFramework support compiling source code up to Java 8 only");
//                    sourceVersion = "8";
//                }
//            }
        }
        return sourceVersion;
    }

    private void _mergeSourceVersion(AppConfig conf) {
        if (!hasConfiguration(SOURCE_VERSION)) {
            sourceVersion = conf.sourceVersion;
        }
    }

    private Boolean selfHealing;

    protected T selfHealing(boolean on) {
        selfHealing = on;
        return me();
    }

    public boolean selfHealing() {
        if (null == selfHealing) {
            selfHealing = get(SYS_SELF_HEALING, false);
        }
        return selfHealing;
    }

    private void _mergeSelfHealing(AppConfig conf) {
        if (!hasConfiguration(SYS_SELF_HEALING)) {
            selfHealing = conf.selfHealing;
        }
    }

    private String targetVersion = null;

    protected T targetVersion(JavaVersion version) {
        targetVersion = FastStr.of(version.name()).substr(1).replace('_', '.').toString();
        return me();
    }

    public String targetVersion() {
        if (null == targetVersion) {
            targetVersion = get(TARGET_VERSION, null);
            if (null == targetVersion) {
                int n = $.JAVA_VERSION;
                if (n > 8) {
                    warn("ActFramework support compiling source code up to Java 8 only");
                    n = 8;
                }
                targetVersion = S.string(n);
            } else {
                if (targetVersion.contains("1.")) {
                    targetVersion = targetVersion.substring(2);
                }
                if (Integer.parseInt(targetVersion) > 8) {
                    warn("ActFramework support compiling source code up to Java 8 only");
                    targetVersion = "8";
                }
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
            if (s.startsWith("java.") || s.startsWith("javax.")) {
                return false;
            }
            if (s.contains("$")) {
                for (String pkg : scanList) {
                    if (s.startsWith(pkg + "$")) {
                        return true;
                    }
                }
            }
            boolean shouldCheckPattern = false;
            Set<String> prefixList = app().scanPrefixList();
            for (String prefix : prefixList) {
                if (s.startsWith(prefix)) {
                    shouldCheckPattern = true;
                    break;
                }
            }
            if (!shouldCheckPattern) {
                return false;
            }
            shouldCheckPattern = false;
            Set<String> suffixList = app().scanSuffixList();
            for (String suffix : suffixList) {
                if (s.endsWith(suffix)) {
                    shouldCheckPattern = true;
                    break;
                }
            }
            if (!shouldCheckPattern) {
                return false;
            }
            for (Pattern pattern : app().scanPattern()) {
                if (pattern.matcher(s).matches()) {
                    return true;
                }
            }
            return false;
        }
    };

    public $.Predicate<String> appClassTester() {
        if (null == APP_CLASS_TESTER) {
            String scanPackage = get(AppConfigKey.SCAN_PACKAGE, null);
            if (S.isBlank(scanPackage)) {
                APP_CLASS_TESTER = SYSTEM_SCAN_LIST;
            } else {
                final String[] sp = scanPackage.trim().split(Constants.LIST_SEPARATOR);
                final int len = sp.length;
                for (int i = 0; i < len; ++i) {
                    String pkg = sp[i];
                    if (pkg.startsWith("act.") || "act".equals(pkg)) {
                        throw new ConfigurationException("Scan package cannot be 'act' or starts with 'act.'");
                    }
                }
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

    private Integer testTimeout;

    protected T testTimeout(int timeout) {
        if (timeout < 10) {
            logger.warn("test.timeout reset to minimum value: 10");
            timeout = 10;
        }
        testTimeout = timeout;
        return me();
    }

    public int testTimeout() {
        if (null == testTimeout) {
            int defTimeout = ("e2e".equalsIgnoreCase(Act.profile()) || "test".equalsIgnoreCase(Act.profile())) ? 10 : 60 * 60;
            testTimeout = get(TEST_TIMEOUT, defTimeout);
            this.testTimeout(testTimeout); // make sure we have a valid number
        }
        return testTimeout;
    }

    private void _mergeTestTimeout(AppConfig conf) {
        if (!hasConfiguration(TEST_TIMEOUT)) {
            testTimeout = conf.testTimeout;
        }
    }

    private TemplatePathResolver templatePathResolver;

    protected T templatePathResolver(TemplatePathResolver resolver) {
        E.NPE(resolver);
        templatePathResolver = resolver;
        return me();
    }

    public TemplatePathResolver templatePathResolver() {
        if (null == templatePathResolver) {
            templatePathResolver = get(AppConfigKey.RESOLVER_TEMPLATE_PATH, new TemplatePathResolver());
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
            templateHome = get(AppConfigKey.TEMPLATE_HOME, "default");
        }
        return templateHome;
    }

    private void _mergeTemplateHome(AppConfig conf) {
        if (!hasConfiguration(AppConfigKey.TEMPLATE_HOME)) {
            templateHome = conf.templateHome;
        }
    }

    private Boolean paramBindingKeywordMatching;

    protected T paramBindingKeywordMatching(boolean enabled) {
        paramBindingKeywordMatching = enabled;
        return me();
    }

    public boolean paramBindingKeywordMatching() {
        if (null == paramBindingKeywordMatching) {
            paramBindingKeywordMatching = get(PARAM_BINDING_KEYWORD_MATCHING, false);
        }
        return paramBindingKeywordMatching;
    }

    private void _mergeParamBindingKeywordMatching(AppConfig config) {
        if (!hasConfiguration(PARAM_BINDING_KEYWORD_MATCHING)) {
            paramBindingKeywordMatching = config.paramBindingKeywordMatching;
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
            pingPath = get(AppConfigKey.PING_PATH, null);
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

    private Integer reqThrottle;

    protected T requestThrottle(final int throttle) {
        E.illegalArgumentIf(throttle < 1, "request throttle must be positive integer");
        this.reqThrottle = throttle;
        return me();
    }

    public int requestThrottle() {
        if (null == reqThrottle) {
            reqThrottle = get(REQUEST_THROTTLE, 2);
        }
        return reqThrottle;
    }

    private void _mergeReqThrottle(AppConfig config) {
        if (!hasConfiguration(REQUEST_THROTTLE)) {
            this.reqThrottle = config.reqThrottle;
        }
    }

    private Boolean reqThrottleExpireScale;

    protected T requestThrottleExpireScale(final boolean enabled) {
        this.reqThrottleExpireScale = enabled;
        return me();
    }

    public boolean requestThrottleExpireScale() {
        if (null == reqThrottleExpireScale) {
            reqThrottleExpireScale = get(REQUEST_THROTTLE_EXPIRE_SCALE, false);
        }
        return reqThrottleExpireScale;
    }

    private void _mergeReqThrottleExpireScale(AppConfig config) {
        if (!hasConfiguration(REQUEST_THROTTLE_EXPIRE_SCALE)) {
            this.reqThrottleExpireScale = config.reqThrottleExpireScale;
        }
    }

    private $.Func0<H.Format> jsonContentTypeProvider = null;
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
        jsonContentTypeProvider = new $.Func0<H.Format>() {
            @Override
            public H.Format apply() throws NotAppliedException, $.Break {
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

    public $.Func0<H.Format> jsonContentTypeProvider() {
        if (null == renderJsonIeFix) {
            String contentType = get(RENDER_JSON_CONTENT_TYPE_IE, null);
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
            renderJsonOutputCharset = get(RENDER_JSON_OUTPUT_CHARSET, false);
        }
        return renderJsonOutputCharset;
    }

    private void _mergeRenderJsonOutputCharset(AppConfig config) {
        if (!hasConfiguration(RENDER_JSON_OUTPUT_CHARSET)) {
            renderJsonOutputCharset = config.renderJsonOutputCharset;
        }
    }

    private String serverHeader;
    private static final String DEF_SERVER_HEADER = "act/" + Act.VERSION.getProjectVersion();
    private static String DEF_APP_SERVER_HEADER = appServerHeader();

    private static String appServerHeader() {
        App app = Act.app();
        if (null == app) {
            return "app/1.0";
        }
        String shortId = app.shortId();
        if (null == shortId) {
            shortId = "app";
        }
        Version version = app.version();
        return shortId + "/" + (null == version ? "1.0" : version.getProjectVersion());
    }

    protected T serverHeader(String header) {
        serverHeader = header;
        return me();
    }

    public String serverHeader() {
        if (null == serverHeader) {
            serverHeader = get(AppConfigKey.SERVER_HEADER, serverHeaderUseApp() ? DEF_APP_SERVER_HEADER : DEF_SERVER_HEADER);
        }
        return serverHeader;
    }

    private void _mergeServerHeader(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SERVER_HEADER)) {
            serverHeader = config.serverHeader;
        }
    }

    private Boolean serverHeaderUseApp;

    protected T serverHeaderUseApp(boolean b) {
        serverHeaderUseApp = b;
        return me();
    }

    private boolean serverHeaderUseApp() {
        if (null == serverHeaderUseApp) {
            serverHeaderUseApp = get(AppConfigKey.SERVER_HEADER_USE_APP, true);
        }
        return serverHeaderUseApp;
    }

    private void _mergeServerHeaderUseApp(AppConfig config) {
        if (!hasConfiguration(SERVER_HEADER_USE_APP)) {
            serverHeaderUseApp = config.serverHeaderUseApp;
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
            sessionKeyUsername = get(SESSION_KEY_USERNAME, "username");
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
            String profile = Act.profile();
            S.Buffer buf = S.buffer(app().shortId());
            if (S.neq("prod", profile, S.IGNORECASE)) {
                buf.a("-").a(profile);
            }
            buf.a("-");
            cookiePrefix = get(COOKIE_PREFIX, buf.toString());
            cookiePrefix = cookiePrefix.trim().toLowerCase();
            set(COOKIE_PREFIX, cookiePrefix);
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
            sessionTtl = get(AppConfigKey.SESSION_TTL, 60 * 30);
        }
        return sessionTtl;
    }

    private void _mergeSessionTtl(AppConfig conf) {
        if (!hasConfiguration(AppConfigKey.SESSION_TTL)) {
            sessionTtl = conf.sessionTtl;
        }
    }

    private boolean sessionPassThrough;
    private boolean sessionPassThroughSet; // use this to save auto-box of sessionPassThrough flag

    protected T sessionPassThrough(boolean b) {
        sessionPassThrough = b;
        return me();
    }

    public boolean sessionPassThrough() {
        if (!sessionPassThroughSet) {
            sessionPassThrough = get(SESSION_PASS_THROUGH, false);
        }
        return sessionPassThrough;
    }

    private void _mergeSessionPassThrough(AppConfig config) {
        if (!hasConfiguration(SESSION_PASS_THROUGH)) {
            sessionPassThrough = config.sessionPassThrough;
        }
    }

    private Boolean sessionPersistent;

    protected T sessionPersistent(boolean persistenSession) {
        sessionPersistent = persistenSession;
        return me();
    }

    public boolean persistSession() {
        if (null == sessionPersistent) {
            sessionPersistent = get(AppConfigKey.SESSION_PERSISTENT_ENABLED, false);
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
            sessionEncrypt = get(AppConfigKey.SESSION_ENCRYPT_ENABLED, false);
        }
        return sessionEncrypt;
    }

    private void _mergeSessionEncrpt(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SESSION_ENCRYPT_ENABLED)) {
            sessionEncrypt = config.sessionEncrypt;
        }
    }

    private act.session.SessionMapper sessionMapper = null;

    protected void sessionMapper(act.session.SessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    @Lazy
    public act.session.SessionMapper sessionMapper() {
        if (null == sessionMapper && null != app.injector()) {
            sessionMapper = get(SESSION_MAPPER, app().getInstance(CookieSessionMapper.class));
        }
        return sessionMapper;
    }

    private void _mergeSessionMapper(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SESSION_MAPPER)) {
            sessionMapper = config.sessionMapper;
        }
    }

    private SessionCodec sessionCodec = null;

    protected void sessionCodec(SessionCodec codec) {
        this.sessionCodec = $.requireNotNull(codec);
    }

    @Lazy
    public SessionCodec sessionCodec() {
        if (null == sessionCodec) {
            if (null == app.injector()) {
                // unit testing
                sessionCodec = null;
            } else {
                sessionCodec = get(SESSION_CODEC, app.getInstance(DefaultSessionCodec.class));
            }
        }
        return sessionCodec;
    }

    private void _mergeSessionCodec(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SESSION_CODEC)) {
            sessionCodec = config.sessionCodec;
        }
    }

    private Boolean sessionOutputExpiration;

    protected T sessionOutputExpiration(boolean enabled) {
        sessionOutputExpiration = enabled;
        return me();
    }

    public boolean sessionOutputExpiration() {
        if (null == sessionOutputExpiration) {
            sessionOutputExpiration = get(SESSION_OUTPUT_EXPIRATION, true);
        }
        return sessionOutputExpiration;
    }

    private String sessionHeader;
    private boolean sessionHeaderSet;

    protected T sessionHeader(String header) {
        this.sessionHeader = header;
        this.sessionHeaderSet = true;
        return me();
    }

    public String sessionHeader() {
        if (!sessionHeaderSet) {
            sessionHeader = get(SESSION_HEADER, S.pathConcat(sessionHeaderPrefix(), '-', "Session"));
            sessionHeaderSet = true;
        }
        return sessionHeader;
    }

    private void _mergeSessionHeader(AppConfig conf) {
        if (!hasConfiguration(SESSION_HEADER)) {
            sessionHeader = conf.sessionHeader;
            sessionHeaderSet = conf.sessionHeaderSet;
        }
    }

    private String sessionHeaderPrefix;

    protected T sessionHeaderPrefix(String prefix) {
        this.sessionHeaderPrefix = prefix;
        return me();
    }

    public String sessionHeaderPrefix() {
        if (null == sessionHeaderPrefix) {
            sessionHeaderPrefix = get(SESSION_HEADER_PREFIX, HeaderTokenSessionMapper.DEF_HEADER_PREFIX);
        }
        return sessionHeaderPrefix;
    }

    private String sessionHeaderPayloadPrefix = null;

    protected void sessionHeaderPayloadPrefix(String prefix) {
        this.sessionHeaderPayloadPrefix = prefix;
    }

    public String sessionHeaderPayloadPrefix() {
        if (null == sessionHeaderPayloadPrefix) {
            String s = get(SESSION_HEADER_PAYLOAD_PREFIX, HeaderTokenSessionMapper.DEF_PAYLOAD_PREFIX);
            s = S.strip(s).of(S.DOUBLE_QUOTES);
            sessionHeaderPayloadPrefix = s;
        }
        return sessionHeaderPayloadPrefix;
    }

    private void _mergeSessionHeaderPayloadPrefix(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.SESSION_HEADER_PAYLOAD_PREFIX)) {
            sessionHeaderPayloadPrefix = config.sessionHeaderPayloadPrefix;
        }
    }

    private String sessionQueryParamName;

    protected T sessionQueryParamName(String paramName) {
        sessionQueryParamName = paramName;
        return me();
    }

    public String getSessionQueryParamName() {
        if (null == sessionQueryParamName) {
            sessionQueryParamName = get(SESSION_QUERY_PARAM_NAME, sessionHeader());
        }
        return sessionQueryParamName;
    }

    private void _mergeSessionQueryParamName(AppConfig config) {
        if (!hasConfiguration(SESSION_QUERY_PARAM_NAME)) {
            sessionQueryParamName = config.sessionQueryParamName;
        }
    }

    private Boolean sessionSecure = null;

    protected T sessionSecure(boolean secure) {
        sessionSecure = secure;
        return me();
    }

    public boolean sessionSecure() {
        if (null == sessionSecure) {
            sessionSecure = get(AppConfigKey.SESSION_SECURE, httpSecure());
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
            secret = get(AppConfigKey.SECRET, "myawesomeapp");
            if ("myawesomeapp".equals(secret)) {
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

    private Boolean secretRotate = null;

    protected T secretRotate(boolean enabled) {
        secretRotate = enabled;
        return me();
    }

    public boolean rotateSecret() {
        if (null == secretRotate) {
            secretRotate = get(SECRET_ROTATE, false);
        }
        return secretRotate;
    }

    private void _mergeSecretRotate(AppConfig config) {
        if (!hasConfiguration(SECRET_ROTATE)) {
            secretRotate = config.secretRotate;
        }
    }

    private Integer secretRotatePeriod;

    /**
     * Set `secret.rotate.period` in terms of minute
     *
     * @param period the minutes between two secret rotate happening
     * @return this config object
     * @see AppConfigKey#SECRET_ROTATE_PERIOD
     */
    protected T secretRotatePeroid(int period) {
        E.illegalArgumentIf(period < 1, "minimum secret.rotate.period is 1 (minute)");
        secretRotatePeriod = period;
        return me();
    }

    public int secretRotatePeriod() {
        if (null == secretRotatePeriod) {
            boolean validSetting = true;
            String s = get(SECRET_ROTATE_PERIOD, "30");
            int minutes;
            if (!N.isInt(s)) {
                int seconds = Time.parseDuration(s);
                int reminder = seconds % 60;
                if (0 != reminder) {
                    validSetting = false;
                    seconds += (60 - reminder);
                }
                minutes = seconds / 60;
            } else {
                minutes = Integer.parseInt(s);
            }
            if (minutes <= 0) {
                validSetting = false;
                minutes = 30;
            }
            secretRotatePeriod = RotationSecretProvider.roundToPeriod(minutes);
            validSetting = validSetting && (secretRotatePeriod == minutes);
            if (!validSetting) {
                warn("invalid secret.rotate.period setting found: %s, system automatically set it to: %s", s, secretRotatePeriod);
            }
        }
        return secretRotatePeriod;
    }

    private void _mergeSecretRotatePeriod(AppConfig config) {
        if (!hasConfiguration(SECRET_ROTATE_PERIOD)) {
            secretRotatePeriod = config.secretRotatePeriod;
        }
    }

    private volatile SecureTicketCodec secureTicketCodec;
    private String secureTicketCodecClass;

    protected T secureTicketCodec(String secureTicketCodecClass) {
        this.secureTicketCodecClass = $.requireNotNull(secureTicketCodecClass);
        return me();
    }

    protected T secureTicketCodec(SecureTicketCodec codec) {
        this.secureTicketCodec = $.requireNotNull(codec);
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
                secureTicketCodecClass = get(SECURE_TICKET_CODEC, null);
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

    private Boolean traceHandler;

    protected T traceHandler(boolean enabled) {
        this.traceHandler = enabled;
        return me();
    }

    public boolean traceHandler() {
        if (null == traceHandler) {
            traceHandler = get(TRACE_HANDLER_ENABLED, false);
        }
        return traceHandler;
    }

    // must be public, otherwise it get IllegalAccessError
    public void toggleTraceHandler(boolean enabled) {
        traceHandler = enabled;
    }

    private void _mergeTraceHandler(AppConfig config) {
        if (!hasConfiguration(TRACE_HANDLER_ENABLED)) {
            this.traceHandler = config.traceHandler;
        }
    }

    private Boolean traceRequest;

    protected T traceRequests(boolean enabled) {
        this.traceRequest = enabled;
        return me();
    }

    public boolean traceRequests() {
        if (null == traceRequest) {
            traceRequest = get(TRACE_REQUEST_ENABLED, false);
        }
        return traceRequest;
    }

    // must be public, otherwise it get IllegalAccessError
    public void toggleTraceRequest(boolean enabled) {
        traceRequest = enabled;
    }

    private void _mergeTraceRequests(AppConfig config) {
        if (!hasConfiguration(TRACE_REQUEST_ENABLED)) {
            this.traceRequest = config.traceRequest;
        }
    }

    private List<File> moduleBases;

    public List<File> moduleBases() {
        if (null == moduleBases) {
            String v = get(AppConfigKey.MODULES, null);
            moduleBases = processModules(v);
        }
        return moduleBases;
    }

    private List<File> processModules(String v) {
        if (S.blank(v)) {
            return C.list();
        } else {
            List<File> files = new ArrayList<>();
            File base = app.base();
            for (String s : v.trim().split("[;:]+")) {
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
            metricEnabled = get(METRIC_ENABLED, true);
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

    private CacheServiceProvider cacheServiceProvider = null;

    protected T cacheService(CacheServiceProvider provider) {
        E.NPE(provider);
        this.cacheServiceProvider = provider;
        return me();
    }

    protected T cacheService(Class<? extends CacheServiceProvider> provider) {
        this.cacheServiceProvider = $.newInstance(provider);
        return me();
    }

    public CacheService cacheService(String name) {
        CacheService cacheService = cacheServiceProvider().get(name);
        E.illegalStateIf(cacheService.state().isShutdown(), "Cache service[%s] already shutdown.", name);
        if (!cacheService.state().isStarted()) {
            cacheService.startup();
        }
        return cacheService;
    }

    public CacheServiceProvider cacheServiceProvider() {
        if (null == cacheServiceProvider) {
            CacheServiceProvider.Impl.setClassLoader(app().classLoader());
            try {
                cacheServiceProvider = get(AppConfigKey.CACHE_IMPL, null);
            } catch (ConfigurationException e) {
                Object obj = helper.getValFromAliases(raw, AppConfigKey.CACHE_IMPL.toString(), "impl", null);
                cacheServiceProvider = CacheServiceProvider.Impl.valueOfIgnoreCase(obj.toString());
                if (null != cacheServiceProvider) {
                    set(AppConfigKey.CACHE_IMPL, cacheServiceProvider);
                } else {
                    throw e;
                }
            }
            if (null == cacheServiceProvider) {
                cacheServiceProvider = CacheServiceProvider.Impl.Auto;
            }
        }
        return cacheServiceProvider;
    }

    public void resetCacheServices() {
        if (!Act.isDev()) {
            return;
        }
        OsglConfig.internalCache().clear();

    }

    private void _mergeCacheServiceProvider(AppConfig config) {
        if (!hasConfiguration(AppConfigKey.CACHE_IMPL)) {
            cacheServiceProvider = config.cacheServiceProvider;
        }
    }

    private String _cacheName;

    protected T cacheName(String name) {
        this._cacheName = name;
        return me();
    }

    public String cacheName() {
        if (null == _cacheName) {
            _cacheName = get(AppConfigKey.CACHE_NAME, "_act_app_");
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
            _cacheNameSession = get(AppConfigKey.CACHE_NAME_SESSION, cacheName());
        }
        return _cacheNameSession;
    }

    private void _mergeCacheNameSession(AppConfig config) {
        if (!hasConfiguration(CACHE_NAME_SESSION)) {
            _cacheNameSession = config._cacheNameSession;
        }
    }

    private Integer cacheTtl;

    protected T cacheTtl(int ttl) {
        cacheTtl = ttl;
        return me();
    }

    public int cacheTtl() {
        if (null == cacheTtl) {
            cacheTtl = getInteger(CACHE_TTL, 60);
        }
        return cacheTtl;
    }

    private void _mergeCacheTtl(AppConfig config) {
        if (!hasConfiguration(CACHE_TTL)) {
            cacheTtl = config.cacheTtl;
        }
    }

    private UnknownHttpMethodProcessor _unknownHttpMethodProcessor = null;

    protected T unknownHttpMethodProcessor(UnknownHttpMethodProcessor handler) {
        this._unknownHttpMethodProcessor = $.requireNotNull(handler);
        return me();
    }

    public UnknownHttpMethodProcessor unknownHttpMethodProcessor() {
        if (null == _unknownHttpMethodProcessor) {
            _unknownHttpMethodProcessor = get(AppConfigKey.HANDLER_UNKNOWN_HTTP_METHOD, UnknownHttpMethodProcessor.METHOD_NOT_ALLOWED);
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
            resourcePreloadSizeLimit = get(RESOURCE_PRELOAD_SIZE_LIMIT, 1024 * 10);
            if (resourcePreloadSizeLimit <= 0) {
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

    private Boolean resourceFiltering;

    protected T resourceFiltering(boolean b) {
        resourceFiltering = b;
        return me();
    }

    public boolean resourceFiltering() {
        if (null == resourceFiltering) {
            if (app.isDev()) {
                resourceFiltering = get(RESOURCE_FILTERING, true);
            } else {
                resourceFiltering = false;
            }
        }
        return resourceFiltering;
    }

    private void _mergeResourceFiltering(AppConfig conf) {
        if (!hasConfiguration(RESOURCE_FILTERING)) {
            this.resourceFiltering = conf.resourceFiltering;
        }
    }

    private static final ResourceBundle.Control DEF_RBC = ResourceBundle.Control.getControl(FORMAT_DEFAULT);
    private String resourceBundleEncoding = null;
    protected T resourceBundleEncoding(String encoding) {
        resourceBundleEncoding = encoding;
        return me();
    }
    public String resourceBundleEncoding() {
        if (null == resourceBundleEncoding) {
            resourceBundleEncoding = get(RESOURCE_BUNDLE_ENCODING, "default");
        }
        return resourceBundleEncoding;
    }
    private void _mergeResourceBundleEncoding(AppConfig conf) {
        if (!hasConfiguration(RESOURCE_BUNDLE_ENCODING)) {
            this.resourceBundleEncoding = conf.resourceBundleEncoding;
            this.resourceBundleControl = null;
        }
    }
    private ResourceBundle.Control resourceBundleControl;
    public ResourceBundle.Control resourceBundleControl() {
        synchronized (DEF_RBC) {
            if (null != resourceBundleControl) {
                return resourceBundleControl;
            }
            final String encoding = resourceBundleEncoding();
            if ("default".equals(encoding)) {
                resourceBundleControl = DEF_RBC;
            } else {
                resourceBundleControl = new ResourceBundle.Control() {
                    @Override
                    public ResourceBundle newBundle(
                            String baseName, Locale locale,
                            String format, ClassLoader loader,
                            boolean reload
                    ) throws IllegalAccessException, InstantiationException, IOException {
                        // The below is a copy of the default implementation.
                        String bundleName = toBundleName(baseName, locale);
                        String resourceName = toResourceName(bundleName, "properties");
                        ResourceBundle bundle = null;
                        InputStream stream = null;
                        if (reload) {
                            URL url = loader.getResource(resourceName);
                            if (url != null) {
                                URLConnection connection = url.openConnection();
                                if (connection != null) {
                                    connection.setUseCaches(false);
                                    stream = connection.getInputStream();
                                }
                            }
                        } else {
                            stream = loader.getResourceAsStream(resourceName);
                        }
                        if (stream != null) {
                            try {
                                // Only this line is changed to make it to read properties files as UTF-8.
                                bundle = new PropertyResourceBundle(new InputStreamReader(stream, encoding));
                            } finally {
                                stream.close();
                            }
                        }
                        return bundle;
                    }
                };
            }
        }
        return resourceBundleControl;
    }

    private Integer uploadInMemoryCacheThreshold;

    protected T uploadInMemoryCacheThreshold(int l) {
        uploadInMemoryCacheThreshold = l;
        return me();
    }

    public int uploadInMemoryCacheThreshold() {
        if (null == uploadInMemoryCacheThreshold) {
            uploadInMemoryCacheThreshold = get(UPLOAD_IN_MEMORY_CACHE_THRESHOLD, 1024 * 10);
        }
        return uploadInMemoryCacheThreshold;
    }

    private void _mergeUploadInMemoryCacheThreshold(AppConfig config) {
        if (!hasConfiguration(UPLOAD_IN_MEMORY_CACHE_THRESHOLD)) {
            uploadInMemoryCacheThreshold = config.uploadInMemoryCacheThreshold;
        }
    }

    private Boolean ssl;

    protected T supportSsl(boolean b) {
        ssl = b;
        return me();
    }

    public boolean supportSsl() {
        if (null == ssl) {
            ssl = get(SSL, false);
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
            wsTicketKey = get(WS_KEY_TICKET, "ws_ticket");
        }
        return wsTicketKey;
    }

    private void _mergeWsTicketKey(AppConfig config) {
        if (!hasConfiguration(WS_KEY_TICKET)) {
            wsTicketKey = config.wsTicketKey;
        }
    }

    private Integer wsPurgeClosedConnPeriod;

    protected T wsPurgeClosedConnPeriod(int period) {
        this.wsPurgeClosedConnPeriod = period;
        return me();
    }

    public int wsPurgeClosedConnPeriod() {
        if (null == wsPurgeClosedConnPeriod) {
            wsPurgeClosedConnPeriod = get(WS_PURGE_CLOSED_CONN_PERIOD, Act.isDev() ? 1 : 10);
        }
        return wsPurgeClosedConnPeriod;
    }

    private void _mergeWsPurgeClosedConnPeroid(AppConfig config) {
        if (!hasConfiguration(WS_PURGE_CLOSED_CONN_PERIOD)) {
            wsPurgeClosedConnPeriod = config.wsPurgeClosedConnPeriod;
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
        app.emit(SysEventId.CONFIG_PREMERGE);
        if (mergeTracker.contains(conf)) {
            return;
        }
        mergeTracker.add(conf);
        for (Method method : AppConfig.class.getDeclaredMethods()) {
            boolean isPrivate = Modifier.isPrivate(method.getModifiers());
            if (isPrivate && method.getName().startsWith("_merge")) {
                method.setAccessible(true);
                $.invokeVirtual(this, method, conf);
            }
        }


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


    private ConcurrentMap<$.T2<Locale, DateTimeType>, String> localizedDateTimePatterns0 = new ConcurrentHashMap<>();

    private String getLocalizedDateTimePattern(Locale locale, DateTimeType dateTimeType) {
        $.T2<Locale, DateTimeType> key = $.T2(locale, dateTimeType);
        String s = localizedDateTimePatterns0.get(key);
        if (null != s) {
            return s;
        }
        String localStr = locale.toString().toLowerCase();
        String conf = S.concat("fmt.", localStr, dateTimeType.suffix());
        s = get(conf);
        if (null == s) {
            boolean canSplit = localStr.contains("_");
            if (canSplit) {
                s = get(S.concat("fmt.", S.replace('_').with('-').in(localStr), dateTimeType.suffix()));
                if (null == s) {
                    s = get(S.concat("fmt.", S.cut(localStr).after("_"), dateTimeType.suffix()));
                    if (null == s) {
                        s = get(S.concat("fmt.", S.cut(localStr).before("_"), dateTimeType.suffix()));
                    }
                }
            }
        }
        if (null == s) {
            if (isLocaleMatchesDefault(locale)) {
                s = dateTimeType.defaultPattern(this);
            } else {
                DateTimeStyle style = dateTimeStyle();
                if (dateTimeType == DateTimeType.DATE) {
                    style = dateStyle();
                } else if (dateTimeType == DateTimeType.TIME) {
                    style = timeStyle();
                }
                s = dateTimeType.defaultPattern(style, locale);
            }
        } else {
            if (S.eq("long", s, S.IGNORECASE)) {
                s = dateTimeType.defaultPattern(DateTimeStyle.LONG, locale);
            } else if (S.eq("medium", s, S.IGNORECASE)) {
                s = dateTimeType.defaultPattern(DateTimeStyle.MEDIUM, locale);
            } else if (S.eq("short", s, S.IGNORECASE)) {
                s = dateTimeType.defaultPattern(DateTimeStyle.SHORT, locale);
            }
        }
        localizedDateTimePatterns0.putIfAbsent(key, s);
        return s;
    }


    private volatile DateTimeStyle dateTimeStyle;

    public DateTimeStyle dateTimeStyle() {
        if (null == dateTimeStyle) {
            synchronized (this) {
                if (null == dateTimeStyle) {
                    dateTimePattern();
                }
            }
        }
        return dateTimeStyle;
    }

    private volatile DateTimeStyle dateStyle;

    public DateTimeStyle dateStyle() {
        if (null == dateStyle) {
            synchronized (this) {
                if (null == dateStyle) {
                    datePattern();
                }
            }
        }
        return dateStyle;
    }

    private volatile DateTimeStyle timeStyle;

    public DateTimeStyle timeStyle() {
        if (null == timeStyle) {
            synchronized (this) {
                if (null == timeStyle) {
                    timePattern();
                }
            }
        }
        return timeStyle;
    }

    public void setDefaultTldReloadCron() {
        raw.put(canonical(TopLevelDomainList.CRON_TLD_RELOAD), "0 0 2 * * *");
    }
}
