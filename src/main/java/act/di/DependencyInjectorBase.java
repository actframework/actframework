package act.di;

import act.app.App;
import act.app.AppServiceBase;

public abstract class DependencyInjectorBase<DI extends DependencyInjectorBase> extends AppServiceBase<DI> implements DependencyInjector<DI>{

    public DependencyInjectorBase(App app) {
        this(app, false);
    }

    protected DependencyInjectorBase(App app, boolean noRegister) {
        super(app, true);
        if (!noRegister) {
            app.injector(this);
        }
    }

    @Override
    protected void releaseResources() {
    }

}
