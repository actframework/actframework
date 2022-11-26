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
import act.app.App;
import act.controller.annotation.Throttled;
import act.handler.UnknownHttpMethodProcessor;
import act.validation.Password;
import act.view.TemplatePathResolver;
import act.view.View;
import act.ws.DefaultSecureTicketCodec;
import act.ws.SecureTicketCodec;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.*;

/**
 * {@link App} configuration keys. General rules:
 * <p/>
 * <ul>
 * <li>When a key is ended with <code>.enabled</code>, then you should be able to set
 * the setting without <code>.enabled</code> or replace it with <code>.disabled</code>
 * but the value will be inverted. For example, <code>built_in.transformer.enabled</code>
 * is equal to <code>built_in.transformer</code> and invert to
 * <code>built_in.transformer.disabled</code></li>
 * <p/>
 * <li>When a key is ended with <code>.impl</code>, then you can either put an instance into
 * the configuration map or a string of the class className</li>
 * </ul>
 */
public enum AppConfigKey implements ConfigKey {

    /**
     * `act.api_doc.enabled` turns on/off API doc feature
     *
     * When API doc is enabled, developer can access the app's API document
     * through `GET /~/apidoc`
     *
     * Default value: `true` when app running in `dev` mode, or `false` otherwise
     */
    API_DOC_ENABLED("api_doc.enabled"),

    /**
     * `act.api_doc.built_in.hide` turns on/off built-in endpoints in
     * API doc.
     *
     * Default value: `false`
     */
    API_DOC_HIDE_BUILT_IN_ENDPOINTS("api_doc.built_in.hide.enabled"),

    /**
     * `app.name` the application name.
     *
     * Default value: {@link App#name()}
     */
    APP_NAME("app.name"),

    /**
     * {@code act.basic_authentication.enabled} turn on/off Basic Authentication
     * in Act application.
     *
     * Default value: `false`
     *
     * **Note** there is no logic around this configuration in the core
     * ActFramework. It is up to the security plugins like `act-aaa-plugin`
     * to favor the value of this setting
     */
    BASIC_AUTHENTICATION("basic_authentication.enabled"),

    /**
     * `built_in_req_handler.enabled` turn on/off built in request
     * handlers.
     *
     * Default value: `true`
     */
    BUILT_IN_REQ_HANDLER_ENABLED("built_in_req_handler.enabled"),


    /**
     * {@code act.cache.impl}
     * Specify {@link org.osgl.cache.CacheServiceProvider Cache service provider}
     * <p>Default value: {@link org.osgl.cache.CacheServiceProvider.Impl#Simple the simple
     * in memory map based cache service implementation}</p>
     */
    CACHE_IMPL("cache.impl"),

    /**
     * `act.cache.ttl` specifies the default TTL for default cache.
     *
     * Default value: 60s
     */
    CACHE_TTL("cache.ttl"),

    /**
     * {@code act.cache.name}
     *
     * Specify the default cache name
     *
     * Default value: `_act_app_`
     */
    CACHE_NAME("cache.name"),

    /**
     * {@code act.cache.name.session}
     *
     * Specify the session cache name
     *
     * Default value: the value configured by {@link #CACHE_NAME}
     */
    CACHE_NAME_SESSION("cache.name.session"),

    /**
     * `cacheFor.dev.enabled`
     *
     * Specify whether `@CacheFor` annotation effective on `dev` mode.
     *
     * Default value: `false`
     */
    CACHE_FOR_ON_DEV("cacheFor.dev.enabled"),

    /**
     * `captcha.width`
     *
     * Specify the width of the captcha image
     *
     * Default value: 200
     */
    CAPTCHA_WIDTH("captcha.width"),

    /**
     * `captcha.height`
     *
     * Specify the height of the captcha image
     *
     * Default value: 70
     */
    CAPTCHA_HEIGHT("captcha.height"),

    /**
     * `captcha.background.color`
     *
     * Specify the background color of the captcha image.
     *
     * It shall be valid color constant name defined in {@link java.awt.Color} class.
     *
     * Default value: white
     */
    CAPTCHA_BG_COLOR("captcha.background.color"),

    /**
     * `captcha.recaptcha.secret` - specifies your
     * site's google reCAPTCHA secret.
     *
     * Once this is setup, then it enables reCAPTCHA server
     * side verification.
     */
    CAPTCHA_RECAPTCHA_SECRET("captcha.recaptcha.secret"),

    /**
     * {@code act.cli.enabled}
     *
     * Turn on/off CLI server support
     *
     * Default value: `true`
     */
    CLI_ENABLED("cli.enabled"),

    /**
     * {@code act.cli.port} specifies the default cli (telnet) port the application
     * listen to.
     * <p>Default value: {@code 5461}</p>
     */
    CLI_PORT("cli.port"),

    /**
     * {@code act.cli.json.page.size}
     * Specify the maximum records in one page for JSON layout by CLI command
     *
     * Default value: 10
     */
    CLI_PAGE_SIZE_JSON("cli.page.size.json"),

    /**
     * {@code act.cli.table.page.size}
     * Specify the maximum records in one page for table layout by CLI command
     *
     * Default value: 22
     */
    CLI_PAGE_SIZE_TABLE("cli.page.size.table"),

    /**
     * {@code act.cli.progress-bar.style}
     * Specify the CLI progress bar style, available styles are:
     *
     * * block
     * * ascii
     *
     * Default value: `block`
     */
    CLI_PROGRESS_BAR_STYLE("cli.progress-bar.style"),

    /**
     * {@code cli.session.ttl} specifies the number of seconds
     * a cli session can exists after last user interaction
     *
     * <p>Default value: {@code 300} seconds. e.g. 5 minutes</p>
     */
    CLI_SESSION_TTL("cli.session.ttl.int"),

