package act.db;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2020 ActFramework
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
 * a `DbSession` encapsulates information that is required
 * to interact with database, e.g.
 *
 * * DbService - provides db configuration/connection
 * * (nested) TxScope and associated Tx spec
 * * tenantId - for multi-tenancy support
 * * target table(s)/collection(s)
 * * entity (row/document) filter including pagination spec
 * * field/property (column) projector
 * * operation - CRUD
 * * mapper - map result back to model entity
 *
 */
public class DbSession {

    private DbService db;

    public DbService db() {
        return db;
    }

}
