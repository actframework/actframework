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

import java.util.Objects;

/**
 * Stores meta information about an Entity field
 */
public class EntityFieldMetaInfo {

    enum Trait {
        CREATED, LAST_MODIFIED
    }

    private Trait trait;
    private String className;
    private String fieldName;
    private String columnName;

    public Trait getTrait() {
        return trait;
    }

    public void setTrait(Trait trait) {
        this.trait = trait;
    }

    public boolean isCreatedAt() {
        return Trait.CREATED == trait;
    }

    public boolean isLastModifiedAt() {
        return Trait.LAST_MODIFIED == trait;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityFieldMetaInfo that = (EntityFieldMetaInfo) o;
        return trait == that.trait &&
                Objects.equals(className, that.className) &&
                Objects.equals(fieldName, that.fieldName) &&
                Objects.equals(columnName, that.columnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trait, className, fieldName, columnName);
    }

    @Override
    public String toString() {
        return "EntityFieldMetaInfo{" +
                "className='" + className + '\'' +
                ", fieldName='" + fieldName + '\'' +
                '}';
    }

}
