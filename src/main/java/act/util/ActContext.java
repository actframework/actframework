package act.util;

import act.Destroyable;
import act.app.ActionContext;
import act.app.App;
import act.cli.CliContext;
import act.conf.AppConfig;
import act.i18n.I18n;
import act.mail.MailerContext;
import act.view.Template;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.enterprise.context.RequestScoped;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static act.app.App.logger;

public interface ActContext<CTX_TYPE extends ActContext> extends ParamValueProvider {
    App app();
    AppConfig config();
    CTX_TYPE accept(H.Format fmt);
    H.Format accept();
    CTX_TYPE locale(Locale locale);
    Locale locale();
    Locale locale(boolean required);
    /**
     * If {@link #templatePath(String) template path has been set before} then return
     * the template path
     */
    String templatePath();
    /**
     * Set path to template file
     * @param path the path to template file
     * @return this {@code AppContext}
     */
    CTX_TYPE templatePath(String path);
    Template cachedTemplate();
    CTX_TYPE cacheTemplate(Template template);
    <T> T renderArg(String name);
    /**
     * Returns all render arguments
     */
    Map<String, Object> renderArgs();
    CTX_TYPE renderArg(String name, Object val);
    CTX_TYPE addListener(Listener listener);
    CTX_TYPE addDestroyable(Destroyable resource);
    CTX_TYPE attribute(String name, Object attr);
    <T> T attribute(String name);
    Map<String, Object> attributes();
    CTX_TYPE removeAttribute(String name);

    String i18n(boolean ignoreError, String msgId, Object... args);

    String i18n(String msgId, Object ... args);

    String i18n(Class<?> bundleSpec, String msgId, Object... args);

    String i18n(boolean ignoreError, Class<?> bundleSpec, String msgId, Object... args);

    String i18n(Enum<?> msgId);

    String i18n(Class<?> bundleSpec, Enum<?> msgId);

    Map<String, String> i18n(Class<? extends Enum> enumClass);

    Map<String, String> i18n(Class<?> bundleSpec, Class<? extends Enum> enumClass);

    String methodPath();

    interface Listener {
        void onDestroy(ActContext context);
    }

    abstract class Base<VC_TYPE extends Base> extends DestroyableBase implements ActContext<VC_TYPE> {

        private App app;
        private String templatePath;
        private Template template;
        private Map<String, Object> renderArgs;
        private List<Listener> listenerList;
        private List<Destroyable> destroyableList;
        private Map<String, Object> attributes;
        private Locale locale;

        public Base(App app) {
            E.NPE(app);
            this.app = app;
            renderArgs = C.newMap();
            attributes = C.newMap();
            listenerList = C.newList();
            destroyableList = C.newList();
        }

        @Override
        protected void releaseResources() {
            for (Listener l : listenerList) {
                try {
                    l.onDestroy(this);
                } catch (Exception e) {
                    logger.warn(e, "error calling listener onDestroy method");
                }
            }
            Destroyable.Util.destroyAll(destroyableList, RequestScoped.class);
            Destroyable.Util.tryDestroyAll(attributes.values(), RequestScoped.class);
            this.attributes.clear();
            this.renderArgs.clear();
            this.template = null;
            this.app = null;
            this.template = null;
            this.listenerList.clear();
            this.destroyableList.clear();
        }

        @Override
        public App app() {
            return app;
        }

        @Override
        public AppConfig config() {
            return app().config();
        }

        @Override
        public String templatePath() {
            return templatePath;
        }

        @Override
        public VC_TYPE templatePath(String templatePath) {
            this.templatePath = templatePath;
            return me();
        }

        @Override
        public Template cachedTemplate() {
            return template;
        }

        @Override
        public VC_TYPE cacheTemplate(Template template) {
            this.template = template;
            return me();
        }

        public final VC_TYPE locale(Locale locale) {
            this.locale = locale;
            return me();
        }

        public final Locale locale() {
            return this.locale;
        }

        public Locale locale(boolean required) {
            Locale locale = this.locale;
            if (null == locale) {
                if (!required) {
                    return null;
                }
                locale = config().locale();
            }
            return locale;
        }

        public static final String DEF_RESOURCE_BUNDLE_NAME = I18n.DEF_RESOURCE_BUNDLE_NAME;

        public String i18n(boolean ignoreError, String msgId, Object... args) {
            return I18n.i18n(ignoreError, locale(true), I18n.DEF_RESOURCE_BUNDLE_NAME, msgId, args);
        }

        public String i18n(String msgId, Object ... args) {
            return I18n.i18n(locale(true), DEF_RESOURCE_BUNDLE_NAME, msgId, args);
        }

        public String i18n(Class<?> bundleSpec, String msgId, Object... args) {
            return I18n.i18n(locale(true), bundleSpec.getName(), msgId, args);
        }

        public String i18n(boolean ignoreError, Class<?> bundleSpec, String msgId, Object... args) {
            return I18n.i18n(ignoreError, locale(true), bundleSpec.getName(), msgId, args);
        }

        public String i18n(Enum<?> msgId) {
            return I18n.i18n(locale(true), DEF_RESOURCE_BUNDLE_NAME, msgId);
        }

        public String i18n(Class<?> bundleSpec, Enum<?> msgId) {
            return I18n.i18n(locale(true), bundleSpec.getName(), msgId);
        }

        public Map<String, String> i18n(Class<? extends Enum> enumClass) {
            return I18n.i18n(locale(true), enumClass);
        }

        public Map<String, String> i18n(Class<?> bundleSpec, Class<? extends Enum> enumClass) {
            return I18n.i18n(locale(true), bundleSpec, enumClass);
        }

        protected VC_TYPE me() {
            return (VC_TYPE) this;
        }

        @Override
        public <T> T renderArg(String name) {
            return (T) renderArgs.get(name);
        }

        @Override
        public VC_TYPE renderArg(String name, Object val) {
            renderArgs.put(name, val);
            return me();
        }

        @Override
        public Map<String, Object> renderArgs() {
            return C.newMap(renderArgs);
        }

        /**
         * Associate a user attribute to the context. Could be used by third party
         * libraries or user application
         *
         * @param name the className used to reference the attribute
         * @param attr the attribute object
         * @return this context
         */
        public VC_TYPE attribute(String name, Object attr) {
            attributes.put(name, attr);
            return me();
        }

        public <T> T attribute(String name) {
            return $.cast(attributes.get(name));
        }

        public VC_TYPE removeAttribute(String name) {
            attributes.remove(name);
            return me();
        }

        @Override
        public Map<String, Object> attributes() {
            return attributes;
        }

        @Override
        public VC_TYPE addListener(Listener listener) {
            listenerList.add(listener);
            return me();
        }

        @Override
        public VC_TYPE addDestroyable(Destroyable resource) {
            destroyableList.add(resource);
            return me();
        }

        public static ActContext currentContext() {
            ActContext ctx = ActionContext.current();
            if (null != ctx) {
                return ctx;
            }
            ctx = MailerContext.current();
            if (null != ctx) {
                return ctx;
            }
            ctx = CliContext.current();
            if (null != ctx) {
                return ctx;
            }
            return null;
        }

        public static Class<? extends ActContext> currentContextType() {
            ActContext ctx = currentContext();
            return null == ctx ? null : ctx.getClass();
        }
    }
}
