package act.inject;

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
import act.app.*;
import act.cli.CliContext;
import act.cli.CliOverHttpContext;
import act.cli.CliSession;
import act.conf.AppConfig;
import act.crypto.AppCrypto;
import act.db.DB;
import act.db.Dao;
import act.db.DbPlugin;
import act.db.DbService;
import act.event.EventBus;
import act.job.JobContext;
import act.mail.MailerContext;
import act.plugin.PrincipalProvider;
import act.route.Router;
import act.util.ActContext;
import act.util.ClassInfoRepository;
import act.util.ProgressGauge;
import act.util.SimpleProgressGauge;
import act.view.ViewManager;
import act.ws.SecureTicketCodec;
import act.ws.WebSocketContext;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.inject.NamedProvider;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.S;
import org.osgl.web.util.UserAgent;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.inject.Provider;

/**
 * Name space of built in providers
 */
@SuppressWarnings("unused")
public final class ActProviders {

    private ActProviders() {}

    private static Set<Class> providedTypes;

    public static void classInit(App app) {
        providedTypes = app.createSet();
        init(providedTypes);
    }

    public static void testClassInit() {
        providedTypes = new HashSet<>();
        init(providedTypes);
    }

    private static void init(final Set<Class> providedTypes) {
        registerBuiltInNamedProviders(ActProviders.class, new $.F2<Class, NamedProvider, Void>() {
            @Override
            public Void apply(Class aClass, NamedProvider provider) throws NotAppliedException, $.Break {
                providedTypes.add(aClass);
                return null;
            }
        });
        registerBuiltInProviders(ActProviders.class, new $.F2<Class, Provider, Void>() {
            @Override
            public Void apply(Class aClass, Provider provider) throws NotAppliedException, $.Break {
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

    public static final Provider<CliSession> CLI_SESSION = new Provider<CliSession>() {
        @Override
        public CliSession get() {
            CliContext context = CliContext.current();
            return null == context ? null : context.session();
        }
    };

    public static final Provider<ClassInfoRepository> CLASS_INFO_REPO = new Provider<ClassInfoRepository>() {
        @Override
        public ClassInfoRepository get() {
            return Act.app().classLoader().classInfoRepository();
        }
    };

    public static final Provider<ProgressGauge> PROGRESS_GAUGE = new Provider<ProgressGauge>() {
        @Override
        public ProgressGauge get() {
            ActContext.Base<?> context = ActContext.Base.currentContext();
            if (null != context) {
                return context.progress();
            }
            return new SimpleProgressGauge();
        }
    };

    public static final Provider<MailerContext> MAILER_CONTEXT = new Provider<MailerContext>() {
        @Override
        public MailerContext get() {
            return MailerContext.current();
        }
    };

    public static final Provider<WebSocketContext> WEB_SOCKET_CONTEXT = new Provider<WebSocketContext>() {
        @Override
        public WebSocketContext get() {
            return WebSocketContext.current();
        }
    };

    public static final Provider<ActContext> ACT_CONTEXT = new Provider<ActContext>() {
        @Override
        public ActContext get() {
            ActContext ctx = MailerContext.current();
            if (null != ctx) {
                return ctx;
            }
            ctx = WebSocketContext.current();
            if (null != ctx) {
                return ctx;
            }
            ctx = ActionContext.current();
            if (null != ctx) {
                return ctx;
            }
            ctx = CliContext.current();
            if (null != ctx) {
                return ctx;
            }
            ctx = CliOverHttpContext.current();
            if (null != ctx) {
                return ctx;
            }
            return JobContext.current();
        }
    };

    public static final Provider<Logger> LOGGER = new Provider<Logger>() {
        @Override
        public Logger get() {
            return Act.LOGGER;
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

    public static final Provider<DbService> DB_SERVICE_PROVIDER = new Provider<DbService>() {
        @Override
        public DbService get() {
            return app().dbServiceManager().dbService(DB.DEFAULT);
        }
    };

    public static final NamedProvider<DbService> NAMED_DB_SERVICE_PROVIDER = new NamedProvider<DbService>() {
        @Override
        public DbService get(String name) {
            DbServiceManager mgr = app().dbServiceManager();
            DbService svc = mgr.dbService(name);
            if (null == svc && ("svc".equalsIgnoreCase(name) || "dbService".equalsIgnoreCase(name) || "dbSvc".equalsIgnoreCase(name) || "service".equalsIgnoreCase(name))) {
                return mgr.dbService(DB.DEFAULT);
            }
            return svc;
        }
    };

    public static final NamedProvider<CacheService> APP_NAMED_CACHE_SERVICE = new NamedProvider<CacheService>() {
        @Override
        public CacheService get(String name) {
            return app().cache(name);
        }
    };

    public static final NamedProvider<H.Cookie> COOKIE = new NamedProvider<H.Cookie>() {
        @Override
        public H.Cookie get(String s) {
            ActionContext ctx = ActionContext.current();
            if (null == ctx) {
                return null;
            }
            H.Request req = ctx.req();
            return null == req ? null : req.cookie(S.ensure(s).strippedOff("", "Cookie"));
        }
    };

    public static final Provider<Router> ROUTER_PROVIDER = new Provider<Router>() {
        @Override
        public Router get() {
            ActionContext ctx = ActionContext.current();
            return null == ctx ? Act.app().router() : ctx.router();
        }
    };

    public static final NamedProvider<Router> NAMED_ROUTER_PROVIDER = new NamedProvider<Router>() {
        @Override
        public Router get(String name) {
            Router router = Act.app().router(name);
            return null == router ? (Router) Act.injector().get(Router.class) : router;
        }
    };

    public static final Provider<ViewManager> VIEW_MANAGER = new Provider<ViewManager>() {
        @Override
        public ViewManager get() {
            return Act.viewManager();
        }
    };

    public static final Provider<EventBus> EVENT_BUS = new Provider<EventBus>() {
        @Override
        public EventBus get() {
            return app().eventBus();
        }
    };

    public static final Provider<Locale> LOCALE = new Provider<Locale>() {
        @Override
        public Locale get() {
            ActContext context = ActContext.Base.currentContext();
            return null != context ? context.locale(true) : app().config().locale();
        }
    };

    public static final Provider<SecureTicketCodec> SECURE_TICKET_CODEC_PROVIDER = new Provider<SecureTicketCodec>() {
        @Override
        public SecureTicketCodec get() {
            AppConfig config = Act.appConfig();
            return config.secureTicketCodec();
        }
    };

    public static final Provider<PrincipalProvider> PRINCIPAL_PROVIDER = new Provider<PrincipalProvider>() {
        @Override
        public PrincipalProvider get() {
            return Act.app().principalProvider();
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

    public static void registerBuiltInNamedProviders(Class<?> providersClass, $.Func2<Class, NamedProvider, ?> register) {
        for (Field field : providersClass.getDeclaredFields()) {
            try {
                if (NamedProvider.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Type type = field.getGenericType();
                    if (type instanceof ParameterizedType) {
                        ParameterizedType ptype = $.cast(field.getGenericType());
                        NamedProvider<?> provider = $.cast(field.get(null));
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
