package org.osgl.oms.conf;

import org.osgl.util.E;

import java.io.File;
import java.net.URI;
import java.util.Map;

import static org.osgl.oms.conf.OmsConfigKey.APP_BASE;
import static org.osgl.oms.conf.OmsConfigKey.HOME;

public class OmsConfig extends Config<OmsConfigKey> {

    public static final String CONF_FILE_NAME = "oms.conf";

    /**
     * Construct a <code>AppConfig</code> with a map. The map is copied to
     * the original map of the configuration instance
     *
     * @param configuration
     */
    public OmsConfig(Map<String, ?> configuration) {
        super(configuration);
    }

    public OmsConfig() {
        this((Map)System.getProperties());
    }

    @Override
    protected ConfigKey keyOf(String s) {
        return OmsConfigKey.valueOfIgnoreCase(s);
    }

    private File home = null;
    public File home() {
        if (null == home) {
            URI uri = get(HOME);
            if (null == uri) {
                E.invalidConfiguration("valid oms.home.dir expected");
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
