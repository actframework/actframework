package act.di.air.builder;

import act.di.air.Builder;
import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public class SetBuilder extends CollectionBuilder<Set> {

    public SetBuilder(Class<Set> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    protected Set createInstance() {
        return C.newSet();
    }

    public static class Factory implements Builder.Factory<Set> {
        @Override
        public Builder<Set> createBuilder(Class<Set> targetClass, Annotation[] annotations, Type[] typeParams) {
            return new SetBuilder(targetClass, annotations, typeParams);
        }

        @Override
        public Class<Set> targetClass() {
            return Set.class;
        }
    }
}
