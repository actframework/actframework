package act.di.builder;

import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public class ListBuilder extends CollectionBuilder<List> {

    public ListBuilder(Class<List> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    public Set<Class<? extends List>> supportedClasses() {
        Set<Class<? extends List>> set = C.newSet();
        set.add(List.class);
        set.add(C.List.class);
        return set;
    }

    @Override
    protected List createInstance() {
        return C.newList();
    }
}
