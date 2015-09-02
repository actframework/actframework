package act.mail;

import act.ActComponent;
import act.Destroyable;
import act.app.App;
import act.app.AppServiceBase;
import act.conf.AppConfig;
import org.osgl.util.C;

import java.util.Map;

@ActComponent
public class MailerConfigManager extends AppServiceBase<MailerConfigManager> {

    public static final String KEY_MAILER = "mailer";

    private C.Map<String, MailerConfig> configMap = C.newMap();

    public MailerConfigManager(App app) {
        super(app);
        loadConfig(app.config());
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.destroyAll(configMap.values());
        configMap.clear();
    }

    public MailerConfig config(String id) {
        return configMap.get(id);
    }

    private void loadConfig(AppConfig config) {
        Object o = config.get(KEY_MAILER);
        if (null == o) {
            o = config.get("act." + KEY_MAILER);
        }
        Map map = config.rawConfiguration();
        if (null != o) {
            String s = o.toString();
            String[] sa = s.split("[\\s,;:]+");
            if (sa.length > 0) {
                for (String id: sa) {
                    configMap.put(id, new MailerConfig(id, map, app()));
                }
            }
        }
        // add default anyway
        configMap.put("default", new MailerConfig("default", map, app()));
    }
}
