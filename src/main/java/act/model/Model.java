package act.model;

/**
 * The model interface
 * @param <MODEL_TYPE> the generic type of Model class
 * @param <ID_TYPE> the generic type of the ID (Key)
 */
public interface Model<MODEL_TYPE extends Model, ID_TYPE> {

    /**
     * Returns the ID (key) of this entity
     *
     * @return the id
     */
    ID_TYPE _id();


    /**
     * Returns {@code true} if the entity has not been saved yet
     * or {@code false} otherwise
     */
    boolean _isNew();

}