    /**
     * {@code cli.session.max} specifies the maximum number of cli threads
     * can exists concurrently
     * <p>Default value: {@code 3}</p>
     */
    CLI_SESSION_MAX("cli.session.max.int"),


    /**
     * `act.cli_over_http.enabled` turn on/off CLI over http feature, which
     * allows ActFramework to handle http request sent through to the  {@link #CLI_OVER_HTTP_PORT}
     * as a way to invoke CLI commands and inspect results
     *
     * Default value: `false`
     */
    CLI_OVER_HTTP("cli_over_http.enabled"),

    /**
     * `act.cli_over_http.authority` specifies the {@link act.cli.CliOverHttpAuthority} implementation
     */
    CLI_OVER_HTTP_AUTHORITY("cli_over_http.authority.impl"),

    /**
     * `act.cli_over_http.port` specifies the default cli over http port the application
     * listen to.
     *
     * Default value: `5462`
     */
    CLI_OVER_HTTP_PORT("cli_over_http.port"),

    /**
     * `act.cli_over_http.port` specify the title to be displayed on the CLI Over Http
     * page
     *
     * Default value: "Cli Over Http"
     */
    CLI_OVER_HTTP_TITLE("cli_over_http.title"),

    /**
     * `act.cli_over_http.syscmd.enabled` turn on/off system command on CLI Over Http
     * page
     *
     * Default value: `true`
     */
    CLI_OVER_HTTP_SYS_CMD("cli_over_http.syscmd.enabled"),

    /**
     * `conf-server.endpoint` specify the remote configuration server endpoint.
     *
     * Once this is set when app configuration initialised it will send a GET
     * request to `${conf-server.endpoint}?id=${conf.id}
     *
     * Note the endpoint must be a full URL
     *
     * Default value: `null`
     */
    CONF_SERVER_ENDPOINT("conf-server.endpoint"),

    /**
     * `conf-loader.impl` specify customized application configuration loader.
     * it should be a class name of an implementation of {@link ExtendedAppConfLoader}.
     *
     * Default value: `null`
     */
    CONF_LOADER("conf-loader.impl"),

    /**
     * `conf.id` set the configuration id - could be used to fetch configuration from configuration server
     *
     * Default value: ${app.name}-${profile}
     */
    CONF_ID("conf.id"),

    /**
     * `conf.private-key` specifies the private key app used to decrypt
     * the respond coming from configuration server.
     *
     * Default value: null
     */
    CONF_PRIVATE_KEY("conf.private-key"),

    /**
     * `act.cookie.domain_provider.impl` specify the provider
     * that provides the cookie domain name
     *
     * Default value: value of {@link #HOST}
     */
    COOKIE_DOMAIN_PROVIDER("cookie.domain_provider.impl"),

    /**
     * {@code cookie.prefix} specifies the prefix to be prepended
     * to the different cookie names e.g. session cookie, flash cookie,
     * locale cookie etc. Let's say the default cookie name is
     * {@code act_session}, and user specifies the prefix {@code my_app}
     * then the session cookie name will be {@code my_app_session}.
     * <p>Note this setting also impact the {@link AppConfig#flashCookieName()}</p>
     * <p>Default value: {@link App#shortId()}</p>
     */
    COOKIE_PREFIX("cookie.prefix"),

    /**
     * {@code act.cors.enabled} turn on/off CORS in Act application
     *
     * Default value: `false`
     */
    CORS("cors.enabled"),

    /**
     * `act.cors.option.check` specify whether the framework should
     * check the current request is an HTTP OPTION method before applying
     * controller headers or not
     *
     * default value: `true`
     */
    CORS_CHECK_OPTION_METHOD("cors.option.check.enabled"),

    /**
     * {@code act.cors.origin} specifies `Access-Control-Allow-Origin` header
     * to be output
     *
     * Default value: `*`
     */
    CORS_ORIGIN("cors.origin"),

    /**
     * {@code act.cors.headers} specifies both `Access-Control-Expose-Headers`
     * and `Access-Control-Allow-Headers`
     *
     * This configuration is deprecated, it is replaced by
     *
     * * {@link #CORS_HEADERS_EXPOSE}
     * * {@link #CORS_HEADERS_ALLOWED}
     *
     * Default value: `Content-Type, X-HTTP-Method-Override`
     */
    @Deprecated
    CORS_HEADERS("cors.headers"),

    /**
     * {@code act.cors.headers.expose} specify `Access-Control-Expose-Headers`.
     * Note this setting will overwrite the setting of {@link #CORS_HEADERS} if
     * it is set
     *
     * Default value: `Act-Session-Expires, Authorization, X-XSRF-Token, X-CSRF-Token, Location, Link, Content-Disposition, Content-Length`
     */
    CORS_HEADERS_EXPOSE("cors.headers.expose"),

    /**
     * {@code act.cors.headers.allowed} specify `Access-Control-Allow-Headers`.
     * Note this setting will overwrite the setting of {@link #CORS_HEADERS} if
     * it is set
     *
     * Default value: `X-HTTP-Method-Override, X-Requested-With, Authorization, X-XSRF-Token, X-CSRF-Token`
     */
    CORS_HEADERS_ALLOWED("cors.headers.allowed"),

    /**
     * {@code act.cors.max_age} specifies `Access-Control-Max-Age`.
     *
     * Default value: 30*60 (seconds)
     */
    CORS_MAX_AGE("cors.max_age"),

    /**
     * `act.cors.allow_credentials` specifies `Access-Control-Allow-Credentials`.
     *
     * Default value: `false`
     */
    CORS_ALLOW_CREDENTIALS("cors.allow_credentials.enabled"),

    /**
     * {@code act.content_suffix.aware.enabled}
     * <p>
     *     Once enabled then the framework automatically recognize request with content suffix.
     *     E.g. {@code /customer/123/json} will match the route {@code /customer/123}
     *     and set the request {@code Accept} header to
     *     {@code application/json}
     * </p>
     * <p>Default value: {@code false}</p>
     */
    CONTENT_SUFFIX_AWARE("content_suffix.aware.enabled"),

