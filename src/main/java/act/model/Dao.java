package act.model;

/**
 * The Data Access Object interface
 * @param <ID_TYPE> the generic key type
 * @param <MODEL_TYPE> the generic model type
 */
public interface Dao<ID_TYPE, MODEL_TYPE extends Model<MODEL_TYPE, ID_TYPE>> {
    MODEL_TYPE findById(ID_TYPE id);
}
