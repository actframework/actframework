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

import act.util.SimpleBean;

public interface Model<ID_TYPE, MODEL_TYPE extends Model> extends SimpleBean {
    /**
     * Returns the ID (key) of this entity
     *
     * @return the id
     */
    ID_TYPE _id();

    /**
     * Set the ID(key) to this entity. This method assume the
     * entity is at <b>empty</b> state, i.e. there is no ID already
     * assigned to this entity before calling this method. Otherwise
     * the {@link IllegalStateException} will be thrown out
     * @param id the ID to be set to this entity
     * @return this entity itself
     * @throws IllegalStateException if ID has already set on this entity
     */
    MODEL_TYPE _id(ID_TYPE id);

    /**
     * Returns {@code true} if the entity is just created in memory or been
     * loaded from data storage
     */
    boolean _isNew();
}