    /**
     * `act.csp` - global Content-Security-Policy header setting
     *
     * Default value: null
     */
    CONTENT_SECURITY_POLICY("csp"),

    /**
     * {@code act.csrf.enabled} turn on/off global CSRF protect
     *
     * Default value: `true`
     */
    CSRF("csrf.enabled"),

    /**
     * {@code act.csrf.param_name} specifies the http request param name
     * used to convey the csrf token
     *
     * Default value: the value of {@link AppConfig#CSRF_TOKEN_NAME}
     */
    CSRF_PARAM_NAME("csrf.param_name"),

    /**
     * {@code act.csrf.header_name} specifies name of the http request
     * header used to convey the csrf token sent from AJAX client.
     *
     * Default value: `X-Xsrf-Token` - the name used by AngularJs
     */
    CSRF_HEADER_NAME("csrf.header_name"),

    /**
     * {@code act.csrf.cookie_name} specify the name of the cookie used
     * to convey the csrf token generated on the server for the first GET
     * request coming from a client.
     *
     * Default value: `XSRF-TOKEN` - the name used by AngularJs
     */
    CSRF_COOKIE_NAME("csrf.cookie_name"),

    /**
     * `act.csrf.protector.impl` specifies the implementation of
     * {@link act.security.CSRFProtector}.
     *
     * The value of this configuration could be either a name of
     * the class that implements {@link act.security.CSRFProtector}
     * interface, or the enum name of {@link act.security.CSRFProtector.Predefined}
     *
     * Default value: `HMAC` which specifies the {@link act.security.CSRFProtector.Predefined#HMAC}
     */
    CSRF_PROTECTOR("csrf.protector.impl"),

    /**
     * `act.db.seq_gen.impl` specifies the implementation of
     * {@link act.db.util._SequenceNumberGenerator}.
     *
     * Default value: `null`
     */
    DB_SEQ_GENERATOR("db.seq_gen.impl"),

    /**
     * `dsp.token` specifies the name of "double submission protect token"
     *
     * Default value: `act_dsp_token`
     */
    DOUBLE_SUBMISSION_PROTECT_TOKEN("dsp.token"),

    /**
     * {@code act.encoding} specifies application default encoding
     * <p>Default value: utf-8</p>
     */
    ENCODING("encoding"),

    /**
     * `act.enum.resolving.case_sensitive` specifies whether it
     * allow enum resolving for request parameters to ignore case
     *
     * Default value: `false` meaning enum resolving is case insensitive
     *
     * This is deprecated since v1.8.8, use {@link #ENUM_RESOLVING_EXACT_MATCH}
     * instead
     */
    @Deprecated
    ENUM_RESOLVING_CASE_SENSITIVE("enum.resolving.case_sensitive"),

    /**
     * `act.enum.resolving.exact_match` specifies whether it
     * allow enum resolving for request parameters to match enum name
     * exactly.
     *
     * Default value: `false`, meaning enum resolving is based on keyword matching
     */
    ENUM_RESOLVING_EXACT_MATCH("enum.resolving.exact_match"),

    /**
     * {@code act.fmt.date} specifies the default date format used to
     * lookup/output the date string
     * <p>Default value: the pattern of {@code java.text.DateFormat.getDateInstance()}</p>
     */
    FORMAT_DATE("fmt.date"),

    /**
     * {@code act.fmt.date} specifies the default date and time format used to
     * lookup/output the date string
     * <p>Default value: the pattern of {@code java.text.DateFormat.getDateTimeInstance()}</p>
     */
    FORMAT_DATE_TIME("fmt.date_time"),

    /**
     * {@code act.fmt.time} specifies the default time format used to
     * lookup/output the date time string
     * <p>Default value: the pattern of {@code java.text.DateFormat.getTimeInstance()}</p>
     */
    FORMAT_TIME("fmt.time"),

    /**
     * `globalReturnValueAdvice` specifies the global {@link act.handler.ReturnValueAdvice}
     * type.
     *
     * Default value: null
     */
    GLOBAL_RETURN_VALUE_ADVICE("globalReturnValueAdvice"),

    /**
     * `globalValidateViolationAdvice` specifies the global {@link act.handler.ValidateViolationAdvice}
     *  type.
     *
     *  Default value: null
     */
    GLOBAL_VALIDATE_VIOLATION_ADVICE("globalValidateViolateAdvice"),

    /**
     * `act.handler.csrf_check_failure.impl` specifies the implementation
     * for {@link act.util.MissingAuthenticationHandler}
     *
     * Default value: {@link act.util.RedirectToLoginUrl}
     */
    HANDLER_CSRF_CHECK_FAILURE("handler.csrf_check_failure.impl"),

    /**
     * `act.handler.csrf_check_failure.ajax.impl` specifies the implementation for
     * {@link act.util.MissingAuthenticationHandler} dealing with the case of AJAX
     * request
     *
     * Default value: the value of {@link #HANDLER_CSRF_CHECK_FAILURE}
     */
    HANDLER_AJAX_CSRF_CHECK_FAILURE("handler.csrf_check_failure.ajax.impl"),

    /**
     * {@code handler.missing_authentication.impl} specifies the implementation
     * for {@link act.util.MissingAuthenticationHandler}
     * <p>Default value: {@link act.util.RedirectToLoginUrl}</p>
     */
    HANDLER_MISSING_AUTHENTICATION("handler.missing_authentication.impl"),

    /**
     * {@code handler.missing_authentication.ajax.impl} specifies the implementation
     * for {@link act.util.MissingAuthenticationHandler} dealing with the case of AJAX
     * request
     * <p>Default value: the value of {@link #HANDLER_MISSING_AUTHENTICATION}</p>
     */
    HANDLER_MISSING_AUTHENTICATION_AJAX("handler.missing_authentication.ajax.impl"),

