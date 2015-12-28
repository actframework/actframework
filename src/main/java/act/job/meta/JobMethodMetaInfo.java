package act.job.meta;

import act.asm.Type;
import act.sys.meta.InvokeType;
import act.sys.meta.ReturnTypeInfo;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.util.S;

public class JobMethodMetaInfo extends DestroyableBase {
    private String id;
    private String name;
    private InvokeType invokeType;
    private JobClassMetaInfo clsInfo;
    private ReturnTypeInfo returnType;

    public JobMethodMetaInfo(JobClassMetaInfo clsInfo) {
        this.clsInfo = clsInfo;
    }

    @Override
    protected void releaseResources() {
        clsInfo.destroy();
        super.releaseResources();
    }

    public JobClassMetaInfo classInfo() {
        return clsInfo;
    }

    public JobMethodMetaInfo name(String name) {
        this.name = name;
        return this;
    }

    public String name() {
        return name;
    }

    public String fullName() {
        return S.builder(clsInfo.className()).append(".").append(name()).toString();
    }

    public JobMethodMetaInfo id(String id) {
        this.id = id;
        return this;
    }

    public String id() {
        return S.blank(id) ? fullName() : id;
    }

    public JobMethodMetaInfo invokeStaticMethod() {
        invokeType = InvokeType.STATIC;
        return this;
    }

    public JobMethodMetaInfo invokeInstanceMethod() {
        invokeType = InvokeType.VIRTUAL;
        return this;
    }

    public boolean isStatic() {
        return InvokeType.STATIC == invokeType;
    }

    public JobMethodMetaInfo returnType(Type type) {
        returnType = ReturnTypeInfo.of(type);
        return this;
    }

    public Type returnType() {
        return returnType.type();
    }

    public boolean hasReturn() {
        return returnType.hasReturn();
    }

    @Override
    public int hashCode() {
        return $.hc(fullName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof JobMethodMetaInfo) {
            JobMethodMetaInfo that = (JobMethodMetaInfo) obj;
            return $.eq(that.fullName(), fullName());
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = S.builder();
        sb.append(_invokeType())
                .append(_return())
                .append(fullName());
        return sb.toString();
    }

    private String _invokeType() {
        if (null == invokeType) {
            return "";
        }
        switch (invokeType) {
            case VIRTUAL:
                return "";
            case STATIC:
                return "static ";
            default:
                assert false;
                return "";
        }
    }

    private String _return() {
        if (null == returnType) {
            return " ";
        }
        if (returnType.hasReturn()) {
            return returnType.type().getClassName() + " ";
        } else {
            return "";
        }
    }

}
