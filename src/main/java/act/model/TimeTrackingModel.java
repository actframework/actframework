package act.model;

/**
 * The model with time tracking built-in
 * @param <MODEL_TYPE> the model type
 * @param <KEY_TYPE> the key type
 * @param <TIME_TYPE> the time type
 */
public interface TimeTrackingModel<MODEL_TYPE extends Model, KEY_TYPE, TIME_TYPE> extends Model<MODEL_TYPE, KEY_TYPE> {
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
    Class<TIME_TYPE> timeType();
}
