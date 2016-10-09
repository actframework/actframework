package act.controller.meta;

import act.app.App;
import org.osgl.mvc.util.Binder;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;

public class BindAnnoInfo extends ParamAnnoInfoTraitBase {
    private List<Class<? extends Binder>> binders = C.newList();
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
        this.binders.add(binder);
        return this;
    }

    public List<Binder> binder(App app) {
        List<Binder> list = C.newList();
        for (Class<? extends Binder> binderClass: binders) {
            list.add(app.getInstance(binderClass));
        }
        return list;
    }

    public BindAnnoInfo model(String model) {
        this.model = model;
        return this;
    }

    public String model() {
        return model;
    }
}
