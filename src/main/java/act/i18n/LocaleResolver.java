package act.i18n;

import act.app.ActionContext;
import act.conf.AppConfig;
import org.osgl.http.H;
import org.osgl.mvc.annotation.PostAction;

import javax.inject.Inject;
import java.util.Locale;

/**
 * Responsible for setting up client Locale for the context
 *
 * The client locale info is resolved in the following sequence:
 * 1. check the request parameter by configured name
 * 2. check the session variable
 * 3. check the cookie value
 * 4. check the `Accept-Language` header
 * 5. use the server locale
 */
public class LocaleResolver {

    private static final String KEY = "__locale__";
    private static final int COOKIE_TTL = 60 * 60 * 24 * 7;

    private ActionContext context;
    private AppConfig config;
    private boolean enabled;
    private Locale locale;
    private boolean forceWriteCookie;

    @PostAction("~/i18n/locale")
    public static void updateLocale() {
        // there is no logic needed as locale has been processed built-in logic already
    }

    @Inject
    public LocaleResolver(ActionContext context) {
        AppConfig config = context.config();
        this.enabled = config.i18nEnabled();
        if (!this.enabled) {
            return;
        }
        this.context = context;
        this.config = config;
    }

    public void resolve() {
        if (!enabled) {
            return;
        }
        Locale locale = resolveFromParam();
        if (null == locale) {
            locale = resolveFromSession();
        }
        if (null == locale) {
            locale = resolveFromCookie();
        }
        if (null == locale) {
            locale = resolveFromHeader();
        }
        if (null == locale) {
            locale = resolveFromServer();
        }
        context.locale(locale);
        this.locale = locale;
    }

    public void dissolve() {
        if (!enabled) {
            return;
        }
        String cookieName = config.localeCookieName();
        if (forceWriteCookie || this.locale != context.locale() || null == context.cookie(cookieName)) {
            Locale locale = context.locale();
            if (null == locale) {
                locale = this.locale;
            }
            String localeStr = locale.toString();
            H.Session session = context.session();
            if (null != session) {
                session.put(KEY, localeStr);
            }
            H.Cookie cookie = new H.Cookie(cookieName, localeStr);
            cookie.domain(config.cookieDomain());
            cookie.path("/");
            cookie.maxAge(COOKIE_TTL);
            context.resp().addCookie(cookie);
        }
    }

    private Locale resolveFromParam() {
        String s = context.paramVal(config.localeParamName());
        Locale locale = parseStr(s);
        if (null != locale) {
            forceWriteCookie = true;
        }
        return locale;
    }

    private Locale resolveFromSession() {
        H.Session session = context.session();
        return null == session ? null : parseStr(session.get(KEY));
    }

    private Locale resolveFromCookie() {
        H.Cookie cookie = context.cookie(config.localeCookieName());
        return null == cookie ? null : parseStr(cookie.value());
    }

    private Locale resolveFromHeader() {
        return context.req().locale();
    }

    private Locale resolveFromServer() {
        return config.locale();
    }

    private Locale parseStr(String val) {
        if (null == val) {
            return null;
        }
        String[] sa = val.trim().split("_");
        int len = sa.length;
        switch (len) {
            case 3:
                return new Locale(sa[0], sa[1], sa[2]);
            case 2:
                return new Locale(sa[0], sa[1]);
            default:
                return new Locale(sa[0]);
        }
    }

}
