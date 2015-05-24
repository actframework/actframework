package act.util;

import act.asm.Type;
import org.osgl.util.E;

public class AsmType<T> {
    private Class<T> cls;
    private Type type;

    public AsmType(Class<T> cls) {
        E.NPE(cls);
        this.cls = cls;
        this.type = Type.getType(cls);
    }

    public Type asmType() {
        return type;
    }

    public String className() {
        return cls.getName();
    }

    public String internalName() {
        return type.getInternalName();
    }

    public String desc() {
        return type.getDescriptor();
    }
}
