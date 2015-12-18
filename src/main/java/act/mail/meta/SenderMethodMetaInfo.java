package act.mail.meta;

import act.asm.Label;
import act.asm.Type;
import act.controller.meta.HandlerMethodMetaInfo;
import act.controller.meta.LocalVariableMetaInfo;
import act.controller.meta.ParamMetaInfo;
import act.sys.meta.InvokeType;
import act.sys.meta.ReturnTypeInfo;
import act.util.AsmTypes;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Map;

public class SenderMethodMetaInfo extends DestroyableBase {
    private String name;
    private String configId;
    private InvokeType invokeType;
    private MailerClassMetaInfo clsInfo;
    private C.List<ParamMetaInfo> params = C.newList();
    private ReturnTypeInfo returnType;
    private Map<Label, Map<Integer, LocalVariableMetaInfo>> locals = C.newMap();
    private int appCtxLVT_id = -1;

    public SenderMethodMetaInfo(MailerClassMetaInfo clsInfo) {
        this.clsInfo = clsInfo;
    }

    @Override
    protected void releaseResources() {
        clsInfo.destroy();
        params.clear();
        locals.clear();
        super.releaseResources();
    }

    public MailerClassMetaInfo classInfo() {
        return clsInfo;
    }

    public SenderMethodMetaInfo name(String name) {
        this.name = name;
        return this;
    }

    public String name() {
        return name;
    }

    public String fullName() {
        return S.builder(clsInfo.className()).append(".").append(name()).toString();
    }

    public SenderMethodMetaInfo configId(String id) {
        configId = id;
        return this;
    }

    public String configId() {
        return null != configId ? configId : classInfo().configId();
    }

    public SenderMethodMetaInfo invokeStaticMethod() {
        invokeType = InvokeType.STATIC;
        return this;
    }

    public SenderMethodMetaInfo invokeInstanceMethod() {
        invokeType = InvokeType.VIRTUAL;
        return this;
    }

    public boolean isStatic() {
        return InvokeType.STATIC == invokeType;
    }

    public SenderMethodMetaInfo returnType(Type type) {
        returnType = ReturnTypeInfo.of(type);
        return this;
    }

    public SenderMethodMetaInfo appCtxLocalVariableTableIndex(int index) {
        appCtxLVT_id = index;
        return this;
    }

    public int appCtxLocalVariableTableIndex() {
        return appCtxLVT_id;
    }

    public Type returnType() {
        return returnType.type();
    }

    public Type returnComponentType() {
        return returnType.componentType();
    }

    public SenderMethodMetaInfo returnComponentType(Type type) {
        returnType.componentType(type);
        return this;
    }

    public boolean hasReturn() {
        return returnType.hasReturn();
    }

    public boolean hasLocalVariableTable() {
        return !locals.isEmpty();
    }

    public SenderMethodMetaInfo addParam(ParamMetaInfo param) {
        params.add(param);
        if (AsmTypes.ACTION_CONTEXT.equals(param.type())) {
        }
        return this;
    }

    public SenderMethodMetaInfo addLocal(LocalVariableMetaInfo local) {
        Label start = local.start();
        Map<Integer, LocalVariableMetaInfo> m = locals.get(start);
        if (null == m) {
            m = C.newMap();
            locals.put(start, m);
        }
        int index = local.index();
        E.illegalStateIf(m.containsKey(index), "Local variable index conflict");
        m.put(local.index(), local);
        return this;
    }

    public LocalVariableMetaInfo localVariable(int index, Label start) {
        Map<Integer, LocalVariableMetaInfo> l = locals.get(start);
        if (null == l) return null;
        return l.get(index);
    }

    public ParamMetaInfo param(int id) {
        return params.get(id);
    }

    public int paramCount() {
        return params.size();
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
        if (obj instanceof HandlerMethodMetaInfo) {
            HandlerMethodMetaInfo that = (HandlerMethodMetaInfo) obj;
            return $.eq(that.fullName(), fullName());
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = S.builder();
        sb.append(_invokeType())
                .append(_return())
                .append(fullName())
                .append("(")
                .append(_params())
                .append(")");
        return sb.toString();
    }

    private String _invokeType() {
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
        if (returnType.hasReturn()) {
            return returnType.type().getClassName() + " ";
        } else {
            return "";
        }
    }

    private String _params() {
        return S.join(", ", params.map(new $.Transformer<ParamMetaInfo, String>() {
            @Override
            public String transform(ParamMetaInfo paramMetaInfo) {
                return paramMetaInfo.type().getClassName();
            }
        }));
    }

}
