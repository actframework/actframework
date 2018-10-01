package testapp.model;

import act.data.Versioned;
import org.osgl.util.S;

/**
 * Used to test etag cache
 */
public class VersionedModel implements Versioned {

    private String version;

    public VersionedModel() {
        this(S.random());
    }

    public VersionedModel(String id) {
        this.version = S.blank(id) ? S.random() : id;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String _version() {
        return version;
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
