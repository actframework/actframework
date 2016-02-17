package act.data.util;

import act.app.App;
import act.app.data.StringValueResolverManager;
import org.osgl.util.PropertySetter;
import org.osgl.util.ReflectionPropertyHandlerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActPropertyHandlerFactory extends ReflectionPropertyHandlerFactory {

    private StringValueResolverManager resolverManager;

    public ActPropertyHandlerFactory(StringValueResolverManager resolverManager) {
        this.resolverManager = resolverManager;
    }

    @Override
    protected PropertySetter newSetter(Class c, Method m, Field f) {
        return new ActReflectionPropertySetter(c, m, f, resolverManager);
    }
}
