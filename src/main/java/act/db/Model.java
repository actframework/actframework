package act.db;

public interface Model<ID_TYPE, MODEL_TYPE extends Model> {
    /**
     * Returns the ID (key) of this entity
     *
     * @return the id
     */
    ID_TYPE _id();

    /**
     * Set the ID(key) to this entity. This method assume the
     * entity is at <b>empty</b> state, i.e. there is no ID already
     * assigned to this entity before calling this method. Otherwise
     * the {@link IllegalStateException} will be thrown out
     * @param id the ID to be set to this entity
     * @return this entity itself
     * @throws IllegalStateException if ID has already set on this entity
     */
    MODEL_TYPE _id(ID_TYPE id);

    /**
     * Returns {@code true} if the entity is just created in memory or been
     * loaded from data storage
     */
    boolean _isNew();
}
