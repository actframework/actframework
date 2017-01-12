package act.db;

import java.util.Map;
import java.util.Set;

/**
 * The `ActiveRecord` interface specifies a special {@link Model} in that
 * the fields/columns could be implicitly defined by database
 */
public interface ActiveRecord<ID_TYPE, MODEL_TYPE extends ActiveRecord> extends Model<ID_TYPE, MODEL_TYPE> {

    /**
     * Add a key/val pair into the active record
     * @param key the key
     * @param val the value
     * @return the active record instance
     */
    MODEL_TYPE putValue(String key, Object val);

    /**
     * Add all key/val pairs from specified kv map into this active record
     * @param kvMap the key/value pair
     * @return this active record instance
     */
    MODEL_TYPE putValues(Map<String, Object> kvMap);

    /**
     * Get value from the active record by key specified
     * @param key the key
     * @param <T> the generic type of the value
     * @return the value or `null` if not found
     */
    <T> T getValue(String key);

    /**
     * Export the key/val pairs from this active record into a map
     * @return the exported map contains all key/val pairs stored in this active record
     */
    Map<String, Object> toMap();

    /**
     * Get the size of the data stored in the active record
     * @return the active record size
     */
    int size();

    /**
     * Check if the active records has a value associated with key specified
     * @param key the key
     * @return `true` if there is value associated with the key in the record, or `false` otherwise
     */
    boolean containsKey(String key);

    /**
     * Returns a set of keys that has value stored in the active record
     * @return the key set
     */
    Set<String> keySet();

    /**
     * Returns a set of entries stored in the active record
     * @return the entry set
     */
    Set<Map.Entry<String, Object>> entrySet();

    /**
     * Returns a Map typed object backed by this active record
     * @return a Map backed by this active record
     */
    Map<String, Object> asMap();

}
