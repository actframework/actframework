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

import static act.conf.ActConfigKey.*;

import org.osgl.util.E;

import java.io.File;
import java.net.URI;
import java.util.Map;

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
            URI uri = get(HOME, null);
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
            String s = get(APP_BASE, null);
            appBase = new File(home(), s);
            validateDir(appBase, APP_BASE.key());
        }
        return appBase;
    }

    private Boolean hotReload;

    public void enableDisableHotReload(boolean on) {
        hotReload = on;
    }

    public boolean hotReloadDisabled() {
        if (null == hotReload) {
            hotReload = get(HOT_RELOAD, true);
        }
        return !hotReload;
    }

    private Integer xioMaxWorkerThreads;
    public int xioMaxWorkerThreads() {
        if (null == xioMaxWorkerThreads) {
            xioMaxWorkerThreads = get(XIO_MAX_WORKER_THREADS, 0);
        }
        return xioMaxWorkerThreads;
    }


    private Boolean xioStatistics;
    public boolean xioStatistics() {
        if (null == xioStatistics) {
            xioStatistics = get(XIO_STATISTICS, false);
        }
        return xioStatistics;
    }

    private static void validateDir(File dir, String conf) {
        if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
            E.invalidConfiguration("%s is not a valid directory: %s", conf, dir.getAbsolutePath());
        }
    }
}
