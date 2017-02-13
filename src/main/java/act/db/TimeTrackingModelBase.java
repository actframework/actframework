package act.db;

import act.data.Timestamped;
import act.inject.param.NoBind;
import org.osgl.$;

@NoBind
public abstract class TimeTrackingModelBase<
        ID_TYPE, MODEL_TYPE extends ModelBase,
        TIMESTAMP_TYPE, TIMESTAMP_TYPE_RESOLVER extends $.Function<TIMESTAMP_TYPE, Long>
        > extends ModelBase<ID_TYPE, MODEL_TYPE>
        implements TimeTrackingModel<TIMESTAMP_TYPE, TIMESTAMP_TYPE_RESOLVER>, Timestamped {
    @Override
    public boolean _isNew() {
        return null == _created();
    }

    @Override
    public long _timestamp() {
        return _timestampTypeResolver().apply(_lastModified());
    }
}
