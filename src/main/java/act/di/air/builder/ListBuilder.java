package act.di.air.builder;

import act.di.air.Builder;
import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public class ListBuilder extends CollectionBuilder<List> {

    public ListBuilder(Class<? extends List> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    protected List createInstance() {
        return C.newList();
    }

    public static class ListBuilderFactory implements Builder.Factory<List> {
        @Override
        public Builder<List> createBuilder(Class<List> targetClass, Annotation[] annotations, Type[] typeParams) {
            return new ListBuilder(targetClass, annotations, typeParams);
        }

        @Override
        public Class<List> targetClass() {
            return List.class;
        }

    }

}
