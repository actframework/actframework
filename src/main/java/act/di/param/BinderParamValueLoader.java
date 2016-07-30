package act.di.param;

import act.util.ActContext;
import org.osgl.mvc.util.Binder;

/**
 * Use {@link org.osgl.mvc.util.Binder} to load param value
 */
class BinderParamValueLoader implements ParamValueLoader {

    private Binder<?> binder;
    private String bindModel;

    BinderParamValueLoader(Binder<?> binder, String model) {
        this.binder = binder;
        this.bindModel = model;
    }

    @Override
    public Object load(ActContext context) {
        return binder.resolve(bindModel, context);
    }
}
