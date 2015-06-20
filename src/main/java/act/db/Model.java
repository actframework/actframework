package act.db;

public interface Model<ID_TYPE, MODEL_TYPE extends Model<ID_TYPE, MODEL_TYPE>> {
    ID_TYPE _id();
    MODEL_TYPE _id(ID_TYPE id);
    boolean _isNew();
}
