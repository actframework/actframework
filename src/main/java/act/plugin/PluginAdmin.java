package act.plugin;

import act.cli.Command;
import act.cli.Optional;
import act.util.PropertySpec;
import org.osgl.util.C;

import java.util.List;

/**
 * Provides admin interface to access Plugin data info
 */
public class PluginAdmin {

    @Command(name = "act.plugin.list", help = "list plugins")
    @PropertySpec("this as Plugin")
    public List<String> list(
            @Optional("sort alphabetically") boolean sort
    ) {
        C.List<String> l =  C.list(Plugin.InfoRepo.plugins());
        return sort ? l.sorted() : l;
    }

}
