package act.util;

import act.app.App;
import act.app.event.AppEventId;

/**
 * An `AppSubTypeFinder` extends {@link SubTypeFinder} with an method to
 * apply the finder when application started.
 */
@SuppressWarnings("unused")
public abstract class AppSubTypeFinder<T> extends SubTypeFinder<T> {

    public AppSubTypeFinder(Class<T> target) {
        super(target);
        registerSingleton();
    }

    public AppSubTypeFinder(Class<T> target, AppEventId bindingEvent) {
        super(target, bindingEvent);
        registerSingleton();
    }

    private void registerSingleton() {
        App.instance().registerSingleton(getClass(), this);
    }

    @SuppressWarnings("unused")
    public static class _AppSubTypeFinderFinder extends SubTypeFinder<AppSubTypeFinder> {

        public _AppSubTypeFinderFinder() {
            super(AppSubTypeFinder.class, AppEventId.SINGLETON_PROVISIONED);
        }

        @Override
        protected void found(final Class<AppSubTypeFinder> target, App app) {
            AppSubTypeFinder finder = app.newInstance(target);
            finder.applyTo(app);
        }
    }
}
