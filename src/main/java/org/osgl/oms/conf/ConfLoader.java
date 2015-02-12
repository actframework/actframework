package org.osgl.oms.conf;

import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.OMS;
import org.osgl.oms.util.SysProps;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.osgl.oms.conf.ConfigKey.KEY_COMMON_CONF_TAG;
import static org.osgl.oms.conf.ConfigKey.KEY_CONF_TAG;

/**
 * Loading configurations from conf file or conf dir
 */
public abstract class ConfLoader<T extends Config> {

    private static Logger logger = L.get(OMS.class);

    // trim "oms." from conf keys
    private static Map<String, Object> processConf(Map<String, ?> conf) {
        Map<String, Object> m = new HashMap<String, Object>(conf.size());
        for (String s : conf.keySet()) {
            Object o = conf.get(s);
            if (s.startsWith("oms.")) s = s.replaceFirst("oms\\.", "");
            m.put(s, o);
        }
        return m;
    }

    public T load(File confFile) {
        // load conf from disk
        Map<String, ?> rawConf = loadConfFromDisk(confFile);

        // load conf from System.properties
        Properties sysProps = System.getProperties();
        rawConf.putAll((Map) sysProps);

        // strip off "oms." prefix if has any
        rawConf = processConf(rawConf);

        // initialize the configuration with all loaded data
        return create(rawConf);
    }

    protected abstract T create(Map<String, ?> rawConf);

    protected abstract String confFileName();

    private Map loadConfFromDisk(File conf) {
        if (conf.isDirectory()) {
            return loadConfFromDir(conf);
        } else {
            return loadConfFromFile(conf);
        }
    }

    private Map loadConfFromFile(File conf) {
        InputStream is = null;
        if (null == conf) {
            ClassLoader cl = OMS.class.getClassLoader();
            is = cl.getResourceAsStream("/" + confFileName());
        } else {
            try {
                is = new FileInputStream(conf);
            } catch (IOException e) {
                logger.warn(e, "Error opening conf file:" + conf);
            }
        }
        if (null != is) {
            Properties p = new Properties();
            try {
                p.load(is);
                return p;
            } catch (Exception e) {
                logger.warn(e, "Error loading %s", confFileName());
            } finally {
                IO.close(is);
            }
        }
        return new HashMap();
    }

    private Map loadConfFromDir(File confDir) {
        /*
         * try to load conf from tagged conf dir, e.g. ${conf_root}/uat or
         * ${conf_root}/dev etc
         */
        String confTag = SysProps.get(KEY_CONF_TAG);
        if (S.blank(confTag)) {
            confTag = OMS.mode().name();
        }
        Map map = C.newMap();
        File taggedConfDir = new File(confDir, confTag);
        if (taggedConfDir.exists() && taggedConfDir.isDirectory()) {
            // try load properties from common tag first
            String common = SysProps.get(KEY_COMMON_CONF_TAG);
            if (S.blank(common)) {
                common = "common";
            }
            File commonConfDir = new File(confDir, common);
            if (commonConfDir.isDirectory()) {
                map.putAll(loadConfFromDir_(commonConfDir));
            }
            map.putAll(loadConfFromDir_(taggedConfDir));
            return map;
        } else {
            return loadConfFromDir_(confDir);
        }
    }

    private Map loadConfFromDir_(File confDir) {
        File[] confFiles = confDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".properties") || name.endsWith(".conf");
            }
        });
        if (null == confFiles) {
            return C.map();
        } else {
            Map map = C.newMap();
            int n = confFiles.length;
            for (int i = 0; i < n; ++i) {
                map.putAll(loadConfFromFile(confFiles[i]));
            }
            return map;
        }
    }
}
