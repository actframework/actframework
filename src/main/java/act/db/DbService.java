package act.db;

import act.app.App;
import act.app.AppServiceBase;
import org.osgl.util.E;

public abstract class DbService extends AppServiceBase<DbService> {

    protected DbService(App app) {
        super(app);
    }

    @Override
    protected abstract void releaseResources();

    /**
     * Get a Dao from a model type. Note the model type is not
     * generic parameter bound as the framework might enhance a application
     * model class to implement the {@link Model} interface
     *
     * @param modelType
     * @param <DAO>
     * @return
     */
    public <DAO extends Dao> DAO dao(Class<?> modelType) {
        throw E.unsupport("to be enhanced");
    }
}
