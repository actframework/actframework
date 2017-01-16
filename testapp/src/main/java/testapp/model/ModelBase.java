package testapp.model;

import org.osgl.util.KVStore;

import java.util.UUID;

public abstract class ModelBase {

    private String id;

    private KVStore kv;

    public ModelBase() {
        id = UUID.randomUUID().toString();
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
