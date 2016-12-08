package act.util;

import act.app.App;
import org.osgl.mvc.annotation.Param;
import org.osgl.util.StringValueResolver;

public class StringValueResolverFinder extends SubTypeFinder<StringValueResolver> {

    public StringValueResolverFinder() {
        super(StringValueResolver.class);
    }

    @Override
    protected void found(Class<? extends StringValueResolver> target, App app) {
        if (target == Param.DEFAULT_RESOLVER.class) {
            return;
        }
        if (target.isAnnotationPresent(NoAutoRegister.class)) {
            return;
        }
        StringValueResolver resolver = app.getInstance(target);
        app.resolverManager().register(resolver.targetType(), resolver);
    }
}
