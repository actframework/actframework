package act.db;

import act.event.ActEvent;

/**
 * Raised by framework when calling {@link Dao#delete(Object)}
 */
public class DeleteEvent<MODEL_TYPE> extends ActEvent<MODEL_TYPE> {
    public DeleteEvent(MODEL_TYPE source) {
        super(source);
    }
}
