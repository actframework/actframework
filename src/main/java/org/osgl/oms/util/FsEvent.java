package org.osgl.oms.util;

import org.osgl.util.C;

import java.util.Collection;
import java.util.List;

public class FsEvent {

    public static enum Kind {
        CREATE, DELETE, MODIFY
    }

    private List<String> paths;
    private Kind kind;

    public FsEvent(Kind kind, String... paths) {
        this.paths = C.listOf(paths);
        this.kind = kind;
    }

    public FsEvent(Kind kind, Collection<String> paths) {
        this.paths = C.list(paths);
        this.kind = kind;
    }

    public List<String> paths() {
        return this.paths;
    }

    public Kind kind() {
        return this.kind;
    }

}