    /**
     * {@code unknown_http_method_handler} specifies a class/instance that
     * implements {@link UnknownHttpMethodProcessor} that process
     * the HTTP methods that are not recognized by {@link act.route.Router},
     * e.g. "OPTION", "PATCH" etc
     *
     * Default value: {@link UnknownHttpMethodProcessor#METHOD_NOT_ALLOWED}
     */
    HANDLER_UNKNOWN_HTTP_METHOD("handler.unknown_http_method.impl"),

    /**
     * `header.session.expiration` specifies the session expiration header name.
     *
     * This is only effective when {@link #SESSION_OUTPUT_EXPIRATION} is effective.
     *
     * Default value: `Act-Session-Expires`
     */
    HEADER_SESSION_EXPIRATION("header.session.expiration"),

    /**
     * `act.header.overwrite` turn on/off HTTP HEADER overwrite.
     *
     * Once this config is turned on, then it can overwrite header
     * with HTTP Query parameter or HTTP post form field. The naming
     * convention of the param/field is:
     *
     * ```
     * act_header_<header_name_in_lowercase_and_underscore>
     * ```
     *
     * For example, if it needs to overwrite `Content-Type`, use
     * `act_header_content_type` as the query parameter name.
     *
     * Default value: `false`
     */
    HEADER_OVERWRITE("header.overwrite.enabled"),

    /**
     * {@code act.host} specifies the host the application
     * reside on.
     * <p/>
     * <p>Default value: {@code localhost}</p>
     */
    HOST("host"),

    /**
     * `act.http.external_server.enabled` specify if the app is running behind a front end
     * http server
     *
     * Default value: `true` when running in PROD mode; `false` when running in DEV mode
     */
    HTTP_EXTERNAL_SERVER("http.external_server.enabled"),

    /**
     * {@code act.http.params.max} specifies the maximum number of http parameters
     * this is to prevent the hash collision DOS attack
     * <p>Default value: {@code 128}</p>
     */
    HTTP_MAX_PARAMS("http.params.max"),

    /**
     * {@code act.http.port} specifies the default http port the application
     * listen to
     * <p/>
     * <p>Default value: {@code 5460}</p>
     */
    HTTP_PORT("http.port"),

    /**
     * `act.http.port.external` set the external port which is used to
     * construct the full url.
     *
     * Note act does not listen to external port directly. The recommended
     * pattern is to have a front end HTTP server (e.g. nginx) to handle
     * the external request and forward to act
     *
     * Default value: `80`
     */
    HTTP_EXTERNAL_PORT("http.port.external"),

    /**
     * `act.http.port.external.secure` set the external secure port which is
     * used to construct full url string when app is running secure mode
     *
     * @see #HTTP_EXTERNAL_PORT
     */
    HTTP_EXTERNAL_SECURE_PORT("http.port.external.secure"),

    /**
     * {@code act.http.secure} specifies whether the default http port is
     * running https or http.
     * <p></p>
     * <p>
     *     Default value: {@code false} when Act is running in dev mode
     *     or {@code true} when Act is running in prod mode
     * </p>
     */
    @SuppressWarnings("unchecked")
    HTTP_SECURE("http.secure.enabled"),

    /**
     * `https.port`
     *
     * Specify the https port - only effect when {@link #SSL} is enabled
     *
     * Default value: `5443`
     */
    HTTPS_PORT("https.port"),

    /**
     * `act.i18n.enabled` turn on/off i18n tools, e.g. {@link act.i18n.LocaleResolver}
     *
     * Default value: `false`
     */
    I18N("i18n.enabled"),

    /**
     * `act.i18n.locale.param_name` specifies the param name to set client locale in http request
     *
     * Default value: `act_locale`
     */
    I18N_LOCALE_PARAM_NAME("i18n.locale.param_name"),

    /**
     * `act.i18n.locale.cookie_name` specifies the name for the locale cookie
     *
     * Default value: `act_locale`
     */
    I18N_LOCALE_COOKIE_NAME("i18n.locale.cookie_name"),

    /**
     * {@code act.idgen.node_id.provider.impl} specifies the {@link act.util.IdGenerator.NodeIdProvider}
     * implementation for {@link App#idGenerator}
     * <p>Default value: {@link act.util.IdGenerator.NodeIdProvider.IpProvider}</p>
     */
    ID_GEN_NODE_ID_PROVIDER("idgen.node_id.provider.impl"),

    /**
     * {@code act.idgen.node_id.effective_ip_bytes} specifies how many bytes in the ip address
     * will be used to calculate node ID. Usually in a cluster environment, the ip address will
     * be different at only (last) one byte or (last) two bytes, in which case it could set this
     * configuration to {@code 1} or {@code 2}. When the configuration is set to {@code 4} then
     * it means all 4 IP bytes will be used to calculate the node ID
     * <p>Default value: {@code 4}</p>
     */
    ID_GEN_NODE_ID_EFFECTIVE_IP_BYTES("idgen.node_id.effective_ip_bytes.size"),

    /**
     * {@code act.idgen.start_id.provider.impl} specifies the {@link act.util.IdGenerator.StartIdProvider}
     * implementation for {@link App#idGenerator}
     * <p>Default value: {@link act.util.IdGenerator.StartIdProvider.DefaultStartIdProvider}</p>
     */
    ID_GEN_START_ID_PROVIDER("idgen.start_id.provider.impl"),

    /**
     * {@code act.idgen.start_id.file} specifies the start id persistent file for
     * {@link act.util.IdGenerator.StartIdProvider.FileBasedStartCounter}
     * <p>Default value: {@code .act.id-app}</p>
     */
    ID_GEN_START_ID_FILE("idgen.start_id.file"),

