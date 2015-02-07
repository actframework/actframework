package org.osgl.oms.be.controller;

import org.osgl.oms.asm.Type;
import org.osgl.util.S;

public class ParamMetaInfo {
    private String name;
    private Type type;

    public ParamMetaInfo type(Type type) {
        this.type = type;
        return this;
    }

    public Type type() {
        return type;
    }

    public ParamMetaInfo name(String newName) {
        this.name = newName;
        return this;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return S.fmt("%s[%s]", name, type);
    }
}
