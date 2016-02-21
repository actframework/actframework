package act.app;

import act.Act;
import act.ActComponent;
import act.Destroyable;
import act.app.event.AppEventId;
import act.conf.AppConfig;
import act.db.*;
import act.event.AppEventListenerBase;
import act.util.ClassNode;
import act.util.General;
import org.osgl.$;
import org.osgl.exception.ConfigurationException;
import org.osgl.util.C;
import org.osgl.util.E;
import org.rythmengine.utils.S;

import java.lang.annotation.Annotation;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

@ActComponent
public class DbServiceManager extends AppServiceBase<DbServiceManager> implements DaoLocator {

    public static final String DEFAULT = "default";

    // map service id to service instance
    private Map<String, DbService> serviceMap = C.newMap();

    // map model class to dao class
    private Map<Class<?>, Dao> modelDaoMap = C.newMap();

    protected DbServiceManager(final App app) {
        super(app, true);
        initServices(app.config());
        app.eventBus().bind(AppEventId.APP_CODE_SCANNED, new AppEventListenerBase() {
            @Override
            public void on(EventObject event) throws Exception {
                ClassNode node = app.classLoader().classInfoRepository().node(Dao.class.getName());
                node.findPublicNotAbstract(new $.Visitor<ClassNode>() {
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
                            Dao dao = $.cast(app.newInstance(daoType));
                            Class<?> modelType = dao.modelType();
                            DB db = modelType.getAnnotation(DB.class);
                            String svcId = null == db ? DEFAULT : db.value();
                            DbService dbService = dbService(svcId);
                            E.invalidConfigurationIf(null == dbService, "cannot find db service by id: %s", svcId);
                            dao = dbService.newDaoInstance(daoType);
                            app.registerSingleton(dao);
                            modelDaoMap.put(modelType, dao);
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(serviceMap.values());
        serviceMap.clear();
        Destroyable.Util.tryDestroyAll(modelDaoMap.values());
        modelDaoMap.clear();
    }

    @Override
    public Dao dao(Class<?> modelClass) {
        Dao dao = modelDaoMap.get(modelClass);
        if (null == dao) {
            String svcId = DEFAULT;
            DB db = modelClass.getDeclaredAnnotation(DB.class);
            if (null != db) {
                svcId = db.value();
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
        Map<String, Object> dbConf = config.subSet("db.");
        if (dbConf.isEmpty()) {
            if (null == db) {
                logger.warn("DB service not intialized: need to specify default db service implementation");
                return;
            } else {
                logger.warn("DB configuration not found. Will try to init default service with the sole db plugin: %s", db);
                DbService svc = db.initDbService(DEFAULT, app(), new HashMap<String, Object>());
                serviceMap.put(DEFAULT, svc);
                return;
            }
        }

        if (dbConf.containsKey("db.instances")) {
            String instances = dbConf.get("db.instances").toString();
            for (String dbId: instances.split("[,\\s;:]+")) {
                initService(dbId, dbConf);
            }
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
                    logger.warn("DB service not intialized: need to specify default db service implementation");
                } else {
                    logger.warn("DB configuration not found. Will try to init default service with the sole db plugin: %s", db);
                    Map<String, Object> svcConf = C.newMap();
                    String prefix = "db.";
                    for (String key : dbConf.keySet()) {
                        if (key.startsWith(prefix)) {
                            Object o = dbConf.get(key);
                            svcConf.put(key.substring(prefix.length()), o);
                        }
                    }
                    DbService svc = db.initDbService(DEFAULT, app(), svcConf);
                    serviceMap.put(DEFAULT, svc);
                }
            } else {
                throw E.invalidConfiguration("Default db service for the application needs to be specified");
            }
        }
    }

    private void initService(String dbId, Map<String, Object> conf) {
        Map<String, Object> svcConf = C.newMap();
        String prefix = "db." + (S.empty(dbId) ? "" : dbId + ".");
        for (String key : conf.keySet()) {
            if (key.startsWith(prefix)) {
                Object o = conf.get(key);
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
