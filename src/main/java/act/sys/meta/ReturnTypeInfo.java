package act.sys.meta;

import act.asm.Type;

public class ReturnTypeInfo {
    private Type type;
    private Type componentType;

    public ReturnTypeInfo() {
        this(Type.VOID_TYPE);
    }

    private ReturnTypeInfo(Type type) {
        this.type = null == type ? Type.VOID_TYPE : type;
    }

    public Type type() {
        return type;
    }

    public ReturnTypeInfo componentType(Type type) {
        componentType = type;
        return this;
    }

    public Type componentType() {
        return componentType;
    }

    public boolean hasReturn() {
        return type != Type.VOID_TYPE;
    }

    public static ReturnTypeInfo of(Type type) {
        return new ReturnTypeInfo(type);
    }
}
