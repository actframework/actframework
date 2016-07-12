package act.di.builder;

import act.di.Builder;
import act.di.loader.BeanLoaderHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Support injection some common {@link java.util.Collection collections}
 */
public abstract class CollectionBuilder<T extends Collection> extends Builder<T> {

    private BeanLoaderHelper helper;

    public CollectionBuilder(Class<T> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
        helper = new BeanLoaderHelper(annotations, typeParameters);
    }

    @Override
    protected void initializeInstance(T instance) {
        for (Object o : helper.load()) {
            instance.add(o);
        }
    }
}
