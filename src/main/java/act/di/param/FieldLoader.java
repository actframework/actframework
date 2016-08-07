package act.di.param;

import act.util.ActContext;
import org.osgl.$;
import org.osgl.inject.InjectException;
import org.osgl.mvc.annotation.Param;

import java.lang.reflect.Field;

/**
 * Load instance loaded by {@link ParamValueLoader} into {@link java.lang.reflect.Field}
 */
class FieldLoader {
    private final Field field;
    private final ParamValueLoader loader;

    FieldLoader(Field field, ParamValueLoader loader) {
        this.field = $.notNull(field);
        this.loader = $.notNull(loader);
    }

    public void applyTo(Object bean, ActContext context) {
        Object o = loader.load(context);
        if (null == o) {
            return;
        }
        try {
            field.set(bean, o);
        } catch (Exception e) {
            throw new InjectException(e);
        }
    }
}
