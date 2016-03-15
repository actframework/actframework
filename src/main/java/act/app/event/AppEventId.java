package act.app.event;

import act.app.App;

public enum AppEventId {
    EVENT_BUS_INITIALIZED() {
        @Override
        public AppEvent of(App app) {
            return new EventBusInitialized(app);
        }
    },
    CONFIG_LOADED() {
        @Override
        public AppEvent of(App app) {
            return new AppConfigLoaded(app);
        }
    },
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
    }, APP_ACT_PLUGIN_LOADED {
        @Override
        public AppEvent of(App app) {
            return new AppCodeScanned(app);
        }
    }, ROUTER_INITIALIZED {
        @Override
        public AppEvent of(App app) {
            return new AppRouterInitialized(app);
        }
    }, ROUTER_LOADED {
        @Override
        public AppEvent of(App app) {
            return new AppRouterLoaded(app);
        }
    }, DEPENDENCY_INJECTOR_LOADED {
        @Override
        public AppEvent of(App app) {
            return new AppDependencyInjectorLoaded(app);
        }
    }, DEPENDENCY_INJECTOR_PROVISIONED {
        @Override
        public AppEvent of(App app) {
            return new AppDependencyInjectorProvisioned(app);
        }
    }, SINGLETON_PROVISIONED {
        @Override
        public AppEvent of(App app) {
            return new SingletonProvisioned(app);
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
    },
    /**
     * The App POST_START event
     * is used for framework. Application shall not
     * use this event
     */
    POST_START() {
        @Override
        public AppEvent of(App app) {
            return new AppPostStart(app);
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
