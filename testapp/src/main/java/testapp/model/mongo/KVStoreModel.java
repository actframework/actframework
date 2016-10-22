package testapp.model.mongo;

import act.db.morphia.MorphiaModel;
import org.osgl.util.KVStore;

/**
 * A morphia model with KVStore
 */
public class KVStoreModel<T extends MorphiaModel> extends MorphiaModel<T> {

    /**
     * A typesafe attribute storage
     */
    protected KVStore kv = new KVStore();


}
