package act.job.meta;

import act.app.App;
import act.asm.Type;
import act.controller.meta.ParamMetaInfo;
import act.sys.meta.InvokeType;
import act.sys.meta.ReturnTypeInfo;
import act.util.ClassNode;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.List;

public class JobMethodMetaInfo extends DestroyableBase {
    private String id;
    private String name;
    private InvokeType invokeType;
    private JobClassMetaInfo clsInfo;
    private ReturnTypeInfo returnType = new ReturnTypeInfo();
    private C.List<ParamMetaInfo> params = C.newList();

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

    public List<JobMethodMetaInfo> extendedJobMethodMetaInfoList(App app) {
        E.illegalStateIf(!classInfo().isAbstract(), "this job method meta info is not abstract");
        final C.List<JobMethodMetaInfo> list = C.newList();
        final JobClassMetaInfo clsInfo = classInfo();
        String clsName = clsInfo.className();
        ClassNode node = app.classLoader().classInfoRepository().node(clsName);
        if (null == node) {
            return list;
        }
        final JobMethodMetaInfo me = this;
        node.accept(new Osgl.Visitor<ClassNode>() {
            @Override
            public void visit(ClassNode classNode) throws Osgl.Break {
                if (!classNode.isAbstract() && classNode.isPublic()) {
                    JobClassMetaInfo subClsInfo = new JobClassMetaInfo().className(classNode.name());
                    JobMethodMetaInfo subMethodInfo = new JobMethodMetaInfo(subClsInfo);
                    if (me.isStatic()) {
                        subMethodInfo.invokeStaticMethod();
                    } else {
                        subMethodInfo.invokeInstanceMethod();
                    }
                    subMethodInfo.name(me.name());
                    subMethodInfo.returnType(me.returnType());
                    list.add(subMethodInfo);
                }
            }
        });
        return list;
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
