package act.mail;

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

import act.Destroyable;
import act.app.App;
import act.app.AppServiceBase;
import act.conf.AppConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class MailerConfigManager extends AppServiceBase<MailerConfigManager> {

    public static final String KEY_MAILER = "mailer";

    private Map<String, MailerConfig> configMap = new HashMap<>();

    @Inject
    public MailerConfigManager(App app) {
        super(app);
        loadConfig(app.config());
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.destroyAll(configMap.values(), ApplicationScoped.class);
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
