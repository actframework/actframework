package act.controller.meta;

import act.app.ActionContext;
import org.osgl.mvc.util.Binder;
import org.osgl.util.E;

public class BindAnnoInfo extends ParamAnnoInfoTraitBase {
    private Class<? extends Binder> binder;
    private String model;

    public BindAnnoInfo(int index) {
        super(index);
    }

    @Override
    public void attachTo(HandlerParamMetaInfo param) {
        param.bindAnno(this);
    }

    public BindAnnoInfo binder(Class<? extends Binder> binder) {
        E.NPE(binder);
        this.binder = binder;
        return this;
    }

    public Binder binder(ActionContext context) {
        return context.newInstance(binder);
    }

    public BindAnnoInfo model(String model) {
        this.model = model;
        return this;
    }

    public String model() {
        return model;
    }
}
