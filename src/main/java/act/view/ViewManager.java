package act.view;

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

import static act.Destroyable.Util.tryDestroyAll;

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.conf.AppConfig;
import act.mail.MailerContext;
import act.util.*;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.util.*;

import java.util.*;
import javax.enterprise.context.ApplicationScoped;

/**
 * Manage different view solutions
 */
public class ViewManager extends LogSupportedDestroyableBase {

    private C.List<View> viewList = C.newList();
    private Map<String, ActionViewVarDef> implicitActionViewVariables = new HashMap<>();
    private Map<String, MailerViewVarDef> implicitMailerViewVariables = new HashMap<>();
    private Map<H.Format, View> directViewQuickLookup = new HashMap<>();
    private Set<H.Format> directViewBlackList = new HashSet<>();

    private Map<String, VarDef> appDefined = new HashMap<>();
    private Map<String, Template> templateCache = new HashMap<>();
    private boolean multiViews = false;
    private Keyword.Style mailTemplateNamingStyle = Keyword.Style.CAMEL_CASE;

    void register(View view) {
        E.NPE(view);
        if (registered(view)) {
            throw new UnexpectedException("View[%s] already registered", view.name());
        }
        viewList.add(view);
    }

    void register(ImplicitVariableProvider implicitVariableProvider) {
        E.NPE(implicitVariableProvider);
        List<ActionViewVarDef> l0 = implicitVariableProvider.implicitActionViewVariables();
        for (ActionViewVarDef var : l0) {
            _register(var, implicitActionViewVariables, false);
        }
        List<MailerViewVarDef> l1 = implicitVariableProvider.implicitMailerViewVariables();
        for (MailerViewVarDef var : l1) {
            _register(var, implicitMailerViewVariables, false);
        }
    }

    void registerAppDefinedVar(ActionViewVarDef var) {
        _register(var, implicitActionViewVariables, true);
    }

    void registerAppDefinedVar(MailerViewVarDef var) {
        _register(var, implicitMailerViewVariables, true);
    }

    private <T extends VarDef> void _register(T var, Map<String, T> map, boolean fromApp) {
        if (map.containsKey(var.name())) {
            throw new UnexpectedException("Implicit variable[%s] has already been registered", var.name());
        }
        map.put(var.name(), var);
        if (fromApp) {
            appDefined.put(var.name(), var);
        }
    }

    public void clearAppDefinedVars() {
        for (String name : appDefined.keySet()) {
            implicitActionViewVariables.remove(name);
            implicitMailerViewVariables.remove(name);
        }
        appDefined.clear();
    }

    public void onAppStart() {
        int viewCount = viewList.size();
        multiViews = viewCount > 1;
    }

    public void reload(App app) {
        for (View view : viewList) {
            view.reload(app);
        }
    }

    public View view(String name) {
        $.Option<View> viewBag = findViewByName(name);
        return viewBag.isDefined() ? viewBag.get() : null;
    }

    public Template load(ActContext context) {
        Template cached = context.cachedTemplate();
        if (null != cached) {
            return cached;
        }

        AppConfig config = context.config();
        Template template;
        String templateContent = context.templateContent();
        if (S.notEmpty(templateContent)) {
            template = getInlineTemplate(context, config, templateContent);
        } else {
            TemplatePathResolver resolver = config.templatePathResolver();

            String path = resolver.resolve(context);
            if (context instanceof MailerContext) {
                String fileName = path.contains("/") ? S.cut(path).afterLast("/") : path;
                String mailTemplatePath = S.concat("mail/", fileName);
                template = getTemplate(context, config, mailTemplatePath);
                if (null == template) {
                    // try template file name variations - without prefix `send`
                    fileName = fileName.substring(4);
                    S.Pair pair = S.binarySplit(fileName, '.');
                    fileName = pair.left();
                    String suffix = "." + pair.right();
                    Keyword keyword = Keyword.of(fileName);
                    String variation = mailTemplateNamingStyle.toString(keyword);
                    mailTemplatePath = S.concat("mail/", variation, suffix);
                    template = getTemplate(context, config, mailTemplatePath);
                    if (null == template) {
                        for (Keyword.Style style : Keyword.Style.values()) {
                            if (mailTemplateNamingStyle == style) {
                                continue;
                            }
                            mailTemplateNamingStyle = style;
                            variation = mailTemplateNamingStyle.toString(keyword);
                            mailTemplatePath = S.concat("mail/", variation, suffix);
                            template = getTemplate(context, config, mailTemplatePath);
                            if (null != template) {
                                break;
                            }
                        }
                    }
                }
                if (null != template) {
                    context.templatePath(mailTemplatePath);
                    return template;
                }
            }

            template = getTemplate(context, config, path);
            if (null == template) {
                String amendedPath = resolver.resolveWithContextMethodPath(context);
                if (S.neq(amendedPath, path)) {
                    template = getTemplate(context, config, amendedPath);
                    if (null != template) {
                        context.templatePath(amendedPath);
                    }
                }
                if (null == template) {
                    // what if we want to render html from md?
                    if (path.endsWith(".html")) {
                        path = S.cut(path).beforeLast(".html");
                        path = path + ".md";
                        template = getTemplate(context, config, path);
                    }
                }
            }
        }
        return template;
    }

