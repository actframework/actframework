package act.model;

import org.osgl.util.E;

/**
 * The model base class
 * @param <MODEL_TYPE> the generic type of Model class
 * @param <ID_TYPE> the generic type of the ID (Key)
 */
public abstract class Model<MODEL_TYPE extends Model, ID_TYPE> {

    private ID_TYPE id;

    /**
     * Returns the ID (key) of this entity
     *
     * @return the id
     */
    public ID_TYPE _id() {
        return id;
    }

    /**
     * Set the ID(key) to this entity. This method assume the
     * entity is at <b>empty</b> state, i.e. there is no ID already
     * assigned to this entity before calling this method. Otherwise
     * the {@link IllegalStateException} will be thrown out
     * @param id the ID to be set to this entity
     * @return this entity itself
     * @throws IllegalStateException if ID has already set on this entity
     */
    public MODEL_TYPE _id(ID_TYPE id) throws IllegalStateException {
        E.illegalStateIf(null != this.id, "id already set on this entity");
        E.NPE(id);
        this.id = id;
        return _me();
    }

    /**
     * Returns {@code true} if the entity has not been saved yet
     * or {@code false} otherwise
     */
    public abstract boolean _isNew();

    /**
     * Returns a {@link Dao} object that can operate on this entity of
     * the entities with the same type.
     *
     * <p>Note this method needs to be enhanced by framework to be called</p>
     *
     * @return the {@code Dao} object
     */
    public static Dao _dao() {
        throw E.unsupport("to be enhanced");
    }

    protected final MODEL_TYPE _me() {
        return (MODEL_TYPE)this;
    }
}
