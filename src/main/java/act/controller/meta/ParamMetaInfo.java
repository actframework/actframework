package act.controller.meta;

import act.app.App;
import act.asm.Type;
import act.util.GeneralAnnoInfo;
import org.osgl.$;
import org.osgl.mvc.util.StringValueResolver;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

public class ParamMetaInfo {
    private String name;
    private Type type;
    private Type componentType;
    private boolean context;
    private ParamAnnoInfo paramAnno;
    private BindAnnoInfo bindAnno;
    private List<GeneralAnnoInfo> generalAnnoInfoList = C.newList();

    public ParamMetaInfo type(Type type) {
        this.type = type;
        return this;
    }

    public Type type() {
        return type;
    }

    public ParamMetaInfo componentType(Type type) {
        this.componentType = type;
        return this;
    }

    public Type componentType() {
        return this.componentType;
    }

    public ParamMetaInfo name(String newName) {
        this.name = newName;
        return this;
    }

    public String name() {
        return name;
    }

    public String bindName() {
        String bindName = name;
        if (null != paramAnno) {
            if (S.notBlank(paramAnno.bindName())) {
                bindName = paramAnno.bindName();
            }
        }
        return bindName;
    }

    public ParamMetaInfo setContext() {
        this.context = true;
        return this;
    }

    public boolean isContext() {
        return context;
    }

    public Object defVal(Class<?> type) {
        if (paramAnno != null) return paramAnno.defVal(type);
        return null;
    }

    public boolean resolverDefined() {
        return null != paramAnno && null != paramAnno.resolver();
    }

    public StringValueResolver resolver(App app) {
        if (null == paramAnno) return null;
        Class<? extends StringValueResolver> c = paramAnno.resolver();
        if (null == c) return null;
        return app.newInstance(c);
    }

    public ParamAnnoInfo paramAnnoInfo() {
        return paramAnno;
    }

    ParamMetaInfo paramAnno(ParamAnnoInfo anno) {
        paramAnno = anno;
        return this;
    }

    public BindAnnoInfo bindAnnoInfo() {
        return bindAnno;
    }

    ParamMetaInfo bindAnno(BindAnnoInfo anno) {
        bindAnno = anno;
        return this;
    }

    public ParamMetaInfo addGeneralAnnotation(GeneralAnnoInfo anno) {
        generalAnnoInfoList.add(anno);
        return this;
    }

    public ParamMetaInfo addGeneralAnnotations(List<GeneralAnnoInfo> list) {
        generalAnnoInfoList.addAll(list);
        return this;
    }

    public List<GeneralAnnoInfo> generalAnnoInfoList() {
        return C.list(generalAnnoInfoList);
    }

    @Override
    public int hashCode() {
        return $.hc(name, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ParamMetaInfo) {
            ParamMetaInfo that = (ParamMetaInfo)obj;
            return $.eq(that.name, this.name) && $.eq(that.type, this.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return S.fmt("%s %s", type.getClassName(), name);
    }
}
