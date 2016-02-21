package act.db;

/**
 * The model with time tracking built-in
 * @param <TIME_TYPE> the time type
 */
public interface TimeTrackingModel<TIME_TYPE> {
    /**
     * Returns the first time this model is created and saved
     * @return the create time
     */
    TIME_TYPE _created();

    /**
     * Set the created timestamp.
     * <p>Note not to be used by application</p>
     * @param timestamp the timestamp
     */
    void _created(TIME_TYPE timestamp);

    /**
     * Returns the last time this model has been updated and saved
     * @return the last modified time
     */
    TIME_TYPE _lastModified();

    /**
     * Set the last modified timestamp
     * <p>Note not to be used by application</p>
     * @param timestamp the timestamp
     */
    void _lastModified(TIME_TYPE timestamp);

    /**
     * Returns the class represents the timestamp type used to track
     * the model creation/updating events
     * @return the class of the time type
     */
    Class<TIME_TYPE> _timestampType();
}
