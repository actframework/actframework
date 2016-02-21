package act.db;

import act.app.App;
import act.util.General;
import act.util.SubTypeFinder2;

import java.lang.annotation.Annotation;

@SuppressWarnings("unused")
public class DaoFinder extends SubTypeFinder2<Dao> {

    public DaoFinder() {
        super(Dao.class);
    }

    @Override
    protected void found(Class<Dao> target, App app) {
        if (isGeneral(target)) {
            return;
        }
        Dao dao = app.newInstance(target);
        app.registerSingleton(dao);
    }

    private boolean isGeneral(Class c) {
        Annotation[] aa = c.getDeclaredAnnotations();
        for (Annotation a : aa) {
            if (a instanceof General) {
                return true;
            }
        }
        return false;
    }
}
