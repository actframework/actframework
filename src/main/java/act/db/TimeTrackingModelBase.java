package act.db;

public abstract class TimeTrackingModelBase<TIMESTAMP_TYPE, ID_TYPE, MODEL_TYPE extends ModelBase>
        extends ModelBase<ID_TYPE, MODEL_TYPE> implements TimeTrackingModel<TIMESTAMP_TYPE> {
    @Override
    public boolean _isNew() {
        return null == _created();
    }
}
