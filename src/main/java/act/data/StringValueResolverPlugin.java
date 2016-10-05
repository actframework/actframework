package act.data;

import act.plugin.Plugin;
import org.osgl.util.StringValueResolver;

public abstract class StringValueResolverPlugin<T> extends StringValueResolver<T> implements Plugin {

    @Override
    public void register() {
        StringValueResolver.addPredefinedResolver(targetType(), this);
    }

}
