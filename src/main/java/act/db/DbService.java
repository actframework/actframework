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
