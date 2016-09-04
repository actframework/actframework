package act.db;

import act.app.App;
import act.app.AppHolderBase;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public abstract class DbService extends AppHolderBase<DbService> {

    private String id;

    public DbService(String id, App app) {
        super(app);
        E.NPE(id);
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    protected abstract void releaseResources();

    public abstract <DAO extends Dao> DAO defaultDao(Class<?> modelType);

    public abstract <DAO extends Dao> DAO newDaoInstance(Class<DAO> daoType);

    public abstract Class<? extends Annotation> entityAnnotationType();

    protected static Class<?> findModelIdTypeByAnnotation(Class<?> modelType, Class<? extends Annotation> idAnnotation) {
        Class<?> curClass = modelType;
        while (Object.class != curClass && null != curClass) {
            for (Field f : curClass.getDeclaredFields()) {
                if (f.isAnnotationPresent(idAnnotation)) {
                    return f.getType();
                }
            }
            curClass = curClass.getSuperclass();
        }
        return null;
    }
}
