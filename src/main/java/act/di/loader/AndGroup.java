package act.di.loader;

import org.osgl.Osgl;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 *
 */
public class AndGroup extends BeanLoaderBase {

    @Override
    public Object load(Object hint, Object... options) {
        Annotation[] annotations = (Annotation[]) hint;
        return null;
    }

    @Override
    public List loadMultiple(Object hint, Object... options) {
        return null;
    }

    @Override
    public Osgl.Function filter(Object hint, Object... options) {
        return null;
    }
}
