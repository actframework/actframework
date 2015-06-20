package act.db;

/**
 * The model with time tracking and auditing built in
 * @param <PRINCIPAL_TYPE> the principal type
 */
public interface AuditingModel<PRINCIPAL_TYPE> {
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

    /**
     * Return the class of principal used in this model
     * @return
     */
    Class<PRINCIPAL_TYPE> _principalType();

}

