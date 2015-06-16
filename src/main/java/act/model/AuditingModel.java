package act.model;

/**
 * The model with time tracking and auditing built in
 * @param <MODEL_TYPE> the model type
 * @param <KEY_TYPE> the key type
 * @param <TIME_TYPE> the time type
 * @param <PRINCIPAL_TYPE> the principal type
 */
public interface AuditingModel<MODEL_TYPE extends Model, KEY_TYPE, TIME_TYPE, PRINCIPAL_TYPE>
        extends TimeTrackingModel<MODEL_TYPE, KEY_TYPE, TIME_TYPE> {
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

}

