package act.inject.param;

import act.app.CliContext;
import act.util.ActContext;

/**
 * Load value from command line option that are mandatory
 */
class RequiredOptionLoader implements ParamValueLoader {
    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        CliContext ctx = (CliContext) context;
        return null;
    }
}
