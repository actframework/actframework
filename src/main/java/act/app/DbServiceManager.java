package act.app;

import act.Act;
import act.conf.AppConfig;
import act.db.DbPlugin;
import act.db.DbService;
import org.osgl.exception.ConfigurationException;
import org.osgl.util.C;
import org.rythmengine.utils.S;

import java.util.Map;

public class DbServiceManager extends AppServiceBase<DbServiceManager> {

    private Map<String, DbService> serviceMap = C.newMap();

    protected DbServiceManager(App app) {
        super(app);
        initServices(app.config());
    }

    @Override
    protected void releaseResources() {
        serviceMap.clear();
    }

    private void initServices(AppConfig config) {
        Map<String, Object> dbConf = config.subSet("db.");
        if (dbConf.isEmpty()) {
            logger.warn("DB configuration not found. No DbService will be initialized");
            return;
        }
        if (dbConf.containsKey("db.instances")) {
            String instances = dbConf.get("db.instances").toString();
            for (String dbId: instances.split("[,\\s;:]+")) {
                initService(dbId, dbConf);
            }
        }
        if (serviceMap.containsKey("default")) return;
        // try init default service if conf found
        String dbId = null;
        if (dbConf.containsKey("db.default.impl")) {
            dbId = "default";
        } else if (dbConf.containsKey("db.impl")) {
            dbId = "";
        }
        if (null != dbId) {
            initService(dbId, dbConf);
        } else if (serviceMap.size() == 1) {
            serviceMap.put("default", serviceMap.values().iterator().next());
        } else {
            logger.warn("default db service not initialized because no configuration found");
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
        String svcId = S.empty(dbId) ? "default" : dbId;
        if (null == impl) {
            throw new ConfigurationException("Cannot init db service[%s]: implementation not specified", svcId);
        }
        DbPlugin plugin = Act.dbManager().plugin(impl.toString());
        if (null == plugin) {
            throw new ConfigurationException("Cannot init db service[%s]: implementation not found", svcId);
        }
        DbService svc = plugin.initDbService(app(), svcConf);
        serviceMap.put(svcId, svc);
        logger.info("db service[%s] initialized", svcId);
    }
}
