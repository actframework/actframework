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

import act.Act;
import act.app.App;
import act.conf.AppConfig;
import act.util.ActContext;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;

import static act.Destroyable.Util.tryDestroyAll;

/**
 * Manage different view solutions
 */
public class ViewManager extends DestroyableBase {

    private C.List<View> viewList = C.newList();
    private Map<String, ActionViewVarDef> implicitActionViewVariables = new HashMap<>();
    private Map<String, MailerViewVarDef> implicitMailerViewVariables = new HashMap<>();
    private Map<String, VarDef> appDefined = new HashMap<>();
    private Map<String, Template> templateCache = new HashMap<>();
    private boolean multiViews = false;

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
            template = getTemplate(context, config, path);
            if (null == template) {
                String amendedPath = resolver.resolveWithContextMethodPath(context);
                if (S.neq(amendedPath, path)) {
                    template = getTemplate(context, config, amendedPath);
                    if (null != template) {
                        context.templatePath(amendedPath);
                    }
                }
            }
        }
        return template;
    }

    public Template getTemplate(String path) {
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
            templateCache.put(path, template);
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
        Template template = templateCache.get(path);
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
        if (null != template) {
            context.cacheTemplate(template);
            templateCache.put(path, template);
        }
        return template;
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
     * @param string the string to be tested
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
