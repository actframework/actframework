package act.di.air.builder;

import act.di.air.Builder;
import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.Set;

public class LinkedListBuilder extends CollectionBuilder<LinkedList> {

    public LinkedListBuilder(Class<LinkedList> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    protected LinkedList createInstance() {
        return new LinkedList();
    }

    public static class Factory implements Builder.Factory<LinkedList> {
        @Override
        public Builder<LinkedList> createBuilder(Class<LinkedList> targetClass, Annotation[] annotations, Type[] typeParams) {
            return new LinkedListBuilder(targetClass, annotations, typeParams);
        }

        @Override
        public Class<LinkedList> targetClass() {
            return LinkedList.class;
        }
    }
}
