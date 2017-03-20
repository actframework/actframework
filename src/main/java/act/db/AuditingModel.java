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

/**
 * The model with time tracking and auditing built in
 * @param <PRINCIPAL_TYPE> the principal type
 */
public interface AuditingModel<PRINCIPAL_TYPE> {
    /**
     * Returns the principal who created and saved this model
     * @return the creator
     */
    PRINCIPAL_TYPE _creator();

    /**
     * Returns the principal who is the last person modified and saved this model
     * @return the last modifier
     */
    PRINCIPAL_TYPE _lastModifier();

    /**
     * Return the class of principal used in this model
     * @return
     */
    Class<PRINCIPAL_TYPE> _principalType();

}