    /**
     * {@code act.idgen.seq_id.provider.impl} specifies the {@link act.util.IdGenerator.SequenceProvider}
     * implementation for {@link App#idGenerator}
     *
     * Default value: {@link act.util.IdGenerator.SequenceProvider.AtomicLongSeq}
     */
    ID_GEN_SEQ_ID_PROVIDER("idgen.seq_id.provider.impl"),

    /**
     * {@code act.idgen.encoder.impl} specifies the {@link act.util.IdGenerator.LongEncoder}
     * implementation for {@link App#idGenerator}
     * <p>Default value: {@link act.util.IdGenerator.SafeLongEncoder}</p>
     */
    ID_GEN_LONG_ENCODER("idgen.encoder.impl"),

    /**
     * {@code job.pool.size} specifies the maximum number of threads
     * can exists in the application's job manager's thread pool
     * <p>Default value: {@code 10}</p>
     */
    JOB_POOL_SIZE("job.pool.size"),

    /**
     * `jwt.enabled`, toggle JWT (JSON Web Token) support.
     *
     * Enable this configuration has the same effect of setting
     *
     * * {@link #SESSION_CODEC} - {@link act.session.JsonWebTokenSessionCodec}
     * * {@link #SESSION_HEADER_PAYLOAD_PREFIX} - `Bearer `
     * * {@link #SESSION_HEADER} - `Authorization`
     *
     * Default value: `false`
     */
    JWT("jwt.enabled"),

    /**
     * `jwt.algo`, specify JWT sign algorithm.
     *
     * Available options:
     * * SHA256
     * * SHA384
     * * SHA512
     *
     * Default value: SHA256
     *
     */
    JWT_ALGO("jwt.algo"),

    /**
     * `jwt.issuer`, specify `iss` payload of JWT
     *
     * Default value: {@link #COOKIE_PREFIX}
     */
    JWT_ISSUER("jwt.issuer"),

    /**
     * `json_body_patch` - enable/disable JSON body patch
     *
     * Default value: `true`
     */
    JSON_BODY_PATCH("json_body_patch.enabled"),

    /**
     * {@code act.locale} specifies the application default locale
     * <p>Default value: {@link java.util.Locale#getDefault}</p>
     */
    LOCALE("locale") {
        @Override
        public <T> T val(Map<String, ?> configuration) {
            Object o = super.val(configuration);
            if (null == o) {
                return null;
            }
            if (o instanceof String) {
                return (T) Locale.forLanguageTag((String) o);
            } else if (o instanceof Locale) {
                return (T) o;
            } else {
                String s = o.toString();
                return (T) Locale.forLanguageTag(s);
            }
        }
    },

    /**
     * {@code act.metric.enabled}
     * Turn on/off metric in Act application
     *
     * Default value: {@code true}
     */
    METRIC_ENABLED("metric.enabled"),

    /**
     * `mock-server.enabled`
     * Turn on/off mock data to API endpoint
     *
     * Default value: `true` when run in Dev mode, `false` otherwise
     */
    MOCK_SERVER_ENABLED("mock-server.enabled"),

    /**
     * {@code act.modules}
     *
     * Declare additional app base (for maven modules)
     *
     * Default value: `null`
     */
    MODULES("modules"),

    /**
     * `act.monitor`
     *
     * When `act.monitor` is turned on then it will load the monitor thread
     *
     * Default value: `false``
     */
    MONITOR("monitor.enabled"),

    /**
     * {@code act.namedPorts} specifies a list of port names this
     * application listen to. These are additional ports other than
     * the default {@link #HTTP_PORT}
     *
     * The list is specified as
     *
     * ```
     * act.namedPorts=admin:8888;ipc:8899
     * ```
     *
     * Default value: `null`
     *
     * Note, the default port that specified in {@link #HTTP_PORT} configuration
     * and shall not be specified in this namedPorts configuration
     */
    NAMED_PORTS("namedPorts"),

    /**
     * `threadlocal_buf.limit` set the maximum size of thread local instance
     * of {@link S.Buffer} and {@link org.osgl.util.ByteArrayBuffer} before it
     * get dropped.
     *
     * Default value: 1024 * 8 (i.e. 8k)
     */
    OSGL_THREADLOCAL_BUF_LIMIT("threadlocal_buf.limit"),

    /**
     * `param_binding.keyword_matching` turn on/off keyword matching in HTTP param
     * binding process.
     *
     * When this configuration is turned on the framework is able to do keyword matching
     * to bind the HTTP parameter, e.g. when it declare to bind a parameter named `fooBar`,
     * when request is sending with parameter named `foo_bar`, it can still finish the bind.
     *
     * **Note** turning on this configuration might cause slightly performance degrade.
     *
     * Default value: `false`
     */
    PARAM_BINDING_KEYWORD_MATCHING("param_binding.keyword_matching.enabled"),

    /**
     * `password.spec` specify default password spec which is used to
     * validate user password.
     *
     * Default value:
     *
     * * dev mode: `a[3,]`, meaning require lower case letter and min length is 3 characters.
     * * prod mode: `aA0[6,]`, meaning require lower case letter, uppercase letter,
     * digit and min length is 6 characters.
     *
     * Developer can also specify a {@link Password.Validator} implementation
     * class for this configuration, in which case, the framework will instantiate the user
     * specified validator instead of {@link act.validation.PasswordSpec} as the default
     * password validator.
     *
     * @see act.validation.PasswordSpec#parse(String)
     */
    PASSWORD_DEF_SPEC("password.spec"),

    /**
     * {@code ping.path} specify the ping path.
     * If this setting is specified, then when session resolving, system
     * will check if the current URL matches the setting. If matched
     * then session cookie expiration time will not be changed. Otherwise
     * the expiration time will refresh
     * <p>Default value: {@code null}</p>
     */
    PING_PATH("ping.path"),

