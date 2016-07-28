package act.di;

import act.app.App;
import act.app.event.AppEventId;
import act.util.SubTypeFinder;

public class DependencyInjectionListenerFinder extends SubTypeFinder<DependencyInjectionListener> {
    public DependencyInjectionListenerFinder() {
        super(DependencyInjectionListener.class);
    }

    @Override
    protected void found(final Class<? extends DependencyInjectionListener> target, final App app) {
        app.jobManager().on(AppEventId.DEPENDENCY_INJECTOR_LOADED, new Runnable() {
            @Override
            public void run() {
                DependencyInjector di = app.injector();
                di.registerDiListener(app.getInstance(target));
            }
        });
    }

}
