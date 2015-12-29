package act.di;

import act.app.App;
import act.event.ActEvent;
import org.osgl.util.E;

/**
 * Used to pass class binding resolution to DI plugin(s)
 */
public abstract class DiBinder<T> extends ActEvent {

    private Class<T> targetClass;

    public DiBinder(Object source, Class<T> targetClass) {
        super(source);
        E.NPE(targetClass);
        this.targetClass = targetClass;
    }

    @Override
    public Class eventType() {
        return DiBinder.class;
    }

    public Class<T> targetClass() {
        return targetClass;
    }

    public abstract T resolve(App app);

}
