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

    public static void registerBuiltInProviders(Class<?> providersClass, $.Func2<Class, Provider, ?> register) {
        for (Field field : providersClass.getDeclaredFields()) {
            try {
                if (Provider.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
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

}
