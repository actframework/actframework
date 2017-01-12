package act.inject;

import act.app.ActionContext;
import act.app.App;
import act.app.util.AppCrypto;
import act.cli.CliContext;
import act.conf.AppConfig;
import act.db.Dao;
import act.event.EventBus;
import act.mail.MailerContext;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.cache.CacheService;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.web.util.UserAgent;

import javax.inject.Provider;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Name space of built in providers
 */
@SuppressWarnings("unused")
public final class ActProviders {

    private ActProviders() {}

    private static Set<Class> providedTypes = C.newSet();

    static {
        registerBuiltInProviders(ActProviders.class, new $.F2<Class, Provider, Void>() {
            @Override
            public Void apply(Class aClass, Provider provider) throws NotAppliedException, Osgl.Break {
                providedTypes.add(aClass);
                return null;
            }
        });
    }

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

    public static final Provider<H.Session> SESSION = new Provider<H.Session>() {
        @Override
        public H.Session get() {
            return ActionContext.current().session();
        }
    };

    public static final Provider<H.Flash> FLASH = new Provider<H.Flash>() {
        @Override
        public H.Flash get() {
            return ActionContext.current().flash();
        }
    };

    public static final Provider<H.Request> REQUEST = new Provider<H.Request>() {
        @Override
        public H.Request get() {
            return ActionContext.current().req();
        }
    };

    public static final Provider<H.Response> RESPONSE = new Provider<H.Response>() {
        @Override
        public H.Response get() {
            return ActionContext.current().resp();
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

    public static final Provider<ActContext> ACT_CONTEXT = new Provider<ActContext>() {
        @Override
        public ActContext get() {
            ActContext ctx = MailerContext.current();
            if (null != ctx) {
                return ctx;
            }
            ctx = ActionContext.current();
            if (null != ctx) {
                return ctx;
            }
            return CliContext.current();
        }
    };

    public static final Provider<Logger> LOGGER = new Provider<Logger>() {
        @Override
        public Logger get() {
            return App.logger;
        }
    };

    public static final Provider<UserAgent> USER_AGENT = new Provider<UserAgent>() {
        @Override
        public UserAgent get() {
            ActionContext actionContext = ActionContext.current();
            if (null == actionContext) {
                throw new IllegalStateException();
            }
            return actionContext.userAgent();
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

    public static final Provider<EventBus> EVENT_BUS = new Provider<EventBus>() {
        @Override
        public EventBus get() {
            return app().eventBus();
        }
    };

    public static void registerBuiltInProviders(Class<?> providersClass, $.Func2<Class, Provider, ?> register) {
        for (Field field : providersClass.getDeclaredFields()) {
            try {
                if (Provider.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Type type = field.getGenericType();
                    if (type instanceof ParameterizedType) {
                        ParameterizedType ptype = $.cast(field.getGenericType());
                        Provider<?> provider = $.cast(field.get(null));
                        register.apply((Class) ptype.getActualTypeArguments()[0], provider);
                    }
                }
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }
    }

    public static boolean isProvided(Class<?> aClass) {
        return providedTypes.contains(aClass) || Dao.class.isAssignableFrom(aClass);
    }

    private static App app() {
        return App.instance();
    }

    public static void addProvidedType(Class<?> aClass) {
        providedTypes.add(aClass);
    }

}
