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
     * Returns the last time this model has been updated and saved
     * @return the last modified time
     */
    TIME_TYPE _lastModified();

    /**
     * Returns the class represents the time type used to track
     * the model creation/updating events
     * @return the class of the time type
     */
    Class<TIME_TYPE> _timeType();
}
