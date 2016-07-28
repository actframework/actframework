package act.util;

import act.Destroyable;
import act.app.App;
import act.conf.AppConfig;
import act.view.Template;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.enterprise.context.RequestScoped;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static act.app.App.logger;

public interface ActContext<CTX_TYPE extends ActContext> {
    App app();
    AppConfig config();
    CTX_TYPE accept(H.Format fmt);
    H.Format accept();
    Locale locale();
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
    CTX_TYPE removeAttribute(String name);

    public static interface Listener {
        void onDestroy(ActContext context);
    }

    public static abstract class ActContextBase<VC_TYPE extends ActContextBase> extends DestroyableBase implements ActContext<VC_TYPE> {
        private App app;
        private String templatePath;
        private Template template;
        private Map<String, Object> renderArgs;
        private List<Listener> listenerList;
        private List<Destroyable> destroyableList;
        private Map<String, Object> attributes;

        public ActContextBase(App app) {
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
        public VC_TYPE addListener(Listener listener) {
            listenerList.add(listener);
            return me();
        }

        @Override
        public VC_TYPE addDestroyable(Destroyable resource) {
            destroyableList.add(resource);
            return me();
        }
    }
}
