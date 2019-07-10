package act.app;

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

import act.Act;
import act.Destroyable;
import act.app.event.SysEventId;
import act.conf.AppConfig;
import act.db.*;
import act.db.util.*;
import act.event.*;
import act.util.ClassNode;
import org.osgl.$;
import org.osgl.exception.ConfigurationException;
import org.osgl.util.*;

import java.lang.reflect.Modifier;
import java.util.*;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class DbServiceManager extends AppServiceBase<DbServiceManager> implements DaoLocator {

    public static final String DEFAULT = DB.DEFAULT;

    // map service id to service instance
    private Map<String, DbService> serviceMap = new HashMap<>();

    // map model class to dao class
    private Map<Class<?>, Dao> modelDaoMap = new HashMap<>();

    private final Dictionary<DbService, DbService> asyncInitializers = new Hashtable<>();

    private String firstInstance = DEFAULT;

    @Inject
    public DbServiceManager(final App app) {
        super(app);
        EntityClassRepository.init(app);
        initServices(app.config());
        prepareAsyncInitializers();
        configureSequenceGenerator(app);

        final Runnable daoInitializer = new Runnable() {
            @Override
            public void run() {
                app.emit(SysEventId.DB_SVC_LOADED);
                initDao();
                app.emit(SysEventId.DB_SVC_PROVISIONED);
                app.getInstance(AuditHelper.class);
            }

            private void initDao() {
                ClassNode node = app.classLoader().classInfoRepository().node(Dao.class.getName());
                node.visitPublicNotAbstractTreeNodes(new $.Visitor<ClassNode>() {
                    private boolean isGeneral(Class c) {
                        return Generics.tryGetTypeParamImplementations(c, DaoBase.class).isEmpty();
                    }

                    @Override
                    public void visit(ClassNode classNode) throws $.Break {
                        Class<? extends Dao> daoType = app.classForName(classNode.name());
                        if (Modifier.isAbstract(daoType.getModifiers())) {
                            return;
                        }
                        if (isGeneral(daoType)) {
                            if (!daoType.getName().startsWith("act.")) {
                                warn("Ignore dao type[%s]: no type implementation found", daoType.getName());
                            }
                            return;
                        }
                        try {
                            Dao dao = $.cast(app.getInstance(daoType));
                            Class<?> modelType = dao.modelType();
                            DB db = modelType.getAnnotation(DB.class);
                            String svcId = null == db ? DEFAULT : db.value();
                            DbService dbService = dbService(svcId);
                            E.invalidConfigurationIf(null == dbService, "cannot find db service by id: %s", svcId);
                            dao = dbService.newDaoInstance(daoType);
                            modelDaoMap.put(modelType, dao);
                        } catch (Exception e) {
                            warn(e, "error loading DAO: %s", daoType);
                        }
                    }
                });
            }
        };

        final EventBus eventBus = app.eventBus();
        if (asyncInitializers.isEmpty()) {
            eventBus.bind(SysEventId.SINGLETON_PROVISIONED, new SysEventListenerBase() {
                @Override
                public void on(EventObject event) throws Exception {
                    daoInitializer.run();
                }
            });
        } else {
            eventBus.bind(DbServiceInitialized.class, new ActEventListenerBase<DbServiceInitialized>() {
                @Override
                public void on(DbServiceInitialized event) {
                    synchronized (asyncInitializers) {
                        asyncInitializers.remove(event.source());
                        if (asyncInitializers.isEmpty()) {
                            daoInitializer.run();
                        }
                    }
                }
            });
        }
    }

    boolean hasDbService() {
        return !serviceMap.isEmpty();
    }

    private void configureSequenceGenerator(final App app) {
        app.jobManager().on(SysEventId.DEPENDENCY_INJECTOR_PROVISIONED, "DbServiceManager:registerSequenceNumberGenerator", new Runnable() {
            @Override
            public void run() {
                _SequenceNumberGenerator seqGen = app.config().sequenceNumberGenerator();
                SequenceNumberGenerator.registerImpl(seqGen);
            }
        });
    }

    private void prepareAsyncInitializers() {
        for (Map.Entry<String, DbService> entry : serviceMap.entrySet()) {
            DbService service = entry.getValue();
            if (service.initAsynchronously()) {
                asyncInitializers.put(service, service);
            }
        }
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(C.newSet(serviceMap.values()), ApplicationScoped.class);
        serviceMap.clear();
        Destroyable.Util.tryDestroyAll(C.newSet(modelDaoMap.values()), ApplicationScoped.class);
        modelDaoMap.clear();
        firstInstance = DEFAULT;
    }

    @Override
    public Dao dao(Class<?> modelClass) {
        Dao dao = modelDaoMap.get(modelClass);
        if (null == dao) {
            String svcId = dbId(modelClass);
            DbService dbService = dbService(svcId);
            dao = dbService.defaultDao(modelClass);
            modelDaoMap.put(modelClass, dao);
        }
        return dao;
    }

    public <T extends DbService> T dbService(String id) {
        return (T)serviceMap.get(id);
    }

    public <T extends DbService> List<T> dbServicesByClass(Class<T> dbServiceClass) {
        // It is possible for same service instance link to different name, e.g. `default` a `db1` etc
        // thus we must add it to set first to prevent the same service appears multiple times in the list
        Set<T> set = new HashSet<>();
        for (DbService service : serviceMap.values()) {
            if (dbServiceClass.isInstance(service)) {
                set.add((T) service);
            }
        }
        return new ArrayList<>(set);
    }

    public Iterable<DbService> registeredServices() {
        return serviceMap.values();
    }

    private void initServices(AppConfig config) {
        DbManager dbManager = Act.dbManager();
        if (!dbManager.hasPlugin()) {
            info("DB service not initialized: No DB plugin found");
            return;
        }
        DbPlugin db = dbManager.theSolePlugin();
        Map<String, String> dbConf = config.subSet("db.");
        if (dbConf.isEmpty()) {
            if (null == db) {
                warn("DB service not initialized: need to specify default db service implementation");
                return;
            } else {
                info("DB configuration not found. Will try to init default service with the sole db plugin: %s", db);
                DbService svc = db.initDbService(DEFAULT, app(), new HashMap<String, String>());
                serviceMap.put(DEFAULT, svc);
                return;
            }
        }

        if (dbConf.containsKey("db.instances")) {
            String instances = dbConf.get("db.instances").toString();
            String[] sa = instances.split("[,\\s;:]+");
            for (String dbId: sa) {
                initService(dbId, dbConf);
            }
            firstInstance = sa[0];
            app().entityMetaInfoRepo().setDefaultAlias(firstInstance);
            app().service(EntityClassRepository.class).setDefaultAlias(firstInstance);
        }
        if (serviceMap.containsKey(DEFAULT)) return;
        // try init default service if conf found
        String dbId = null;
        if (dbConf.containsKey("db." + DEFAULT +".impl")) {
            dbId = DEFAULT;
        } else if (dbConf.containsKey("db.impl")) {
            dbId = "";
        }
        if (null != dbId) {
            initService(dbId, dbConf);
        } else if (serviceMap.size() == 1) {
            DbService svc = serviceMap.values().iterator().next();
            serviceMap.put(DEFAULT, svc);
            warn("db service configuration not found. Use the sole one db service[%s] as default service", svc.id());
        } else {
            if (serviceMap.isEmpty()) {
                if (null == db) {
                    warn("DB service not initialized: need to specify default db service implementation");
                } else {
                    info("Init default service with the sole db plugin: %s", db);
                    Map<String, String> svcConf = new HashMap<>();
                    String prefix = "db.";
                    for (String key : dbConf.keySet()) {
                        if (key.startsWith(prefix)) {
                            String o = dbConf.get(key);
                            svcConf.put(key.substring(prefix.length()), o);
                        }
                    }
                    DbService svc = db.initDbService(DEFAULT, app(), svcConf);
                    serviceMap.put(DEFAULT, svc);
                }
            } else {
                warn("Default service not specified. Use the first db instance as default service: %s", firstInstance);
                serviceMap.put(DEFAULT, serviceMap.get(firstInstance));
            }
        }
    }

    private void initService(String dbId, Map<String, String> conf) {
        Map<String, String> svcConf = new HashMap<>();
        String prefix = "db." + (S.blank(dbId) ? "" : dbId + ".");
        for (String key : conf.keySet()) {
            if (key.startsWith(prefix)) {
                String o = conf.get(key);
                svcConf.put(key.substring(prefix.length()), o);
            }
        }
        String impl = svcConf.remove("impl");
        String svcId = S.empty(dbId) ? DEFAULT : dbId;
        if (null == impl) {
            throw new ConfigurationException("Cannot init db service[%s]: implementation not specified", svcId);
        }
        DbPlugin plugin = Act.dbManager().plugin(impl);
        if (null == plugin) {
            throw new ConfigurationException("Cannot init db service[%s]: implementation not found: %s", svcId, impl);
        }
        DbService svc = plugin.initDbService(S.blank(dbId) ? DEFAULT : dbId, app(), svcConf);
        serviceMap.put(svcId, svc);
        info("db service[%s] initialized", svcId);
    }

    public static String dbId(Class<?> modelClass) {
        DB db = modelClass.getAnnotation(DB.class);
        if (null != db) {
            return db.value();
        }
        return DbServiceManager.DEFAULT;
    }

}
