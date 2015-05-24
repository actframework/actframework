package act.controller.meta;

import act.asm.Type;

public class ReturnTypeInfo {
    private Type type;

    private ReturnTypeInfo() {
        this(Type.VOID_TYPE);
    }

    private ReturnTypeInfo(Type type) {
        this.type = null == type ? Type.VOID_TYPE : type;
    }

    public Type type() {
        return type;
    }

    public boolean hasReturn() {
        return type != Type.VOID_TYPE;
    }

    public static ReturnTypeInfo of(Type type) {
        return new ReturnTypeInfo(type);
    }
}