    /**
     * {@code profile} specifies the profile to load configuration
     * If this setting is specified, and there is a folder named as
     * the {@code profile} setting sit under {@code /resource/conf}
     * folder, then the properties files will be loaded from
     * that folder.
     * <p>Default value: the value of the {@link Act#mode()}</p>
     * <p>Note, unlike other configuration items which is usually specified
     * in the configuration file. {@code profile} setting is load
     * by {@link System#getProperty(String)}</p>, thus it is usually
     * specified with JVM argument {@code -Dprofile=<profile>}
     */
    PROFILE("profile"),

    /**
     * `req.throttle` specifies the maximum number of requests
     * that can be handled per second from the same ip address
     * when {@link Throttled}
     * is specified on the action handler.
     *
     * Default value: `2`
     */
    REQUEST_THROTTLE("req.throttle.int"),

    /**
     * `req.throttle.expire.scale` - whether increase throttle reset
     * expire time incrementally.
     *
     * Default value: `false`
     */
    REQUEST_THROTTLE_EXPIRE_SCALE("req.throttle.expire.scale.enabled"),

    /**
     * `render.json.output_charset`
     *
     * Specifies output charset in `application/json` response header `Content-Type`
     *
     * Default value: `false`
     */
    RENDER_JSON_OUTPUT_CHARSET("render.json.output_charset.enabled"),

    /**
     * `render.json.content_type.ie`
     *
     * Internet Explorer is know to have an issue with `application/json` content type.
     * if this configuration is set, the framework will output Content-Type header using
     * the setting when the request is detected as initialized from IE browser.
     *
     * Default value: `null`
     */
    RENDER_JSON_CONTENT_TYPE_IE("render.json.content_type.ie"),


    /**
     * {@code resolver.error_template_path.impl} specifies error page (template)
     * path resolver implementation
     * <p>Default value: {@code act.util.ErrorTemplatePathResolver.DefaultErrorTemplatePathResolver}</p>
     */
    RESOLVER_ERROR_TEMPLATE_PATH("resolver.error_template_path.impl"),


    /**
     * {@code resolver.template_path.impl} specifies the class that
     * extends {@link TemplatePathResolver}. Application
     * developer could use this configuration to add some flexibility to
     * template path resolving logic, e.g. different home for different locale
     * or different home for different device type etc.
     * <p/>
     * <p>Default value: {@link TemplatePathResolver}</p>
     */
    RESOLVER_TEMPLATE_PATH("resolver.template_path.impl"),

    /**
     * `resource_bundle.encoding` specifies encoding of resource bundles.
     *
     * This configuration allows override the default resource bundle
     * encoding setting used by specific Java runtime:
     * - Before Java 9: ISO-8859-1
     * - Java 9 and above: UTF-8
     *
     * Default value: `null` meaning follow JDK default encoding setting
     */
    RESOURCE_BUNDLE_ENCODING("resource_bundle.encoding"),

    /**
     *  `resource.filtering`
     *
     *  Enable/disable resource filtering (only impact dev mode)
     *
     *  Default value: `true`
     */
    RESOURCE_FILTERING("resource.filtering.enabled"),

    /**
     * `resource.preload.size.limit`
     *
     * Specifies the maximum number of bytes of a resource that can be preload into memory.
     * Specifies `0` or negative number to disable resource preload feature
     *
     * Default value: `1024 * 10`, i.e. 10KB
     */
    RESOURCE_PRELOAD_SIZE_LIMIT("resource.preload.size.limit.int"),

    /**
     * {@code scan_package}
     * Specify the app package in which all classes is subject
     * to bytecode processing, e.g enhancement and injection.
     * This setting should be specified when application loaded.
     * Otherwise Act will try to process all classes found in
     * application's lib and classes folder, which might cause
     * performance issue on loading
     */
    SCAN_PACKAGE("scan_package"),

    /**
     * `scan_package.sys`
     *
     * **Note** Not to be used by application.
     *
     * This is Used by ActFramework only. When app started it will either
     * get the specified scan package from parameter, or infer scan package
     * from calling class.
     */
    SCAN_PACKAGE_SYS("scan_package.sys"),

    /**
     * {@code secret}
     * Specifies the secret key the application used to do general
     * encrypt/decrypt/sign etc
     * <p>Default value: {@code myawesomeapp}</p>
     */
    SECRET("secret"),

    /**
     * `secret.rotate.enabled` turn on app secret rotation for session/flash
     * token signing and encrypt.
     *
     * Default value: `false`
     */
    SECRET_ROTATE("secret.rotate.enabled"),

    /**
     * `secret.rotate.period` set the secret rotate period in terms of minute.
     *
     * **Note** the number of minute must be a factor of 60. Any number that
     * is not the factor of 60 then it will be up rounded:
     *
     * * 1 -> 1
     * * 2 -> 2
     * * 3 -> 4
     * * 4 -> 4
     * * 5 -> 5
     * * 6 -> 6
     * * 7 -> 10
     * * 8 -> 10
     * * 33 -> 30
     * * 50 -> 60
     *
     * the rotation period less than hour will be count from the beginning of
     * the current hour.
     *
     * If the number minutes exceeds 60, then it must be a factor of 60 * 24. Any
     * number if not will be rounded:
     *
     * * 65 -> 60
     * * 60 * 3 -> 60 * 3
     * * 60 * 5 -> 60 * 6
     * * 60 * 7 -> 60 * 6
     * * 60 * 10 -> 60 * 12 (half day)
     *
     * if the number of minutes equals of exceeds 120, the rotation period will
     * be counted from the beginning of the day.
     *
     * The maximum period is `60 * 24`, i.e. 24 hours. Any setting exceed that number
     * will be cut off down to 24 hours.
     *
     * Default value: `30` minutes, ie. half an hour
     */
    SECRET_ROTATE_PERIOD("secret.rotate.period"),

    /**
     * `secure_ticket_codec`
     *
     * Specify the implementation of {@link SecureTicketCodec}
     *
     * Default value: {@link DefaultSecureTicketCodec}
     */
    SECURE_TICKET_CODEC("secure_ticket_codec"),

