package act.conf;

import org.osgl.util.E;

import java.io.File;
import java.net.URI;
import java.util.Map;

import static act.conf.ActConfigKey.APP_BASE;
import static act.conf.ActConfigKey.HOME;

public class ActConfig extends Config<ActConfigKey> {

    public static final String CONF_FILE_NAME = "act.conf";

    /**
     * Construct a <code>AppConfig</code> with a map. The map is copied to
     * the original map of the configuration instance
     *
     * @param configuration
     */
    public ActConfig(Map<String, ?> configuration) {
        super(configuration);
    }

    public ActConfig() {
        this((Map) System.getProperties());
    }

    @Override
    protected ConfigKey keyOf(String s) {
        return ActConfigKey.valueOfIgnoreCase(s);
    }

    private File home = null;

    public File home() {
        if (null == home) {
            URI uri = get(HOME);
            if (null == uri) {
                E.invalidConfiguration("valid act.home.dir expected");
            }
            home = new File(uri.getPath());
            validateDir(home, HOME.key());
        }
        return home;
    }

    private File appBase = null;

    public File appBase() {
        if (null == appBase) {
            String s = get(APP_BASE);
            appBase = new File(home(), s);
            validateDir(appBase, APP_BASE.key());
        }
        return appBase;
    }

    private static void validateDir(File dir, String conf) {
        if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
            E.invalidConfiguration("%s is not a valid directory: %s", conf, dir.getAbsolutePath());
        }
    }
}
