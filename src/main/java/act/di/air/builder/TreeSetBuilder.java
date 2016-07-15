package act.di.air.builder;

import act.di.air.Builder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.TreeSet;

public class TreeSetBuilder extends CollectionBuilder<TreeSet> {

    public TreeSetBuilder(Class<TreeSet> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    protected TreeSet createInstance() {
        return new TreeSet();
    }

    public class Factory implements Builder.Factory<TreeSet> {
        @Override
        public Builder<TreeSet> createBuilder(Class<TreeSet> targetClass, Annotation[] annotations, Type[] typeParams) {
            return new TreeSetBuilder(targetClass, annotations, typeParams);
        }

        @Override
        public Class<TreeSet> targetClass() {
            return TreeSet.class;
        }
    }
}
