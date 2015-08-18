package act.app.event;

import act.app.App;

public enum AppEventId {
    DB_SVC_LOADED () {
        @Override
        public AppEvent of(App app) {
            return new AppDbSvcLoaded(app);
        }
    }, PRE_LOAD_CLASSES () {
        @Override
        public AppEvent of(App app) {
            return new AppPreLoadClasses(app);
        }
    }, CLASS_LOADER_INITIALIZED () {
        @Override
        public AppEvent of(App app) {
            return new AppClassLoaderInitialized(app);
        }
    }, CLASS_LOADED {
        @Override
        public AppEvent of(App app) {
            return new AppClassLoaded(app);
        }
    }, APP_CODE_SCANNED {
        @Override
        public AppEvent of(App app) {
            return new AppCodeScanned(app);
        }
    }, DEPENDENCY_INJECTOR_LOADED {
        @Override
        public AppEvent of(App app) {
            return new AppDependencyInjectorLoaded(app);
        }
    }, PRE_START () {
        @Override
        public AppEvent of(App app) {
            return new AppPreStart(app);
        }
    }, START() {
        @Override
        public AppEvent of(App app) {
            return new AppStart(app);
        }
    }, STOP () {
        @Override
        public AppEvent of(App app) {
            return new AppStop(app);
        }
    }
    ;

    public abstract AppEvent of(App app);
}
