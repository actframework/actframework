package act.di.builder;

import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Set;

public class ArrayListBuilder extends CollectionBuilder<ArrayList> {

    public ArrayListBuilder(Class<ArrayList> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    public Set<Class<? extends ArrayList>> supportedClasses() {
        Set<Class<? extends ArrayList>> set = C.newSet();
        set.add(ArrayList.class);
        return set;
    }

    @Override
    protected ArrayList createInstance() {
        return new ArrayList();
    }
}
