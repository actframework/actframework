package act.data.util;

import act.app.data.StringValueResolverManager;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.ReflectionPropertySetter;
import org.osgl.util.S;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActReflectionPropertySetter extends ReflectionPropertySetter {

    private static Logger logger = LogManager.get(ActReflectionPropertySetter.class);

    private StringValueResolverManager resolverManager;

    public ActReflectionPropertySetter(Class c, Method m, Field f, StringValueResolverManager resolverManager) {
        super(c, m, f);
        this.resolverManager = resolverManager;
    }

    @Override
    protected Object convertValue(Class requiredClass, Object value) {
        if (null == value) {
            return null;
        }
        if (requiredClass.isAssignableFrom(value.getClass())) {
            return value;
        }
        Object retVal = resolverManager.resolve(S.string(value), requiredClass);
        if (null == retVal) {
            logger.warn("Cannot resolve value %s for class %s", value, requiredClass);
        }
        return retVal;
    }
}
