package act.db2;

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
