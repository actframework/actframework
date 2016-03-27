package act.data.util;

import act.app.App;
import org.osgl.util.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActPropertyHandlerFactory extends ReflectionPropertyHandlerFactory {

    private ActObjectFactory objectFactory;
    private ActStringValueResolver stringValueResolver;

    public ActPropertyHandlerFactory(App app) {
        this.objectFactory = new ActObjectFactory(app);
        this.stringValueResolver = new ActStringValueResolver(app);
    }

    @Override
    protected PropertySetter newSetter(Class c, Method m, Field f) {
        return new ReflectionPropertySetter(objectFactory, stringValueResolver, c, m, f);
    }

    @Override
    protected PropertyGetter newGetter(Class c, Method m, Field f) {
        return new ReflectionPropertyGetter(objectFactory, stringValueResolver, c, m, f, this);
    }
}
