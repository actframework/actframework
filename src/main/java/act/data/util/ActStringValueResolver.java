package act.data.util;

import act.app.App;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;

public class ActStringValueResolver extends Osgl.F2<String, Class<?>, Object> {

    private App app;

    public ActStringValueResolver(App app) {
        this.app = $.notNull(app);
    }

    @Override
    public Object apply(String s, Class<?> aClass) throws NotAppliedException, Osgl.Break {
        return app.resolverManager().resolve(s, aClass);
    }

}
