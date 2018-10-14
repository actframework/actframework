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

import act.util.ClassInfoRepository;
import act.util.ClassNode;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.*;

/**
 * Keep track meta information about an entity class
 */
public class EntityClassMetaInfo {

    private String className;
    private String entityName;
    private boolean hasEntityListeners;
    private EntityFieldMetaInfo idField;
    private EntityFieldMetaInfo createdAtField;
    private EntityFieldMetaInfo lastModifiedAtField;
    private EntityFieldMetaInfo createdByField;
    private EntityFieldMetaInfo lastModifiedByField;
    private Map<String, EntityFieldMetaInfo> fields = new HashMap<>();

    public String className() {
        return className;
    }

    public void className(String className) {
        this.className = className;
        this.entityName = S.afterLast(className, className.contains("?") ? "?" : ".");
    }

    public String entityName() {
        return entityName;
    }

    public void entityName(String entityName) {
        this.entityName = entityName;
    }

    public void foundEntityListenersAnnotation() {
        hasEntityListeners = true;
    }

    public EntityFieldMetaInfo fieldInfo(String fieldName) {
        return fields.get(fieldName);
    }

    public EntityFieldMetaInfo createdAtField() {
        return createdAtField;
    }

    public EntityFieldMetaInfo createdByField() {
        return createdByField;
    }

    public EntityFieldMetaInfo lastModifiedAtField() {
        return lastModifiedAtField;
    }

    public EntityFieldMetaInfo lastModifiedByField() {
        return lastModifiedByField;
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

    public boolean hasEntityListeners() {
        return hasEntityListeners;
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

    void mergeFromMappedSuperClasses(ClassInfoRepository classRepo, EntityMetaInfoRepo entityRepo) {
        ClassNode node = classRepo.node(className);
        ClassNode parent = node.parent();
        if (null != parent) {
            mergeFrom(parent, entityRepo);
        }
    }

    void clear() {
        fields.clear();
        hasEntityListeners = false;
    }

    void createdAtField(EntityFieldMetaInfo fieldInfo) {
        E.illegalStateIf(null != createdAtField, "createdAt field already set");
        this.createdAtField = $.requireNotNull(fieldInfo);
    }

    void createdByField(EntityFieldMetaInfo fieldInfo) {
        E.illegalStateIf(null != createdByField, "createdBy field already set");
        this.createdByField = $.requireNotNull(fieldInfo);
    }

    void lastModifiedAtField(EntityFieldMetaInfo fieldInfo) {
        E.illegalArgumentIf(null != lastModifiedAtField, "lastModifiedAt field already set");
        this.lastModifiedAtField = $.requireNotNull(fieldInfo);
    }

    void lastModifiedByField(EntityFieldMetaInfo fieldInfo) {
        E.illegalArgumentIf(null != lastModifiedByField, "lastModifiedBy field already set");
        this.lastModifiedByField = $.requireNotNull(fieldInfo);
    }

    void idField(EntityFieldMetaInfo fieldInfo) {
        E.illegalArgumentIf(null != idField, "ID field already set");
        this.idField = $.requireNotNull(fieldInfo);
    }

    private void mergeFrom(ClassNode parent, EntityMetaInfoRepo repo) {
        EntityClassMetaInfo parentInfo = repo.classMetaInfo(parent.name());
        if (null != parentInfo) {
            if (null == idField) {
                idField = parentInfo.idField;
            }
            if (null == createdAtField) {
                createdAtField = parentInfo.createdAtField;
            }
            if (null == lastModifiedAtField) {
                lastModifiedAtField = parentInfo.lastModifiedAtField;
            }
            if (null == createdByField) {
                createdByField = parentInfo.createdByField;
            }
            if (null == lastModifiedByField) {
                lastModifiedByField = parentInfo.lastModifiedByField;
            }
            for (Map.Entry<String, EntityFieldMetaInfo> entry : parentInfo.fields.entrySet()) {
                if (!fields.containsKey(entry.getKey())) {
                    fields.put(entry.getKey(), entry.getValue());
                }
            }
        }
        parent = parent.parent();
        if (null != parent) {
            mergeFrom(parent, repo);
        }
    }
}
