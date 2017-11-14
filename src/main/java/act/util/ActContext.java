package act.util;

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

import act.Destroyable;
import act.act_messages;
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
import org.osgl.util.S;

import javax.enterprise.context.RequestScoped;
import javax.validation.ConstraintViolation;
import java.util.*;

public interface ActContext<CTX_TYPE extends ActContext> extends ParamValueProvider {
    /**
     * Used to store the {@link java.lang.reflect.Method} this context is trying
     * to invoke right now. Note the method might be corresponding to the
     * {@link #methodPath()} or not if the current method is an interceptor.
     */
    String ATTR_CUR_METHOD = "__ctx_cur_method__";
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

    /**
     * Returns the template context
     * @return template context
     */
    String templateContext();

    /**
     * Set template context
     * @param context the path to template context
     * @return this {@code ActContext}
     */
    CTX_TYPE templateContext(String context);

    /**
     * Check if the template path is implicit i.e. derived from {@link #methodPath()}
     * @return `true` if template path is implicit; `false` otherwise
     */
    boolean templatePathIsImplicit();
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

    CTX_TYPE addViolations(Map<String, ConstraintViolation> violations);
    CTX_TYPE addViolation(String property, ConstraintViolation violation);
    boolean hasViolation();
    Map<String, ConstraintViolation> violations();
    ConstraintViolation violation(String property);

    /**
     * Returns the user session with which this context is associated
     *
     * @return session id or null if no session associated with this context (e.g. mail context)
     */
    String sessionId();

    /**
     * Returns data format pattern. Normally should be date time format
     *
     * @return the data format pattern
     */
    String pattern();

    /**
     * Set data format pattern
     *
     * @param pattern
     *      the data format pattern
     * @return
     *      this context instance
     */
    CTX_TYPE pattern(String pattern);

    String _act_i18n(String msgId, Object... args);

    String i18n(boolean ignoreError, String msgId, Object... args);

    String i18n(String msgId, Object ... args);

    String i18n(Class<?> bundleSpec, String msgId, Object... args);

    String i18n(boolean ignoreError, Class<?> bundleSpec, String msgId, Object... args);

    String i18n(Enum<?> msgId);

    String i18n(Class<?> bundleSpec, Enum<?> msgId);

    Map<String, Object> i18n(Class<? extends Enum> enumClass);

    Map<String, Object> i18n(Class<?> bundleSpec, Class<? extends Enum> enumClass);

    Map<String, Object> i18n(Class<? extends Enum> enumClass, boolean outputProperties);

    Map<String, Object> i18n(Class<?> bundleSpec, Class<? extends Enum> enumClass, boolean outputProperties);

    String methodPath();

    /**
     * Returns a reusable {@link S.Buffer} instance
     * @return an S.Buffer instance that can be reused
     */
    S.Buffer strBuf();

    interface Listener {
        void onDestroy(ActContext context);
    }

