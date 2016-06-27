package testapp.model;

import act.Act;
import org.osgl.util.KVStore;

public abstract class ModelBase {

    private String id;

    private KVStore kv;

    public ModelBase() {
        id = Act.cuid();
        kv = new KVStore();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public KVStore getKv() {
        return new KVStore(kv);
    }

    public void setKv(KVStore kv) {
        this.kv = new KVStore(kv);
    }
}
