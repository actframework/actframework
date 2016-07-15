package act.di.air.builder;

import act.di.air.Builder;
import javafx.util.BuilderFactory;
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
    protected HashSet createInstance() {
        return new HashSet();
    }

    public static class Factory implements Builder.Factory<HashSet> {
        @Override
        public Builder<HashSet> createBuilder(Class<HashSet> targetClass, Annotation[] annotations, Type[] typeParams) {
            return new HashSetBuilder(targetClass, annotations, typeParams);
        }

        @Override
        public Class<HashSet> targetClass() {
            return HashSet.class;
        }
    }
}
