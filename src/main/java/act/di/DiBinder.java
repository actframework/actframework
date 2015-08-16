package act.di;

import act.app.App;
import act.event.ActEvent;

import java.util.EventObject;

/**
 * Used to pass class binding resolution to DI plugin(s)
 */
public abstract class DiBinder<T> extends EventObject {

    private Class<T> targetClass;

    public DiBinder(Object source) {
        super(source);
    }

    public Class<T> targetClass() {
        return targetClass;
    }

    public abstract T resolve(App app);

}