    /**
     * // created for GH352
     * Returns a Template instance by given path
     * @param path
     *      the path to the template
     * @return
     *      A template found by path or null if not found
     */
    @SuppressWarnings("unused")
    public Template getTemplate(String path) {
        ActContext.Base ctx = ActContext.Base.currentContext();
        if (null != ctx) {
            String curPath = ctx.templatePath();
            ctx.templateLiteral(path);
            try {
                return load(ctx);
            } finally {
                ctx.templatePath(curPath);
            }
        }
        final String templatePath = S.ensureStartsWith(path, '/');
        Template template = null;

        View defView = Act.appConfig().defaultView();

        if (null != defView) {
            template = !isTemplatePath(path) ? defView.loadInlineTemplate(path) : defView.loadTemplate(templatePath);
        }
        if (null == template && multiViews) {
            for (View view : viewList) {
                if (view == defView) continue;
                template = view.loadTemplate(templatePath);
                if (null != template) {
                    break;
                }
            }
        }
        if (null != template) {
            cache(path, template);
        }
        return template;
    }

    private Template getInlineTemplate(ActContext context, AppConfig config, String content) {
        View defView = config.defaultView();
        if (null != defView && defView.appliedTo(context)) {
            return defView.loadInlineTemplate(content);
        }
        return null;
    }

    private Template getTemplate(ActContext context, AppConfig config, String path) {
        Template template = cached(path);
        if (null != template) {
            return template;
        }

        final String templatePath = S.ensureStartsWith(path, '/');


        View defView = config.defaultView();

        if (null != defView && defView.appliedTo(context)) {
            template = defView.loadTemplate(templatePath);
        }
        if (null == template && multiViews) {
            for (View view : viewList) {
                if (view == defView || !view.appliedTo(context)) continue;
                template = view.loadTemplate(templatePath);
                if (null != template) {
                    break;
                }
            }
        }
        if (null != template && template.supportCache()) {
            context.cacheTemplate(template);
            cache(path, template);
        }
        return template;
    }

    public DirectRender loadDirectRender(ActionContext context) {
        H.Format accept = context.accept();
        if (directViewBlackList.contains(accept)) {
            return null;
        }
        View view = directViewQuickLookup.get(accept);
        if (null != view) {
            return view.directRenderFor(accept);
        }
        for (View view1 : viewList) {
            DirectRender dr = view1.directRenderFor(accept);
            if (null != dr) {
                directViewQuickLookup.put(accept, view1);
                return dr;
            }
        }
        directViewBlackList.add(accept);
        return null;
    }


    public Collection<ActionViewVarDef> implicitActionViewVariables() {
        return implicitActionViewVariables.values();
    }

    public Collection<MailerViewVarDef> implicitMailerViewVariables() {
        return implicitMailerViewVariables.values();
    }

    public void reset() {
        viewList.clear();
        templateCache.clear();
    }

    @Override
    protected void releaseResources() {
        tryDestroyAll(viewList, ApplicationScoped.class);
        viewList = null;

        implicitActionViewVariables.clear();
        implicitActionViewVariables = null;

        implicitMailerViewVariables.clear();
        implicitMailerViewVariables = null;

        templateCache.clear();
        templateCache = null;
    }

    private boolean registered(View view) {
        final String name = view.name().toUpperCase();
        return findViewByName(name).isDefined();
    }

    private $.Option<View> findViewByName(final String name) {
        return viewList.findFirst(new $.Predicate<View>() {
            @Override
            public boolean test(View view) {
                return view.name().toUpperCase().equals(name.toUpperCase());
            }
        });
    }

    private Template cached(String key) {
        return Act.isDev() ? null : templateCache.get(key);
    }

    private void cache(String key, Template template) {
        if (template.supportCache() && Act.isProd()) {
            templateCache.put(key, template);
        }
    }

    /**
     * Check if a given string is a template path or template content
     *
     * If the string contains anyone the following characters then we assume it
     * is content, otherwise it is path:
     *
     * * space characters
     * * non numeric-alphabetic characters except:
     * ** dot "."
     * ** dollar: "$"
     *
     * @param string
     *         the string to be tested
     * @return `true` if the string literal is template content or `false` otherwise
     */
    public static boolean isTemplatePath(String string) {
        int sz = string.length();
        if (sz == 0) {
            return true;
        }
        for (int i = 0; i < sz; ++i) {
            char c = string.charAt(i);
            switch (c) {
                case ' ':
                case '\t':
                case '\b':
                case '<':
                case '>':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '!':
                case '@':
                case '#':
                case '*':
                case '?':
                case '%':
                case '|':
                case ',':
                case ':':
                case ';':
                case '^':
                case '&':
                    return false;
            }
        }
        return true;
    }
}
