package act.app;

import act.Act;
import act.ActComponent;
import act.app.event.AppEventId;
import act.conf.AppConfig;
import act.db.Dao;
import act.db.DbManager;
import act.db.DbPlugin;
import act.db.DbService;
import act.event.AppEventListenerBase;
import act.util.ClassNode;
import org.osgl._;
import org.osgl.exception.ConfigurationException;
import org.osgl.util.C;
import org.osgl.util.E;
import org.rythmengine.utils.S;

import java.lang.reflect.Constructor;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

@ActComponent
public class DbServiceManager extends AppServiceBase<DbServiceManager> implements DaoLocator {

    public static final String DEFAULT = "default";

    // map service id to service instance
    private Map<String, DbService> serviceMap = C.newMap();

    // map model class to dao class
    private Map<Class<?>, Class<? extends Dao>> modelDaoMap = C.newMap();

    protected DbServiceManager(final App app) {
        super(app, true);
        initServices(app.config());
        app.eventBus().bind(AppEventId.APP_CODE_SCANNED, new AppEventListenerBase() {
            @Override
            public void on(EventObject event) throws Exception {
                ClassNode node = app.classLoader().classInfoRepository().node(Dao.class.getName());
                node.findPublicNotAbstract(new _.Visitor<ClassNode>() {
                    @Override
                    public void visit(ClassNode classNode) throws _.Break {
                        Class<? extends Dao> daoType = _.classForName(classNode.name(), app.classLoader());
                        try {
                            Dao dao = _.cast(app.newInstance(daoType));
                            Class<?> modelType = dao.modelType();
                            modelDaoMap.put(modelType, daoType);
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
        serviceMap.clear();
        modelDaoMap.clear();
    }

    @Override
    public Dao dao(Class<?> modelClass) {
        Class<? extends Dao> daoClass = modelDaoMap.get(modelClass);
        E.NPE(daoClass);
        return app().newInstance(daoClass);
    }

    public <T extends DbService> T dbService(String id) {
        return (T)serviceMap.get(id);
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
