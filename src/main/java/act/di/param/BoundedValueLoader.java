package act.di.param;

import act.util.ActContext;
import org.osgl.mvc.util.Binder;

/**
 * Use {@link org.osgl.mvc.util.Binder} to load param value
 */
class BoundedValueLoader implements ParamValueLoader {

    private Binder binder;
    private String bindModel;

    BoundedValueLoader(Binder binder, String model) {
        this.binder = binder;
        this.bindModel = model;
    }

    @Override
    public Object load(Object bean, ActContext context, boolean noDefaultValue) {
        return binder.resolve(bean, bindModel, context);
    }
}
