package act.app;

import act.db.Dao;

/**
 * A generic interface to allow find Dao instance from a model type class
 */
public interface DaoLocator {
    Dao dao(Class<?> modelType);
}
