package act.db;

import act.app.App;
import org.osgl.$;

/**
 * The model base class
 * @param <MODEL_TYPE> the generic type of Model class
 * @param <ID_TYPE> the generic type of the ID (Key)
 */
public abstract class ModelBase<ID_TYPE, MODEL_TYPE extends ModelBase>
implements Model<ID_TYPE, MODEL_TYPE> {

    // for JSON deserialization
    public ID_TYPE getId() {
        return _id();
    }

    // for JSON deserialization
    public void setId(ID_TYPE id) {
        _id(id);
    }

    /**
     * Returns a {@link Dao} object that can operate on this entity of
     * the entities with the same type.
     *
     * <p>Note this method needs to be enhanced by framework to be called</p>
     *
     * @return the {@code Dao} object
     */
    public static <T extends Dao>
    T dao() {
        return (T) App.instance().dbServiceManager().dao(ModelBase.class);
    }

    @Override
    public int hashCode() {
        return $.hc(getClass(), _id());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ($.eq(obj.getClass(), getClass())) {
            Model that = (Model) obj;
            return $.eq(that._id(), this._id());
        }
        return false;
    }

    protected final MODEL_TYPE _me() {
        return (MODEL_TYPE)this;
    }

    public static void main(String[] args) {
        ModelBase.dao();
    }
}
