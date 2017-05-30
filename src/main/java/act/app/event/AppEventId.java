package act.app.event;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
    CONFIG_PREMERGE() {
        @Override
        public AppEvent of(App app) {
            return new AppConfigPreMerge(app);
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
            return new AppActPluginLoaded(app);
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
    }, STATELESS_PROVISIONED {
        @Override
        public AppEvent of(App app) {
            return new SingletonProvisioned(app);
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
    },
    ACT_START() {
        @Override
        public AppEvent of(App app) {
            return new ActStart(app);
        }
    },
    STOP () {
        @Override
        public AppEvent of(App app) {
            return new AppStop(app);
        }
    }
    ;

    public abstract AppEvent of(App app);
}
