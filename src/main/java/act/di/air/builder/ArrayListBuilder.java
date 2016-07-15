package act.di.air.builder;

import act.di.air.Builder;
import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Set;

public class ArrayListBuilder extends CollectionBuilder<ArrayList> {

    public ArrayListBuilder(Class<ArrayList> targetClass, Annotation[] annotations, Type[] typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    protected ArrayList createInstance() {
        return new ArrayList();
    }

    public static class Factory implements Builder.Factory<ArrayList> {
        @Override
        public Builder<ArrayList> createBuilder(Class<ArrayList> targetClass, Annotation[] annotations, Type[] typeParams) {
            return new ArrayListBuilder(targetClass, annotations, typeParams);
        }

        @Override
        public Class<ArrayList> targetClass() {
            return ArrayList.class;
        }
    }
}
