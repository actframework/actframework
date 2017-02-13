package act.inject;

import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.Generics;

import javax.inject.Provider;
import java.lang.reflect.Type;
import java.util.List;

/**
 * App implemented {@link javax.inject.Provider} can extends this base class to automatically register to injector
 *
 * **Note** this class will automatically register to ACT's injector. Thus if you need to configure
 */
public abstract class ActProvider<T> implements Provider<T> {

    private final Class<T> targetType;

    public ActProvider() {
        targetType = exploreTypeInfo();
    }

    protected ActProvider(Class<T> targetType) {
        this.targetType = $.notNull(targetType);
    }

    public Class<T> targetType() {
        return targetType;
    }

    private Class<T> exploreTypeInfo() {
        List<Type> types = Generics.typeParamImplementations(getClass(), ActProvider.class);
        int sz = types.size();
        E.illegalStateIf(1 != sz, "generic type number not match");
        Type type = types.get(0);
        E.illegalArgumentIf(!(type instanceof Class), "generic type is not a class: %s", type);
        return (Class) type;
    }

}


