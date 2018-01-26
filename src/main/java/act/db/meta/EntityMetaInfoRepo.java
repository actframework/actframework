package act.db.meta;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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
import act.app.AppServiceBase;
import act.db.CreatedAt;
import act.db.LastModifiedAt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static act.db.meta.EntityFieldMetaInfo.Trait.CREATED;
import static act.db.meta.EntityFieldMetaInfo.Trait.ID;
import static act.db.meta.EntityFieldMetaInfo.Trait.LAST_MODIFIED;

/**
 * Stores meta information about entity classes. At the moment there
 * are two index table maintained in this class:
 *
 * * index field name by class name for {@link CreatedAt}
 * * index field name by class name for {@link LastModifiedAt}
 */
public class EntityMetaInfoRepo extends AppServiceBase<EntityMetaInfoRepo> {

    Map<String, EntityClassMetaInfo> lookup = new HashMap<>();
    Map<Class, EntityClassMetaInfo> lookup2 = new HashMap<>();

    EntityMetaInfoRepo(final App app) {
        super(app);
    }

    public void registerCreatedField(String className, String fieldName) {
        EntityClassMetaInfo info = getOrCreate(className);
        info.getOrCreateFieldInfo(fieldName).trait(CREATED);
    }

    public void registerLastModifiedField(String className, String fieldName) {
        EntityClassMetaInfo info = getOrCreate(className);
        info.getOrCreateFieldInfo(fieldName).trait(LAST_MODIFIED);
    }

    public void registerIdField(String className, String fieldName) {
        EntityClassMetaInfo info = getOrCreate(className);
        info.getOrCreateFieldInfo(fieldName).trait(ID);
    }

    public void registerColumnName(String className, String fieldName, String columnName) {
        EntityClassMetaInfo info = getOrCreate(className);
        info.getOrCreateFieldInfo(fieldName).columnName(columnName);
    }

    public Set<Class> entityClasses() {
        return lookup2.keySet();
    }

    public EntityClassMetaInfo classMetaInfo(Class<?> entityClass) {
        return lookup2.get(entityClass);
    }

    public EntityClassMetaInfo classMetaInfo(String className) {
        return lookup.get(className);
    }

    @Override
    protected void releaseResources() {
        for (EntityClassMetaInfo classInfo : lookup.values()) {
            classInfo.clear();
        }
        lookup.clear();
    }

    void register(Class<?> entityClass, EntityClassMetaInfo info) {
        lookup2.put(entityClass, info);
        lookup.put(entityClass.getName(), info);
    }

    private EntityClassMetaInfo getOrCreate(String className) {
        EntityClassMetaInfo info = lookup.get(className);
        if (null == info) {
            info = new EntityClassMetaInfo();
            info.className(className);
            lookup.put(className, info);
        }
        return info;
    }
}
