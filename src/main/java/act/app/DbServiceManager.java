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
import act.app.event.AppEventId;
import act.conf.AppConfig;
import act.db.*;
import act.db.util.SequenceNumberGenerator;
import act.db.util._SequenceNumberGenerator;
import act.event.AppEventListenerBase;
import act.util.ClassNode;
import act.util.General;
import org.osgl.$;
import org.osgl.exception.ConfigurationException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.rythmengine.utils.S;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class DbServiceManager extends AppServiceBase<DbServiceManager> implements DaoLocator {

    private static Logger logger = LogManager.get(DbServiceManager.class);

    public static final String DEFAULT = "default";

    // map service id to service instance
    private Map<String, DbService> serviceMap = C.newMap();

    // map model class to dao class
    private Map<Class<?>, Dao> modelDaoMap = C.newMap();

    @Inject
    public DbServiceManager(final App app) {
        super(app);
        EntityClassRepository.init(app);
        initServices(app.config());
        configureSequenceGenerator(app);
        app.eventBus().bind(AppEventId.SINGLETON_PROVISIONED, new AppEventListenerBase() {
            @Override
            public void on(EventObject event) throws Exception {
                ClassNode node = app.classLoader().classInfoRepository().node(Dao.class.getName());
                node.visitPublicNotAbstractTreeNodes(new $.Visitor<ClassNode>() {
                    private boolean isGeneral(Class c) {
                        Annotation[] aa = c.getDeclaredAnnotations();
                        for (Annotation a : aa) {
                            if (a instanceof General) {
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public void visit(ClassNode classNode) throws $.Break {
                        Class<? extends Dao> daoType = $.classForName(classNode.name(), app.classLoader());
                        if (isGeneral(daoType)) {
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
                            logger.warn(e, "error loading DAO: %s", daoType);
                        }
                    }
                });
            }
        });
    }

    private void configureSequenceGenerator(final App app) {
        app.jobManager().on(AppEventId.DEPENDENCY_INJECTOR_PROVISIONED, new Runnable() {
            @Override
            public void run() {
                _SequenceNumberGenerator seqGen = app.config().sequenceNumberGenerator();
                seqGen.configure(app.config(), DbServiceManager.this);
                SequenceNumberGenerator.registerImpl(seqGen);
            }
        });
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(serviceMap.values(), ApplicationScoped.class);
        serviceMap.clear();
        Destroyable.Util.tryDestroyAll(modelDaoMap.values(), ApplicationScoped.class);
        modelDaoMap.clear();
    }

    @Override
    public Dao dao(Class<?> modelClass) {
        Dao dao = modelDaoMap.get(modelClass);
        if (null == dao) {
            String svcId = DEFAULT;
            Annotation[] aa = modelClass.getDeclaredAnnotations();
            for (Annotation a : aa) {
                if (a instanceof DB) {
                    svcId = ((DB)a).value();
                }
            }
            DbService dbService = dbService(svcId);
            dao = dbService.defaultDao(modelClass);
            modelDaoMap.put(modelClass, dao);
        }
        return dao;
    }

    public <T extends DbService> T dbService(String id) {
        return (T)serviceMap.get(id);
    }

    public Iterable<DbService> registeredServices() {
        return serviceMap.values();
    }

    private void initServices(AppConfig config) {
        DbManager dbManager = Act.dbManager();
        if (!dbManager.hasPlugin()) {
            logger.warn("DB service not initialized: No DB plugin found");
            return;
        }
        DbPlugin db = dbManager.theSolePlugin();
        Map<String, String> dbConf = config.subSet("db.");
        if (dbConf.isEmpty()) {
            if (null == db) {
                logger.warn("DB service not intialized: need to specify default db service implementation");
                return;
            } else {
                logger.warn("DB configuration not found. Will try to init default service with the sole db plugin: %s", db);
                DbService svc = db.initDbService(DEFAULT, app(), new HashMap<String, String>());
                serviceMap.put(DEFAULT, svc);
                return;
            }
        }

        String firstInstance = null;
        if (dbConf.containsKey("db.instances")) {
            String instances = dbConf.get("db.instances").toString();
            String[] sa = instances.split("[,\\s;:]+");
            for (String dbId: sa) {
                initService(dbId, dbConf);
            }
            firstInstance = sa[0];
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
            logger.warn("db service configuration not found. Use the sole one db service[%s] as default service", svc.id());
        } else {
            if (serviceMap.isEmpty()) {
                if (null == db) {
                    logger.warn("DB service not initialized: need to specify default db service implementation");
                } else {
                    logger.warn("DB configuration not found. Will try to init default service with the sole db plugin: %s", db);
                    Map<String, String> svcConf = C.newMap();
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
                logger.warn("Default service not specified. Use the first db instance as default service: %s", firstInstance);
                serviceMap.put(DEFAULT, serviceMap.get(firstInstance));
            }
        }
    }

    private void initService(String dbId, Map<String, String> conf) {
        Map<String, String> svcConf = C.newMap();
        String prefix = "db." + (S.empty(dbId) ? "" : dbId + ".");
        for (String key : conf.keySet()) {
            if (key.startsWith(prefix)) {
                String o = conf.get(key);
                svcConf.put(key.substring(prefix.length()), o);
            }
        }
        Object impl = svcConf.remove("impl");
        String svcId = S.empty(dbId) ? DEFAULT : dbId;
        if (null == impl) {
            throw new ConfigurationException("Cannot init db service[%s]: implementation not specified", svcId);
        }
        DbPlugin plugin = Act.dbManager().plugin(impl.toString());
        if (null == plugin) {
            throw new ConfigurationException("Cannot init db service[%s]: implementation not found", svcId);
        }
        DbService svc = plugin.initDbService(dbId, app(), svcConf);
        serviceMap.put(svcId, svc);
        logger.info("db service[%s] initialized", svcId);
    }
}
