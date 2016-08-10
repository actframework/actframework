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

    public void applyTo($.Func0<Object> beanSource, ActContext context) {
        Object o = loader.load(null, context, true);
        if (null == o) {
            return;
        }
        try {
            field.set(beanSource.apply(), o);
        } catch (Exception e) {
            throw new InjectException(e);
        }
    }
}
