package act.db;

import act.data.Timestamped;
import org.osgl.$;

/**
 * The model with time tracking built-in
 * @param <TIMESTANP_TYPE> the time type
 */
public interface TimeTrackingModel<TIMESTANP_TYPE, TIMESTAMP_TYPE_RESOLVER extends $.Function<TIMESTANP_TYPE, Long>>
extends Timestamped {
    /**
     * Returns the first time this model is created and saved
     * @return the create time
     */
    TIMESTANP_TYPE _created();

    /**
     * Set the created timestamp.
     * <p>Note not to be used by application</p>
     * @param timestamp the timestamp
     */
    void _created(TIMESTANP_TYPE timestamp);

    /**
     * Returns the last time this model has been updated and saved
     * @return the last modified time
     */
    TIMESTANP_TYPE _lastModified();

    /**
     * Set the last modified timestamp
     * <p>Note not to be used by application</p>
     * @param timestamp the timestamp
     */
    void _lastModified(TIMESTANP_TYPE timestamp);

    /**
     * Returns the class represents the timestamp type used to track
     * the model creation/updating events
     * @return the class of the time type
     */
    Class<TIMESTANP_TYPE> _timestampType();

    /**
     * Returns the instance that can resolve the timestamp type into long
     * @return the timestamp resolver
     */
    TIMESTAMP_TYPE_RESOLVER _timestampTypeResolver();
}
