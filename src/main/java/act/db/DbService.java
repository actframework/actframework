package act.db;

import act.app.App;
import act.app.AppHolderBase;
import org.osgl.util.E;

import java.lang.annotation.Annotation;

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
}
