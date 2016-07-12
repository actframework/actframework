package act.di.builder;

import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class LinkedListBuilder extends CollectionBuilder<LinkedList> {

    public LinkedListBuilder(Class<LinkedList> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    public Set<Class<? extends LinkedList>> supportedClasses() {
        Set<Class<? extends LinkedList>> set = C.newSet();
        set.add(LinkedList.class);
        return set;
    }

    @Override
    protected LinkedList createInstance() {
        return new LinkedList();
    }
}
