package org.osgl.oms.view;

import org.osgl._;
import org.osgl.exception.UnexpectedException;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.conf.AppConfig;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;

/**
 * Manage different view solutions
 */
public class ViewManager {

    private C.List<View> viewList = C.newList();
    private C.List<VarDef> implicitVariables = C.newList();

    void register(View view) {
        E.NPE(view);
        if (registered(view)) {
            throw new UnexpectedException("View[%s] already registered", view.name());
        }
        viewList.add(view);
    }

    void register(ImplicitVariableProvider implicitVariableProvider) {
        E.NPE(implicitVariableProvider);
        List<VarDef> l = implicitVariableProvider.implicitVariables();
        for (VarDef var : l) {
            if (implicitVariables.contains(var)) {
                throw new UnexpectedException("Implicit variable[%s] has already been registered", var.name());
            }
            implicitVariables.add(var);
        }
    }

    public View view(String name) {
        _.Option<View> viewBag = findViewByName(name);
        return viewBag.isDefined() ? viewBag.get() : null;
    }

    public Template load(AppContext context) {
        AppConfig config = context.config();
        View defView = config.defaultView();
        Template template = null;
        if (null != defView) {
            template = defView.load(context);
            if (null != template) {
                return template;
            }
        }
        for (View view : viewList) {
            if (view == defView) continue;
            template = view.load(context);
            if (null != template) {
                return template;
            }
        }
        return null;
    }

    public List<VarDef> implicitVariables() {
        return C.list(implicitVariables);
    }

    private boolean registered(View view) {
        final String name = view.name().toUpperCase();
        return findViewByName(name).isDefined();
    }

    private _.Option<View> findViewByName(final String name) {
        return viewList.findFirst(new _.Predicate<View>() {
            @Override
            public boolean test(View view) {
                return view.name().toUpperCase().equals(name);
            }
        });
    }
}
