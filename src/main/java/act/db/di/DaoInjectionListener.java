package act.db.di;

import act.db.Dao;
import act.di.DiListener;

public interface DaoInjectionListener extends DiListener<Dao> {

    Class<? extends Dao> targetDaoType();

    /**
     * This allows the implementation to fetch the db service ID from DB annotation
     * @param modelType The component class
     */
    void modelType(Class<?> modelType);
}
