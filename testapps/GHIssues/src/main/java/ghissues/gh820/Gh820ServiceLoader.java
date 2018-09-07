package ghissues.gh820;

import org.osgl.inject.BeanSpec;
import org.osgl.inject.GenericTypedBeanLoader;
import org.osgl.util.E;

import java.lang.reflect.Type;
import java.util.List;

public class Gh820ServiceLoader implements GenericTypedBeanLoader<Gh820Service> {
    @Override
    public Gh820Service load(BeanSpec spec) {
        List<Type> typeParams = spec.typeParams();
        E.illegalArgumentIf(typeParams.isEmpty(), "Expected type parameters in " + spec);
        Type type = typeParams.get(0);
        if (type == String.class) {
            return new Gh820StringService();
        } else if (type == Integer.class) {
            return new Gh820IntService();
        }
        throw E.unsupport("Unsupported type: " + type);
    }
}
