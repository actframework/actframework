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

public enum SysEventId {
    /**
     * Emitted after {@link act.event.EventBus} initialized.
     */
    EVENT_BUS_INITIALIZED() {
        @Override
        public SysEvent of(App app) {
            return new EventBusInitialized(app);
        }
    },
    /**
     * Emitted after {@link App#config()} loaded.
     */
    CONFIG_LOADED() {
        @Override
        public SysEvent of(App app) {
            return new AppConfigLoaded(app);
        }
    },
    /**
     * Emitted if {@link act.app.conf.AppConfigurator} implementation
     * found and before it merged into {@link App#config()}.
     *
     * This event might not be triggered if there is no
     * {@link act.app.conf.AppConfigurator} defined in application.
     */
    CONFIG_PREMERGE() {
        @Override
        public SysEvent of(App app) {
            return new AppConfigPreMerge(app);
        }
    },
    /**
     * Emitted before {@link App#classLoader()} initialized
     */
    CLASS_LOADER_INITIALIZED () {
        @Override
        public SysEvent of(App app) {
            return new AppClassLoaderInitialized(app);
        }
    },
    /**
     * Emitted after all routers initialized.
     */
    ROUTER_INITIALIZED {
        @Override
        public SysEvent of(App app) {
            return new AppRouterInitialized(app);
        }
    },
    /**
     * Emitted after built-in and all file based route tables loaded into routers
     */
    ROUTER_LOADED {
        @Override
        public SysEvent of(App app) {
            return new AppRouterLoaded(app);
        }
    },
    /**
     * Emitted after {@link act.plugin.AppServicePlugin global application plugin} loaded.
     */
    APP_ACT_PLUGIN_LOADED {
        @Override
        public SysEvent of(App app) {
            return new AppActPluginLoaded(app);
        }
    },
    /**
     * Emitted before loading classes into {@link App#classLoader()}
     */
    PRE_LOAD_CLASSES () {
        @Override
        public SysEvent of(App app) {
            return new AppPreLoadClasses(app);
        }
    },
    /**
     * Emitted after application code scanned.
     */
    APP_CODE_SCANNED {
        @Override
        public SysEvent of(App app) {
            return new AppCodeScanned(app);
        }
    },
    /**
     * Emitted after classes loaded into {@link App#classLoader()}
     */
    CLASS_LOADED {
        @Override
        public SysEvent of(App app) {
            return new AppClassLoaded(app);
        }
    },
    /**
     * Emitted after dependency injector initialized.
     *
     * Note this event happened before {@link #DEPENDENCY_INJECTOR_LOADED}
     * and it marks a point when framework can register modules into
     * dependency injector.
     */
    DEPENDENCY_INJECTOR_INITIALIZED {
        @Override
        public SysEvent of(App app) {
            return new AppDependencyInjectorInitialized(app);
        }
    },
    /**
     * Emitted after dependency injector loaded.
     *
     * This event happened after all modules registered into dependency
     * injector.
     */
    DEPENDENCY_INJECTOR_LOADED {
        @Override
        public SysEvent of(App app) {
            return new AppDependencyInjectorLoaded(app);
        }
    },
    /**
     * Emitted after depenedency injector fully provisioned.
     *
     * This event happens after {@link #DEPENDENCY_INJECTOR_LOADED}
     */
    DEPENDENCY_INJECTOR_PROVISIONED {
        @Override
        public SysEvent of(App app) {
            return new AppDependencyInjectorProvisioned(app);
        }
    },
    /**
     * Emitted after all `@Singleton` instance registered.
     */
    SINGLETON_PROVISIONED {
        @Override
        public SysEvent of(App app) {
            return new SingletonProvisioned(app);
        }
    },
    /**
     * Emit once a database service has been loaded.
     *
     * When this event is emitted, the Dao of the db service hasn't been
     * initialized yet.
     *
     * @see #DB_SVC_PROVISIONED
     */
    DB_SVC_LOADED () {
        @Override
        public SysEvent of(App app) {
            return new AppDbSvcLoaded(app);
        }
    },
    /**
     * Emit once all Dao has been initialized for a database service.
     *
     * Note this event happens after {@link #DB_SVC_LOADED}
     */
    DB_SVC_PROVISIONED () {
        @Override
        public SysEvent of(App app) {
            return new AppDbSvcProvisioned(app);
        }
    },
    /**
     * Emitted to mark application is ready to start
     */
    PRE_START () {
        @Override
        public SysEvent of(App app) {
            return new AppPreStart(app);
        }
    },
    /**
     * Emitted when all '@Stateless' bean registered into
     * Singleton registry.
     *
     * This happens after {@link #PRE_START}.
     */
    STATELESS_PROVISIONED {
        @Override
        public SysEvent of(App app) {
            return new StatelessProvisioned(app);
        }
    },
    /**
     * Mark application has been started.
     *
     * This happens after {@link #STATELESS_PROVISIONED}.
     */
    START() {
        @Override
        public SysEvent of(App app) {
            return new AppStart(app);
        }
    },
    /**
     * Mark a point after {@link #START}.
     */
    POST_START() {
        @Override
        public SysEvent of(App app) {
            return new AppPostStart(app);
        }
    },
    /**
     * Mark a point after all {@link #POST_START} jobs has finished
     */
    POST_STARTED() {
        @Override
        public SysEvent of(App app) {
            return new AppPostStarted(app);
        }
    },
    /**
     * Mark Act instance fully started.
     *
     * This main difference between this event and {@link #START} is
     * in that {@link #START} will be triggered after app hot reloaded
     * in dev mode, while this even will only trigger one time.
     */
    ACT_START() {
        @Override
        public SysEvent of(App app) {
            return new ActStart(app);
        }
    },
    /**
     * Mark app stop event.
     *
     * This triggers when hotreload is happening.
     */
    STOP () {
        @Override
        public SysEvent of(App app) {
            return new AppStop(app);
        }
    }
    ;

    public abstract SysEvent of(App app);
}
