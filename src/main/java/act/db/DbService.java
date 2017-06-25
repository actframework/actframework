package act.db;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Act;
import act.app.App;
import act.app.AppHolderBase;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;

public abstract class DbService extends AppHolderBase<DbService> {

    /**
     * This is deprecated, please use {@link #logger} instead
     */
    @Deprecated
    protected static final Logger _logger = LogManager.get(DbService.class);

    protected final Logger logger = LogManager.get(getClass());

    private String id;

    /**
     * Construct a `DbService` with service ID and the current application
     * @param id the service ID
     * @param app the current application
     */
    public DbService(String id, App app) {
        super(app);
        E.NPE(id);
        this.id = id;
    }

    /**
     * Returns the DB ID of the service
     * @return the service ID
     */
    public String id() {
        return id;
    }

    /**
     * Returns all model classes registered on this datasource
     *
     * @return model classes talk to this datasource
     */
    public Set<Class> modelClasses() {
        EntityClassRepository repo = Act.getInstance(EntityClassRepository.class);
        return repo.modelClasses(id());
    }

    @Override
    protected abstract void releaseResources();

    /**
     * Tells the framework whether this service init asynchronously or synchronously
     *
     * By default a db service is init synchronously
     *
     * **IMPORTANT** if the implementation of the db service overwrite this method and
     * return `true`, the implementation must raise a {@link DbServiceInitialized} event
     * once the async initialization process is finished
     *
     * @return `true` if this db service initialization asynchronously or `false` otherwise
     */
    public boolean initAsynchronously() {
        return false;
    }

    /**
     * Report if the db service has been initialized
     * @return `true` if the db service is initialized
     */
    public abstract boolean initialized();

    public abstract <DAO extends Dao> DAO defaultDao(Class<?> modelType);

    public abstract <DAO extends Dao> DAO newDaoInstance(Class<DAO> daoType);

    public abstract Class<? extends Annotation> entityAnnotationType();

    /**
     * Utility method to find the ID type from Model type. Could be used by sub class on {@link #defaultDao(Class)}
     * method implementation
     *
     * @param modelType the model type
     * @param idAnnotation the ID annotation
     * @return the ID type
     */
    @SuppressWarnings("unused")
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
