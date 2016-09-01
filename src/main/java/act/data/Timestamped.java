package act.data;

/**
 * A `Timestamped` object has method to retrieve the last modified timestamp of the data
 */
public interface Timestamped {
    /**
     * Returns the timestamp of the data
     * @return the timestamp
     */
    long _timestamp();
}
