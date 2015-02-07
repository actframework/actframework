package org.osgl.oms.conf;

import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.OMS;
import org.osgl.util.IO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loading configurations from file
 */
public abstract class ConfLoader<T extends Config> {

    private static Logger logger = L.get(OMS.class);

    public T load(File confFile) {
        // load conf from disk
        Map<String, ?> rawConf = _loadConfFromDisk(confFile);
        rawConf = _processConf(rawConf);

        // load conf from System.properties
        Properties sysProps = System.getProperties();
        rawConf.putAll((Map) sysProps);

        // initialize the configuration with all loaded data
        return create(rawConf);
    }

    protected abstract T create(Map<String, ?> rawConf);
    protected abstract String confFileName();

    private Map _loadConfFromDisk(File conf) {
        InputStream is = null;
        boolean emptyConf = false;
        if (null == conf) {
            ClassLoader cl = OMS.class.getClassLoader();
            is = cl.getResourceAsStream(confFileName());
        } else {
            try {
                is = new FileInputStream(conf);
            } catch (IOException e) {
                if (!emptyConf) {
                    logger.warn(e, "Error opening conf file:" + conf);
                }
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

    // trim "oms." from conf keys
    private static Map<String, Object> _processConf(Map<String, ?> conf) {
        Map<String, Object> m = new HashMap<String, Object>(conf.size());
        for (String s : conf.keySet()) {
            Object o = conf.get(s);
            if (s.startsWith("oms.")) s = s.replaceFirst("oms\\.", "");
            m.put(s, o);
        }
        return m;
    }
}
