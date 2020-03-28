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

import static act.conf.ConfigKey.KEY_COMMON_CONF_TAG;

import act.Act;
import act.app.ProjectLayout;
import act.app.RuntimeDirs;
import act.util.LogSupport;
import act.util.SysProps;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loading configurations from conf file or conf dir
 */
public abstract class ConfLoader<T extends Config> extends LogSupport {

    public T load() {
        Map<String, ?> rawConf = new HashMap<>();
        Properties sysProps = System.getProperties();
        rawConf.putAll((Map) sysProps);

        rawConf = processConf(rawConf);
        processScanPackage(rawConf);
        return create(rawConf);
    }

    public T load(File resourceRoot) {
        // load conf from disk
        Map<String, Object> rawConf = null == resourceRoot ? new HashMap<>() : loadConfFromDisk(resourceRoot);

        // load conf from System.properties
        Properties sysProps = System.getProperties();
        rawConf.putAll((Map) sysProps);

        // load conf from Environment variable
        Map<String, String> envMap = System.getenv();
        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("act_env_")) {
                key = Config.canonical(key.substring(8));
                rawConf.put(key, entry.getValue());
            }
        }

        // strip off "act." prefix if has any
        // canonical all keys
        rawConf = processConf(rawConf);
        processScanPackage(rawConf);

        // initialize the configuration with all loaded data
        return create(rawConf);
    }

    /**
     * Return the "common" configuration set name. By default it is "common"
     * @return the "common" conf set name
     */
    public static String common() {
        String common = SysProps.get(KEY_COMMON_CONF_TAG);
        if (S.blank(common)) {
            common = "common";
        }
        return common;
    }

    /**
     * Return the name of the current conf set
     * @return the conf set name
     */
    public static String confSetName() {
        String profile = SysProps.get(AppConfigKey.PROFILE.key());
        if (S.blank(profile)) {
            profile = Act.mode().name().toLowerCase();
            System.setProperty(AppConfigKey.PROFILE.key(), profile);
        }
        return profile;
    }

    protected abstract T create(Map<String, ?> rawConf);

    protected abstract String confFileName();

    private Map loadConfFromDisk(File conf)  {
        if (conf.isDirectory()) {
            return loadConfFromDir(conf);
        } else if (conf.getName().endsWith(".jar")) {
            return loadConfFromJar(conf);
        } else {
            return loadConfFromFile(conf);
        }
    }

    private Map loadConfFromJar(File jarFile) {
        boolean traceEnabled = isTraceEnabled();
        if (traceEnabled) {
            trace("loading app conf from jar file: %s", jarFile);
        }
        TreeMap<String, JarEntry> map = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String path1, String path2) {
                boolean path1IsRoot = path1.contains("/");
                boolean path2IsRoot = path2.contains("/");
                if (path1IsRoot && path2IsRoot) {
                    return path1.compareTo(path2);
                }
                if (path1IsRoot) {
                    return -1;
                } else if (path2IsRoot) {
                    return 1;
                }
                if (path1.startsWith("conf/")) {
                    path1 = path1.substring(5);
                    path2 = path2.substring(5);
                    return compare(path1, path2);
                }
                boolean path1IsCommon = path1.startsWith("common/");
                boolean path2IsCommon = path2.startsWith("common/");
                if (path1IsCommon && path2IsCommon) {
                    return path1.compareTo(path2);
                }
                if (path1IsCommon) {
                    return -1;
                } else if (path2IsCommon) {
                    return 1;
                }
                return path1.compareTo(path2);
            }
        });
        try (JarFile jar = new JarFile(jarFile)) {
            String profile = Act.profile();
            for (JarEntry entry : C.enumerable(jar.entries())) {
                String name = entry.getName();
                if (isAppProperties(name, profile)) {
                    if (traceEnabled) {
                        trace("found jar entry for app properties: %s", name);
                    }
                    map.put(name, entry);
                }
            }
            Map conf = new HashMap();
            for (Map.Entry<String, JarEntry> entry : map.entrySet()) {
                Properties p = new Properties();
                try {
                    if (traceEnabled) {
                        trace("loading app properties from jar entry: %s", entry.getKey());
                    }
                    p.load(jar.getInputStream(entry.getValue()));
                    for (Map.Entry<Object, Object> pEntry: p.entrySet()) {
                        conf.put(Config.canonical(S.string(entry.getKey())), entry.getValue());
                    }
                } catch (IOException e) {
                    logger.warn("Error loading %s from jar file: %s", entry.getKey(), jarFile);
                }
            }
            return conf;
        } catch (IOException e) {
            warn(e, "error opening jar file: %s", jarFile);
        }
        return new HashMap<>();
    }

    private boolean isAppProperties(String name, String profile) {
        if (!name.endsWith(".properties")) {
            return false;
        }
        if (name.startsWith("conf/")) {
            String name0 = name.substring(5);
            if (!name0.contains("/") || name0.startsWith("common")) {
                return true;
            }
            return !S.blank(profile) && name0.startsWith(profile + "/");
        }
        return !name.contains("/") && !name.startsWith("act") && !name.startsWith("build.");
    }

    private Map loadConfFromFile(File conf) {
        if (isTraceEnabled()) {
            trace("loading app conf from file: %s", conf);
        }
        if (!conf.canRead()) {
            logger.warn("Cannot read conf file[%s]", conf.getAbsolutePath());
            return new HashMap<>();
        }
        InputStream is = IO.inputStream(conf);
        Properties p = new Properties();
        try {
            p.load(is);
            return p;
        } catch (Exception e) {
            logger.warn(e, "Error loading %s", confFileName());
        } finally {
            IO.close(is);
        }
        return new HashMap<>();
    }

    // trim "act." from conf keys
    private static Map<String, Object> processConf(Map<String, ?> conf) {
        Map<String, Object> m = new HashMap<String, Object>(conf.size());
        for (String s : conf.keySet()) {
            Object o = conf.get(s);
            if (s.startsWith("act.")) s = s.substring(4);
            m.put(s, o);
            m.put(Config.canonical(s), o);
        }
        return m;
    }
    private void processScanPackage(Map rawConfig) {
        String scanPackage = (String) rawConfig.get(AppConfigKey.SCAN_PACKAGE.key());
        if (null == scanPackage) {
            Object v = rawConfig.get(AppConfigKey.SCAN_PACKAGE_SYS.key());
            rawConfig.put(AppConfigKey.SCAN_PACKAGE.key(), v);
        }
    }
    private Map loadConfFromDir(File resourceDir) {
        if (!resourceDir.exists()) {
            logger.warn("Cannot read conf dir[%s]", resourceDir.getAbsolutePath());
            return new HashMap<>();
        }

        Map map = new HashMap<>();

        /*
         * Load from resources root
         */
        map.putAll(loadConfFromDir_(resourceDir));

        /*
         * Load from resources/conf root
         */
        File confDir = ProjectLayout.Utils.file(resourceDir, RuntimeDirs.CONF);
        map.putAll(loadConfFromDir_(confDir));

        /*
         * try load from resources/conf/common conf
         */
        String common = common();
        File commonConfDir = new File(confDir, common);
        if (commonConfDir.isDirectory()) {
            map.putAll(loadConfFromDir_(commonConfDir));
        }

        /*
         * try to load conf from profile conf dir, e.g. ${conf_root}/uat or
         * ${conf_root}/dev etc
         */
        String profile = confSetName();
        logger.debug("Loading conf profile: %s", profile);
        File taggedConfDir = new File(confDir, profile);
        if (taggedConfDir.exists() && taggedConfDir.isDirectory()) {
            map.putAll(loadConfFromDir_(taggedConfDir));
        }

        return map;
    }

    private Map loadConfFromDir_(File confDir) {
        File[] confFiles = confDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".properties") || name.endsWith(".conf");
            }
        });
        if (null == confFiles) {
            return C.Map();
        } else {
            Map map = new HashMap<>();
            int n = confFiles.length;
            for (int i = 0; i < n; ++i) {
                map.putAll(loadConfFromFile(confFiles[i]));
            }
            return map;
        }
    }
}
