package act.data;

import act.plugin.Plugin;
import org.osgl.mvc.util.StringValueResolver;

public abstract class StringValueResolverPlugin<T> extends StringValueResolver<T> implements Plugin {

    protected abstract Class<T> targetType();

    @Override
    public void register() {
        StringValueResolver.addPredefinedResolver(targetType(), this);
    }
}
