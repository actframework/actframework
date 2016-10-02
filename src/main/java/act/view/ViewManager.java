package act.view;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static act.Destroyable.Util.tryDestroyAll;

/**
 * Manage different view solutions
 */
public class ViewManager extends DestroyableBase {

    private C.List<View> viewList = C.newList();
    private C.List<ActionViewVarDef> implicitActionViewVariables = C.newList();
    private C.List<MailerViewVarDef> implicitMailerViewVariables = C.newList();
    private Map<String, View> preferredViews = new HashMap<String, View>();
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
            if (implicitActionViewVariables.contains(var)) {
                throw new UnexpectedException("Implicit variable[%s] has already been registered", var.name());
            }
            implicitActionViewVariables.add(var);
        }
        List<MailerViewVarDef> l1 = implicitVariableProvider.implicitMailerViewVariables();
        for (MailerViewVarDef var : l1) {
            if (implicitMailerViewVariables.contains(var)) {
                throw new UnexpectedException("Implicit variable[%s] has already been registered", var.name());
            }
            implicitMailerViewVariables.add(var);
        }
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
        AppConfig config = context.config();
        Template cached = context.cachedTemplate();
        if (null != cached) {
            return cached;
        }

        TemplatePathResolver resolver = config.templatePathResolver();
        String path = resolver.resolve(context);

        StringBuilder sb = S.builder();
        if (!path.startsWith("/")) {
            sb.append("/");
        }
        sb.append(path);
        String templatePath = sb.toString();

        Template template = null;

        if (multiViews) {
            View preferred = preferredViews.get(templatePath);
            if (null != preferred) {
                template = preferred.loadTemplate(templatePath, context);
                if (null != template) {
                    context.cacheTemplate(template);
                    return template;
                }
            }
        }

        View defView = config.defaultView();

        if (null != defView) {
            template = defView.loadTemplate(templatePath, context);
        }
        if (null == template && multiViews) {
            for (View view : viewList) {
                if (view == defView) continue;
                template = view.loadTemplate(templatePath, context);
                if (null != template) {
                    if (multiViews) {
                        preferredViews.put(templatePath, view);
                    }
                    break;
                }
            }
        } else if (multiViews) {
            preferredViews.put(templatePath, defView);
        }
        if (null != template) {
            context.cacheTemplate(template);
        }
        return template;
    }


    public List<ActionViewVarDef> implicitActionViewVariables() {
        return C.list(implicitActionViewVariables);
    }

    public List<MailerViewVarDef> implicitMailerViewVariables() {
        return C.list(implicitMailerViewVariables);
    }

    @Override
    protected void releaseResources() {
        tryDestroyAll(viewList, ApplicationScoped.class);
        viewList = null;

        implicitActionViewVariables.clear();
        implicitActionViewVariables = null;

        implicitMailerViewVariables.clear();
        implicitMailerViewVariables = null;

        preferredViews.clear();
        preferredViews = null;
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
}
