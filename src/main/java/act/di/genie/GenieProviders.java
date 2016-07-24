package act.di.genie;

import act.app.*;
import act.app.event.AppClassLoaded;
import act.mail.MailerContext;
import act.util.ActContext;
import act.util.ClassNode;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.genie.Genie;
import org.osgl.genie.ScopeCache;
import org.osgl.genie.loader.AnnotatedElementLoader;
import org.osgl.genie.loader.ConfigurationValueLoader;
import org.osgl.genie.loader.TypedElementLoader;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Integrate Genie with ActFramework
 */
class GenieProviders {
    private static final ScopeCache.SingletonScope _SINGLETON_SCOPE = new ScopeCache.SingletonScope() {
        @Override
        public <T> T get(Class<T> aClass) {
            return app().singleton(aClass);
        }

        @Override
        public <T> void put(Class<T> aClass, T t) {
            app().registerSingleton(aClass, t);
        }
    };

    private static final ScopeCache.RequestScope _REQ_SCOPE = new ScopeCache.RequestScope() {
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

    private static final ScopeCache.SessionScope _SESS_SCOPE = new ScopeCache.SessionScope() {
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

    private static ConfigurationValueLoader _CONF_VAL_LOADER = new ConfigurationValueLoader() {
        @Override
        protected Object conf(String s) {
            return app().config().get(s);
        }
    };

    private static final TypedElementLoader _TYPED_ELEMENT_LOADER = new TypedElementLoader() {
        @Override
        protected List<Class> load(Class aClass) {
            final AppClassLoader cl = app().classLoader();
            ClassNode root = cl.classInfoRepository().node(aClass.getName());
            if (null == root) {
                return C.list();
            }
            final List<Class> list = C.newList();
            root.visitPublicNotAbstractSubTreeNodes(new Osgl.Visitor<ClassNode>() {
                @Override
                public void visit(ClassNode classNode) throws Osgl.Break {
                    Class c = $.classForName(classNode.name(), cl);
                    list.add(c);
                }
            });
            return list;
        }
    };

    private static final AnnotatedElementLoader _ANNO_ELEMENT_LOADER = new AnnotatedElementLoader() {
        @Override
        protected List<Class<?>> load(Class<? extends Annotation> aClass, Genie genie) {
            final AppClassLoader cl = app().classLoader();
            ClassNode root = cl.classInfoRepository().node(aClass.getName());
            if (null == root) {
                return C.list();
            }
            final List<Class<?>> list = C.newList();
            root.visitPublicNotAbstractAnnotatedClasses(new Osgl.Visitor<ClassNode>() {
                @Override
                public void visit(ClassNode classNode) throws Osgl.Break {
                    Class c = $.classForName(classNode.name(), cl);
                    list.add(c);
                }
            });
            return list;
        }
    };

    private GenieProviders() {}

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

    public static final Provider<ConfigurationValueLoader> CONF_VALUE_LOADER = new Provider<ConfigurationValueLoader>() {
        @Override
        public ConfigurationValueLoader get() {
            return _CONF_VAL_LOADER;
        }
    };

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
