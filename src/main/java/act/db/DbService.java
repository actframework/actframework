package act.db;

import act.app.App;
import act.app.AppServiceBase;
import org.osgl.util.E;

public abstract class DbService extends AppServiceBase<DbService> {

    private String id;

    protected DbService(String id, App app) {
        super(app);
        E.NPE(id);
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    protected abstract void releaseResources();

    protected abstract <DAO extends Dao> DAO defaultDao(Class<?> modelType);
}
