package act.conf;

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
