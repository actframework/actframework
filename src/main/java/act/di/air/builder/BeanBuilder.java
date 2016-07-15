package act.di.air.builder;

import act.di.air.Builder;
import act.di.loader.BeanLoaderHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * A builder that relies on a {@link act.di.BeanLoader} to initialize a single
 * object instance
 */
public class BeanBuilder<T> extends Builder<T> {

    protected final BeanLoaderHelper helper;

    public BeanBuilder(Class<T> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
        helper = new BeanLoaderHelper(annotations, typeParameters);
    }

    @Override
    protected T createInstance() {
        return (T) helper.loadOne();
    }

    @Override
    protected void initializeInstance(T instance) {
        // nothing need to done here
    }
}
