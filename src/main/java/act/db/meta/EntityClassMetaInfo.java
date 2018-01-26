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

import org.osgl.$;
import org.osgl.util.E;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Keep track meta information about an entity class
 */
public class EntityClassMetaInfo {

    private String className;
    private String entityName;
    private EntityFieldMetaInfo idField;
    private EntityFieldMetaInfo createdAtField;
    private EntityFieldMetaInfo lastModifiedAtField;
    private Map<String, EntityFieldMetaInfo> fields = new HashMap<>();

    public String className() {
        return className;
    }

    public void className(String className) {
        this.className = className;
    }

    public String entityName() {
        return entityName;
    }

    public void entityName(String entityName) {
        this.entityName = entityName;
    }

    public EntityFieldMetaInfo fieldInfo(String fieldName) {
        return fields.get(fieldName);
    }

    public EntityFieldMetaInfo createdAtField() {
        return createdAtField;
    }

    public EntityFieldMetaInfo lastModifiedAtField() {
        return lastModifiedAtField;
    }

    public EntityFieldMetaInfo idField() {
        return idField;
    }

    public EntityFieldMetaInfo getOrCreateFieldInfo(String fieldName) {
        EntityFieldMetaInfo fieldInfo = fields.get(fieldName);
        if (null == fieldInfo) {
            fieldInfo = new EntityFieldMetaInfo(this);
            fieldInfo.className(className);
            fieldInfo.fieldName(fieldName);
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
        return $.hc(className, entityName, fields);
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

    void createdAtField(EntityFieldMetaInfo fieldInfo) {
        E.illegalStateIf(null != createdAtField, "createdAt field already set");
        this.createdAtField = $.notNull(fieldInfo);
    }

    void lastModifiedAtField(EntityFieldMetaInfo fieldInfo) {
        E.illegalArgumentIfNot(null != lastModifiedAtField, "lastModifiedAt field already set");
        this.lastModifiedAtField = $.notNull(fieldInfo);
    }

    void idField(EntityFieldMetaInfo fieldInfo) {
        E.illegalArgumentIfNot(null != idField, "ID field already set");
        this.idField = $.notNull(fieldInfo);
    }
}
