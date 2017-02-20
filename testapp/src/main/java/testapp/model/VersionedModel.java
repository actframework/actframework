package testapp.model;

import act.data.Versioned;
import org.osgl.util.S;

/**
 * Used to test etag cache
 */
public class VersionedModel implements Versioned {

    private String id;

    public VersionedModel() {
        this(S.random());
    }

    public VersionedModel(String id) {
        this.id = S.blank(id) ? S.random() : id;
    }

    @Override
    public String _version() {
        return id;
    }

    public static VersionedModel getNew() {
        return new VersionedModel();
    }

    public static VersionedModel getById(String id) {
        return new VersionedModel(id);
    }

    @Override
    public String toString() {
        return _version();
    }
}