    abstract class Base<CTX extends Base> extends DestroyableBase
            implements ActContext<CTX> {
        private App app;
        private String templatePath;
        private String templateContext;
        private Template template;
        private Map<String, Object> renderArgs;
        private List<Listener> listenerList;
        private List<Destroyable> destroyableList;
        private Map<String, Object> attributes;
        private Locale locale;
        private int fieldOutputVarCount;
        private S.Buffer strBuf;
        private boolean noTemplateCache;
        private SimpleProgressGauge progress = new SimpleProgressGauge();
        private String jobId;
        private String pattern;

        // (violation.propertyPath, violation)
        private Map<String, ConstraintViolation> violations;

        public Base(App app) {
            E.NPE(app);
            this.app = app;
            renderArgs = new HashMap<>();
            attributes = new HashMap<>();
            listenerList = new ArrayList<>();
            destroyableList = new ArrayList<>();
            strBuf = S.newBuffer();
            violations = new HashMap<>();
        }

        @Override
        protected void releaseResources() {
            for (Listener l : listenerList) {
                try {
                    l.onDestroy(this);
                } catch (Exception e) {
                    warn(e, "error calling listener onDestroy method");
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
            this.violations.clear();
            // note we can't destroy progress as it might still be used
            // by background thread
            //this.progress.destroy();
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
            String path = templatePath;
            String context = templateContext;
            if (S.notBlank(path)) {
                return path.startsWith("/") || S.blank(context) ? path : S.pathConcat(context, '/', path);
            } else {
                if (S.blank(context)) {
                    return methodPath().replace('.', '/');
                } else {
                    return S.pathConcat(context, '/', S.afterLast(methodPath(), "."));
                }
            }
        }

        @Override
        public CTX templatePath(String templatePath) {
            this.template = null;
            this.templatePath = templatePath;
            return me();
        }

        public String templateContext() {
            return this.templateContext;
        }

        public CTX templateContext(String templateContext) {
            this.template = null;
            this.templateContext = templateContext;
            return me();
        }

        /**
         * Template path is implicit if {@link #templatePath(String)} is never called
         * on this context instance
         * @return `true` if template path is implicit
         */
        @Override
        public boolean templatePathIsImplicit() {
            return null == templatePath;
        }

        @Override
        public Template cachedTemplate() {
            return template;
        }

        public CTX disableTemplateCaching() {
            this.noTemplateCache = true;
            return me();
        }

        @Override
        public CTX cacheTemplate(Template template) {
            if (!noTemplateCache) {
                this.template = template;
            }
            return me();
        }

        @Override
        public String pattern() {
            return pattern;
        }

        @Override
        public CTX pattern(String pattern) {
            this.pattern = pattern;
            return me();
        }

        /**
         * Default session id is `null`
         * @return the session id
         */
        public String sessionId() {
            return null;
        }

        public final CTX locale(Locale locale) {
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

        public String i18n(boolean ignoreError, String msgId, Object... args) {
            return I18n.i18n(ignoreError, locale(true), msgId, args);
        }

        public String i18n(String msgId, Object ... args) {
            return I18n.i18n(locale(true), msgId, args);
        }

        public String _act_i18n(String msgId, Object... args) {
            return I18n.i18n(locale(true), act_messages.class, msgId, args);
        }

        public String i18n(Class<?> bundleSpec, String msgId, Object... args) {
            return I18n.i18n(locale(true), bundleSpec, msgId, args);
        }

        public String i18n(boolean ignoreError, Class<?> bundleSpec, String msgId, Object... args) {
            return I18n.i18n(ignoreError, locale(true), bundleSpec, msgId, args);
        }

        public String i18n(Enum<?> msgId) {
            return I18n.i18n(locale(true), msgId);
        }

        public String i18n(Class<?> bundleSpec, Enum<?> msgId) {
            return I18n.i18n(locale(true), bundleSpec, msgId);
        }

        public Map<String, Object> i18n(Class<? extends Enum> enumClass) {
            return I18n.i18n(locale(true), enumClass);
        }

        public Map<String, Object> i18n(Class<?> bundleSpec, Class<? extends Enum> enumClass) {
            return I18n.i18n(locale(true), bundleSpec, enumClass);
        }

        public Map<String, Object> i18n(Class<? extends Enum> enumClass, boolean outputPropeties) {
            return I18n.i18n(locale(true), enumClass, outputPropeties);
        }

        public Map<String, Object> i18n(Class<?> bundleSpec, Class<? extends Enum> enumClass, boolean outputProperties) {
            return I18n.i18n(locale(true), bundleSpec, enumClass, outputProperties);
        }

        protected CTX me() {
            return (CTX) this;
        }

        @Override
        public <T> T renderArg(String name) {
            return (T) renderArgs.get(name);
        }

        @Override
        public CTX renderArg(String name, Object val) {
            renderArgs.put(name, val);
            return me();
        }

        // see https://github.com/actframework/actframework/issues/312
        public CTX fieldOutputVarCount(int count) {
            this.fieldOutputVarCount = count;
            return me();
        }

        public int fieldOutputVarCount() {
            return fieldOutputVarCount;
        }

        @Override
        public Map<String, Object> renderArgs() {
            return C.newMap(renderArgs);
        }

        protected boolean hasRenderArgs() {
            return !renderArgs.isEmpty();
        }

        /**
         * Associate a user attribute to the context. Could be used by third party
         * libraries or user application
         *
         * @param name the className used to reference the attribute
         * @param attr the attribute object
         * @return this context
         */
        public CTX attribute(String name, Object attr) {
            attributes.put(name, attr);
            return me();
        }

        public <T> T attribute(String name) {
            return $.cast(attributes.get(name));
        }

        public CTX removeAttribute(String name) {
            attributes.remove(name);
            return me();
        }

        @Override
        public Map<String, Object> attributes() {
            return attributes;
        }

        @Override
        public CTX addListener(Listener listener) {
            listenerList.add(listener);
            return me();
        }

        @Override
        public CTX addDestroyable(Destroyable resource) {
            destroyableList.add(resource);
            return me();
        }

        @Override
        public S.Buffer strBuf() {
            return strBuf.consumed() ? strBuf.reset() : S.newBuffer();
        }

        @Override
        public CTX addViolations(Map<String, ConstraintViolation> violations) {
            this.violations.putAll(violations);
            return me();
        }

        @Override
        public CTX addViolation(String property, ConstraintViolation violation) {
            this.violations.put(property, violation);
            return me();
        }

        @Override
        public boolean hasViolation() {
            return !violations.isEmpty();
        }

        @Override
        public Map<String, ConstraintViolation> violations() {
            return C.map(this.violations);
        }

        @Override
        public ConstraintViolation violation(String property) {
            return this.violations.get(property);
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
            app().jobManager().setJobProgressGauge(jobId, progress);
        }

        public ProgressGauge progress() {
            return progress;
        }

        public static ActContext.Base<?> currentContext() {
            ActContext.Base<?> ctx = ActionContext.current();
            if (null != ctx) {
                return ctx;
            }
            ctx = CliContext.current();
            if (null != ctx) {
                return ctx;
            }
            ctx = MailerContext.current();
            if (null != ctx) {
                return ctx;
            }
            return null;
        }

        public static Class<? extends ActContext> currentContextType() {
            ActContext ctx = currentContext();
            return null == ctx ? null : ctx.getClass();
        }

        public static String dataPattern() {
            ActContext<?> current = currentContext();
            return null == current ? null : current.pattern();
        }
    }
}
