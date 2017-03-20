package act.conf;

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
import act.cli.Command;
import act.cli.Optional;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.inject.Inject;
import java.util.List;

public class ConfAdmin {

    @Inject
    private AppConfig appConfig;

    @Command(name = "act.conf.list", help = "list configuration")
    public List<ConfigItem> list(
            @Optional("list system configuration") boolean system,
            @Optional(lead = "-q", help = "specify search text") String q
    ) {
        ConfigKey[] keys = system ? ActConfigKey.values() : AppConfigKey.values();
        List<ConfigItem> list = C.newSizedList(keys.length);

        Config<?> config = system ? Act.conf() : appConfig;
        boolean hasQuery = S.notBlank(q);
        for (ConfigKey key: keys) {
            String keyString = key.toString();
            if (hasQuery && !keyString.contains(q)) {
                continue;
            }
            list.add(new ConfigItem(key.toString(), config));
        }
        return list;
    }

}
