package act.db;

import org.osgl.util.KV;

/**
 * The `ActiveRecord` interface specifies a special {@link Model} in that
 * the fields/columns could be implicitly defined by database
 */
public interface ActiveRecord extends KV {
}
