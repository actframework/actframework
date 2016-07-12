package act.di.builder;

import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class HashSetBuilder extends CollectionBuilder<HashSet> {

    public HashSetBuilder(Class<HashSet> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    public Set<Class<? extends HashSet>> supportedClasses() {
        Set<Class<? extends HashSet>> set = C.newSet();
        set.add(HashSet.class);
        return set;
    }

    @Override
    protected HashSet createInstance() {
        return new HashSet();
    }
}