    /**
     * `server.header` specifies the server header to be output to the response
     *
     * Default value: `act/${act-version}`
     */
    SERVER_HEADER("server.header"),

    /**
     * `server.header.user-app.enabled` specifies whether use app name and
     * version as the server header.
     *
     * Default value: `true`
     */
    SERVER_HEADER_USE_APP("server.header.use-app.enabled"),

    /**
     * `session.outputExpiration.enabled` turn on/off expiration output to
     * response header.
     *
     * This setting only effective when it is using token to
     * map session payload.
     *
     * Default value: `true`
     *
     */
    SESSION_OUTPUT_EXPIRATION("session.outputExpiration.enabled"),

    /**
     * `session.pass_through` turn on/off session pass through mode.
     *
     * When session pass_through is turned on the framework will not
     * tried to resolve or serialize the session
     */
    SESSION_PASS_THROUGH("session.pass_through.enabled"),

    /**
     * `session.ttl` specifies the session duration in seconds.
     * If user failed to interact with server for amount of time that
     * exceeds the setting then the session will be destroyed
     *
     * Default value: `60 * 30` i.e half an hour
     */
    SESSION_TTL("session.ttl"),

    /**
     * `session.persistent.enabled` specify whether the system
     * should treat session cookie as persistent cookie. If this setting
     * is enabled, then the user's session will not be destroyed after
     * browser closed.
     *
     * Default value: `false`
     *
     * See <a href="http://en.wikipedia.org/wiki/HTTP_cookie#Persistent_cookie">HTTP_cookie</a>
     */
    SESSION_PERSISTENT_ENABLED("session.persistent.enabled"),

    /**
     * `session.encrypt.enabled` specify whether the system should
     * encrypt the key/value pairs in the session cookie. Enable session
     * encryption will greatly improve the security but with the cost
     * of additional CPU usage and a little bit longer time on request
     * processing.
     *
     * Default value: `false`
     */
    SESSION_ENCRYPT_ENABLED("session.encrypt.enabled"),

    /**
     * `act.session.key.username` specifies the session key for username
     *
     * Default value: `username`
     */
    SESSION_KEY_USERNAME("session.key.username"),

    /**
     * `session.mapper.impl` specifies the implementation of {@link act.session.SessionMapper}
     *
     * Default value: {@link act.session.CookieSessionMapper}
     */
    SESSION_MAPPER("session.mapper.impl"),

    /**
     * `session.codec.impl` specifies the implementation of {@link act.session.SessionCodec}
     *
     * Default value: {@link act.session.DefaultSessionCodec}
     */
    SESSION_CODEC("session.codec.impl"),

    /**
     * `session.mapper.header.prefix`
     *
     * This setting is deprecated. Please use
     * {@link #SESSION_MAPPER} instead.
     *
     * Default value: `null`
     */
    @Deprecated
    SESSION_MAPPER_HEADER_PREFIX("session.mapper.header.prefix"),

    /**
     * `session.header` - specify the session header name.
     *
     * Effective only when {@link act.session.SessionMapper} is
     * {@link act.session.HeaderTokenSessionMapper}.
     *
     * If this configuration is set then {@link #SESSION_HEADER_PREFIX}
     * is ignored for session header name.
     *
     * Default value: `null`
     */
    // TODO: change it to `header.session`
    SESSION_HEADER("session.header"),

    /**
     * `session.query.param.name` - specify the name of the query parameter
     * used to specify session token.
     *
     * Refer: https://github.com/actframework/actframework/issues/1293
     *
     * Default value: the value of {@link #SESSION_HEADER}
     */
    SESSION_QUERY_PARAM_NAME("session.query.param.name"),

    /**
     * `session.header.prefix`, specify the prefix of session
     * header.
     *
     * This is only effective when {@link #SESSION_MAPPER} is set
     * to {@link act.session.HeaderTokenSessionMapper} or any
     * compound session mapper that support it.
     *
     * If {@link #SESSION_HEADER} is not set, then header name of
     * session token is `${session.header.prefix}-Session`.
     *
     * The flash header name is always `${flash.header.prefix}-Flash`.
     *
     * Default value: {@link act.session.HeaderTokenSessionMapper#DEF_HEADER_PREFIX}
     */
    SESSION_HEADER_PREFIX("session.header.prefix"),

    /**
     * `session.header.payload.prefix`, set the session payload prefix, e.g.
     * `"Bearer "`.
     *
     * Default value: `` (blank string)
     */
    SESSION_HEADER_PAYLOAD_PREFIX("session.header.payload.prefix"),

    /**
     * `session.secure.enabled` specifies whether the session cookie should
     * be set as secure. Enable secure session will cause session cookie only
     * effective in https connection. Literally this will enforce the web site to run
     * default by https.
     *
     * Default value: `true`
     *
     * **Note** when {@link Act Act server} is running in {@link Act.Mode#DEV mode}
     * session http only will be disabled without regarding to the `session.secure.enabled`
     * setting
     */
    SESSION_SECURE("session.secure.enabled"),

    /**
     * `source.version` specifies the java version
     * of the src code. This configuration is used only
     * in dev mode.
     *
     * Default value: 1.7
     */
    SOURCE_VERSION("source.version"),

    /**
     * `ssl.enabled`
     *
     * Turn on/off SSL support
     *
     * Default value: `false`
     */
    SSL("ssl.enabled"),

    /**
     * `system.self-healing`
     *
     * Turn on/off System Self Healing. Refer GH1234
     *
     * Default value: `false`
     */
    SYS_SELF_HEALING("system.self-healing"),

    /**
     * `target.version` specifies the java version of the compile
     * target code. This configuration is used only in dev mode.
     *
     * Default value: 1.7
     */
    TARGET_VERSION("target.version"),

