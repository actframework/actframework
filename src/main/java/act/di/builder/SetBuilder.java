package act.di.builder;

import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public class SetBuilder extends CollectionBuilder<Set> {

    public SetBuilder(Class<Set> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    public Set<Class<? extends Set>> supportedClasses() {
        Set<Class<? extends Set>> set = C.newSet();
        set.add(Set.class);
        set.add(C.Set.class);
        return set;
    }

    @Override
    protected Set createInstance() {
        return C.newSet();
    }
}
