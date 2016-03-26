package act.db.di;

import act.app.DbServiceManager;
import act.db.DB;

import java.lang.annotation.Annotation;

public abstract class DaoInjectionListenerBase implements DaoInjectionListener {

    private Class<?> modelType;
    private String dbSvcId = DbServiceManager.DEFAULT;

    @Override
    public void modelType(Class<?> modelType) {
        this.modelType = modelType;
        DB db = declaredAnnotation(modelType, DB.class);
        if (null != db) {
            dbSvcId = db.value();
        }
    }

    protected String svcId() {
        return dbSvcId;
    }

    protected Class<?> modelType() {
        return modelType;
    }

    private <T extends Annotation> T declaredAnnotation(Class c, Class<T> annoClass) {
        Annotation[] aa = c.getDeclaredAnnotations();
        if (null == aa) {
            return null;
        }
        for (Annotation a : aa) {
            if (annoClass.isInstance(a)) {
                return (T) a;
            }
        }
        return null;
    }
}
