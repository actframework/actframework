package act.db2;

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

import org.osgl.util.E;

import java.util.Map;

/**
 * A `DbService` provides methods to access a database source via underline service provider
 * object, e.g. JDBC `Connection` for SQL database or `MongoClient` for mongodb.
 *
 * A `DbService` instance require a group of database configuration to be setup, including:
 * - connection URL
 * - username
 * - password
 * - other provider specific database configurations
 */
public class DbService<RESULT_SET> {

    /**
     * Run query statement with optional parameters and returns the result set.
     *
     * @param queryStatement the query statement
     * @param parameters parameters
     * @return
     */
    public Map<String, Object> runQuery(String queryStatement, Object ... parameters) {
        throw E.tbd();
    }

}
