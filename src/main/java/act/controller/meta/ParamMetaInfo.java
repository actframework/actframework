package act.controller.meta;

import act.app.App;
import act.util.GenericAnnoInfo;
import org.osgl._;
import act.asm.Type;
import org.osgl.mvc.util.StringValueResolver;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

public class ParamMetaInfo {
    private String name;
    private Type type;
    private ParamAnnoInfo paramAnno;
    private BindAnnoInfo bindAnno;
    private List<GenericAnnoInfo> genericAnnoInfoList = C.newList();

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

    public String bindName() {
        String bindName = name;
        if (null != paramAnno) {
            if (S.notBlank(paramAnno.bindName())) {
                bindName = paramAnno.bindName();
            }
        }
        return bindName;
    }

    public Object defVal(Class<?> type) {
        if (paramAnno == null) return null;
        return paramAnno.defVal(type);
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

    ParamMetaInfo addGenericAnnotation(GenericAnnoInfo anno) {
        genericAnnoInfoList.add(anno);
        return this;
    }

    public List<GenericAnnoInfo> genericAnnoInfoList() {
        return C.list(genericAnnoInfoList);
    }

    @Override
    public int hashCode() {
        return _.hc(name, type);
    }

    @Override
    public String toString() {
        return S.fmt("%s %s", type.getClassName(), name);
    }
}
