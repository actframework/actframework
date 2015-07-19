package act.util;

import act.app.App;
import act.conf.AppConfig;
import act.view.Template;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.Locale;
import java.util.Map;

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

    public static abstract class ActContextBase<VC_TYPE extends ActContextBase> extends DestroyableBase implements ActContext<VC_TYPE> {
        private App app;
        private String templatePath;
        private Template template;
        private Map<String, Object> renderArgs;

        public ActContextBase(App app) {
            E.NPE(app);
            this.app = app;
            renderArgs = C.newMap();
        }

        @Override
        protected void releaseResources() {
            this.renderArgs.clear();
            this.template = null;
            this.app = null;
            this.template = null;
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
    }
}
