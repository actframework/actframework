package act.di.air.builder;

import act.di.air.Builder;
import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public class CListBuilder extends CollectionBuilder<C.List> {

    public CListBuilder(Class<? extends C.List> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    protected C.List createInstance() {
        return C.newList();
    }

    public static class Factory implements Builder.Factory<C.List> {
        @Override
        public Builder<C.List> createBuilder(Class<C.List> targetClass, Annotation[] annotations, Type[] typeParams) {
            return new CListBuilder(targetClass, annotations, typeParams);
        }

        @Override
        public Class<C.List> targetClass() {
            return C.List.class;
        }

    }
}
