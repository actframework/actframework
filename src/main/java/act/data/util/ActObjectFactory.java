package act.data.util;

import act.app.App;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;

public class ActObjectFactory extends Osgl.F1<Class<?>, Object> {

    private App app;

    public ActObjectFactory(App app) {
        this.app = $.notNull(app);
    }

    @Override
    public Object apply(Class<?> aClass) throws NotAppliedException, Osgl.Break {
        return app.newInstance(aClass);
    }
}
