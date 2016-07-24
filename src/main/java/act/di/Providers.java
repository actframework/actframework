package act.di;

import act.app.ActionContext;
import act.app.App;
import act.app.CliContext;
import act.app.CliSession;
import act.app.util.AppCrypto;
import act.conf.AppConfig;
import act.mail.MailerContext;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.genie.ScopeCache;
import org.osgl.http.H;
import org.osgl.util.E;

import javax.inject.Provider;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

/**
 * Name space of built in providers
 */
public final class Providers {

    private static ScopeCache.SingletonScope _SINGLETON_SCOPE = new ScopeCache.SingletonScope() {
        @Override
        public <T> T get(Class<T> aClass) {
            return app().singleton(aClass);
        }

        @Override
        public <T> void put(Class<T> aClass, T t) {
            app().registerSingleton(aClass, t);
        }
    };

    private static ScopeCache.RequestScope _REQ_SCOPE = new ScopeCache.RequestScope() {
        @Override
        public <T> T get(Class<T> aClass) {
            ActContext ctx = context();
            return null == ctx ? null : (T) ctx.attribute(aClass.getName());
        }

        @Override
        public <T> void put(Class<T> aClass, T t) {
            ActContext ctx = context();
            E.illegalStateIf(null == ctx);
            ctx.attribute(aClass.getName(), t);
        }
    };

    private static ScopeCache.SessionScope _SESS_SCOPE = new ScopeCache.SessionScope() {
        @Override
        public <T> T get(Class<T> aClass) {
            ActionContext actionContext = ActionContext.current();
            if (null != actionContext) {
                H.Session sess = actionContext.session();
                String key = aClass.getName();
                T t = sess.cached(key);
                if (null != t) {
                    sess.cache(key, t, ttl());
                }
                return t;
            }
            CliContext cliContext = CliContext.current();
            if (null != cliContext) {
                CliSession sess = cliContext.session();
                return sess.attribute(aClass.getName());
            }
            return null;
        }

        @Override
        public <T> void put(Class<T> aClass, T t) {
            ActionContext actionContext = ActionContext.current();
            if (null != actionContext) {
                H.Session sess = actionContext.session();
                sess.cache(aClass.getName(), t, ttl());
            }
            CliContext cliContext = CliContext.current();
            if (null != cliContext) {
                CliSession sess = cliContext.session();
                sess.attribute(aClass.getName(), t);
            }
        }

        private int ttl() {
            return (int)app().config().sessionTtl();
        }
    };


    private Providers() {}

    public static final Provider<App> APP = new Provider<App>() {
        @Override
        public App get() {
            return app();
        }
    };

    public static final Provider<ActionContext> ACTION_CONTEXT = new Provider<ActionContext>() {
        @Override
        public ActionContext get() {
            return ActionContext.current();
        }
    };

    public static final Provider<CliContext> CLI_CONTEXT = new Provider<CliContext>() {
        @Override
        public CliContext get() {
            return CliContext.current();
        }
    };

    public static final Provider<MailerContext> MAILER_CONTEXT = new Provider<MailerContext>() {
        @Override
        public MailerContext get() {
            return MailerContext.current();
        }
    };

    public static final Provider<AppConfig> APP_CONFIG = new Provider<AppConfig>() {
        @Override
        public AppConfig get() {
            return app().config();
        }
    };

    public static final Provider<AppCrypto> APP_CRYPTO = new Provider<AppCrypto>() {
        @Override
        public AppCrypto get() {
            return app().crypto();
        }
    };

    public static final Provider<CacheService> APP_CACHE_SERVICE = new Provider<CacheService>() {
        @Override
        public CacheService get() {
            return app().cache();
        }
    };

    public static final Provider<ScopeCache.SingletonScope> SINGLETON_SCOPE = new Provider<ScopeCache.SingletonScope>() {
        @Override
        public ScopeCache.SingletonScope get() {
            return _SINGLETON_SCOPE;
        }
    };

    public static final Provider<ScopeCache.RequestScope> REQ_SCOPE = new Provider<ScopeCache.RequestScope>() {
        @Override
        public ScopeCache.RequestScope get() {
            return _REQ_SCOPE;
        }
    };

    public static final Provider<ScopeCache.SessionScope> SESSION_SCOPE = new Provider<ScopeCache.SessionScope>() {
        @Override
        public ScopeCache.SessionScope get() {
            return _SESS_SCOPE;
        }
    };

    public static void registerBuiltInProviders($.Func2<Class, Provider, ?> register) {
        for (Field field : Providers.class.getDeclaredFields()) {
            try {
                if (Provider.class.isAssignableFrom(field.getType())) {
                    ParameterizedType type = $.cast(field.getGenericType());
                    Provider<?> provider = $.cast(field.get(null));
                    register.apply((Class) type.getActualTypeArguments()[0], provider);
                }
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }
    }

    private static App app() {
        return App.instance();
    }

    private static ActContext context() {
        ActContext context = ActionContext.current();
        if (null == context) {
            context = CliContext.current();
            if (null == context) {
                context = MailerContext.current();
            }
        }
        return context;
    }

}
