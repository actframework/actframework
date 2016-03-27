package act.db.di;

import act.di.DependencyInjectionListener;

public interface DaoInjectionListener extends DependencyInjectionListener {

    /**
     * This allows the implementation to fetch the db service ID from DB annotation
     * @param modelType The component class
     */
    void modelType(Class<?> modelType);
}