    /**
     * `template.home` specifies where the view templates resides.
     * If not specified then will use the {@link View#name() view name
     * in lower case} as the template home if that view is used.
     *
     * Default value: `default`
     */
    TEMPLATE_HOME("template.home"),

    /**
     * `test.timeout` specifies automate test http agent timeout in seconds
     *
     * Default value: 60 * 60 (i.e. 1 hour) in dev mode, 10 in automate test mode
     */
    TEST_TIMEOUT("test.timeout"),

    /**
     * `trace.handler.enabled` turn on/off handle invocation calls.
     *
     * When this configuration is turned on, every call to the
     * action handler/job handler/mail sender method will be logged.
     *
     * Default value: `false`
     */
    TRACE_HANDLER_ENABLED("trace.handler.enabled"),

    /**
     * `trace.request.enabled` turn on/off incoming request log
     *
     * When this configuration is turned on, every incoming request
     * will be logged
     *
     * default value: `false`
     */
    TRACE_REQUEST_ENABLED("trace.request.enabled"),

    /**
     * `upload.in_memory.threshold`
     *
     * If file upload content length is less than this configuration then
     * the file will not get written into disk, instead it will get cached
     * into a in memory byte array
     *
     * Default value: `1024 * 10`
     */
    UPLOAD_IN_MEMORY_CACHE_THRESHOLD("upload.in_memory.threshold.int"),

    /**
     * `url.context` specifies the app URL context.
     *
     * If this configuration is specified then all route configured will
     * be attached to the configured context path.
     *
     * Default value: `null`
     */
    URL_CONTEXT("url.context"),

    /**
     * `url.login` specifies the login URL which is used
     * by {@link act.util.RedirectToLoginUrl}
     *
     * Default value: `/login`
     */
    URL_LOGIN("url.login"),

    /**
     * `url.login.ajax` specifies the login URL which is used
     * by {@link act.util.RedirectToLoginUrl} when request is AJAX
     *
     * Default value: the value of {@link #URL_LOGIN}
     */
    URL_LOGIN_AJAX("url.login.ajax"),

    /**
     * `view.default` specifies the default view solution. If there
     * are multiple views registered and default view are available, then
     * it will be used at priority to load the templates
     *
     * Default value: `rythm`
     */
    VIEW_DEFAULT("view.default"),

    /**
     * `ws.key.ticket`
     *
     * Specifies the parameter variable name to get websocket ticket
     *
     * Default value: `ws_ticket`
     */
    WS_KEY_TICKET("ws.key.ticket"),

    /**
     * `ws.purge-closed-conn.period`
     *
     * Specifies the waiting period in seconds to purge closed websocket connections
     *
     * Default value: `10` in PROD mode, `1` in DEV mode
     */
    WS_PURGE_CLOSED_CONN_PERIOD("ws.purge-closed-conn.period"),

    /**
     * `x_forward_protocol`
     *
     * specifies the header it shall check to determine if the current request
     * is convey on HTTPS channel.
     *
     * Default value: `http`
     */
    X_FORWARD_PROTOCOL("x_forward_protocol"),

    /**
     * `xml_root`
     *
     * Specifies the XML response root tag name.
     *
     * Default value: `xml`
     */
    XML_ROOT("xml_root"),

    ;
    private String key;
    private Object defVal;
    static ConfigKeyHelper helper = new ConfigKeyHelper(Act.F.MODE_ACCESSOR);

    AppConfigKey(String key) {
        this(key, null);
    }

    AppConfigKey(String key, Object defVal) {
        this.key = Config.canonical(key);
        this.defVal = defVal;
    }

    public static void onApp(final App app) {
        helper.onApp();
    }

    /**
     * Return the key string
     *
     * @return the key of the configuration
     */
    public String key() {
        return key;
    }

    /**
     * Return default value of this setting. The configuration data map
     * is passed in in case the default value be variable depending on
     * another setting.
     *
     * @param configuration
     * @return return the default value
     */
    protected Object getDefVal(Map<String, ?> configuration) {
        return defVal;
    }

    /**
     * Calling to this method is equals to calling {@link #key()}
     *
     * @return key of the configuration
     */
    @Override
    public String toString() {
        return key;
    }

    @Override
    public Object defVal() {
        return defVal;
    }

    public <T> List<T> implList(String key, Map<String, ?> configuration, Class<T> c) {
        return helper.getImplList(key, configuration, c);
    }

    /**
     * Return configuration value from the configuration data map using the {@link #key}
     * of this {@link AppConfigKey setting} instance
     *
     * @param configuration
     * @param <T>
     * @return return the configuration
     */
    public <T> T val(Map<String, ?> configuration) {
        return helper.getConfiguration(this, configuration);
    }

    private static Map<String, AppConfigKey> lookup = new HashMap<>(50);

    static {
        Set<String> suffixes = ConfigKeyHelper.suffixes();
        Set<String> nonAliasSuffixes = ConfigKeyHelper.nonAliasSuffixes();
        for (AppConfigKey k : values()) {
            addToLookup(k.name(), k);
            String key = k.key().toUpperCase();
            addToLookup(key, k);
            String suffix = S.afterLast(key, ".");
            if (S.notBlank(suffix) && suffixes.contains(suffix) && !nonAliasSuffixes.contains(suffix)) {
                Set<String> aliases = ConfigKeyHelper.aliases(key, suffix);
                for (String alias : aliases) {
                    addToLookup(Config.canonical(alias), k);
                }
            }
        }
    }

    private static void addToLookup(String name, AppConfigKey key) {
        lookup.put(Config.canonical(name), key);
    }

    /**
     * Return key enum instance from the string in case insensitive mode
     *
     * @param s
     * @return configuration key from the string
     */
    public static AppConfigKey valueOfIgnoreCase(String s) {
        E.illegalArgumentIf(S.blank(s), "config key cannot be empty");
        return lookup.get(Config.canonical(s));
    }

}
