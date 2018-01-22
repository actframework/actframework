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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Keep track meta information about an entity class
 */
public class EntityClassMetaInfo {

    private String className;
    private String entityName;
    private Map<String, EntityFieldMetaInfo> fields = new HashMap<>();

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public EntityFieldMetaInfo fieldInfo(String fieldName) {
        return fields.get(fieldName);
    }

    public EntityFieldMetaInfo getOrCreateFieldInfo(String fieldName) {
        EntityFieldMetaInfo fieldInfo = fields.get(fieldName);
        if (null == fieldInfo) {
            fieldInfo = new EntityFieldMetaInfo();
            fieldInfo.setClassName(className);
            fieldInfo.setFieldName(fieldName);
            fields.put(fieldName, fieldInfo);
        }
        return fieldInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityClassMetaInfo that = (EntityClassMetaInfo) o;
        return Objects.equals(className, that.className) &&
                Objects.equals(entityName, that.entityName) &&
                Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, entityName, fields);
    }

    @Override
    public String toString() {
        return "EntityClassMetaInfo{" +
                "className='" + className + '\'' +
                '}';
    }

    void clear() {
        fields.clear();
    }
}
