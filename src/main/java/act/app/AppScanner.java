package act.app;

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
import act.internal.util.AppDescriptor;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.Iterators;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Scan file system and construct {@link App} instances
 */
public class AppScanner {

    private static Logger logger = L.get(AppScanner.class);

    public static AppScanner DEF_SCANNER = new AppScanner();
    public static AppScanner SINGLE_APP_SCANNER = new SingleAppScanner();

    private final Map<String, ProjectLayoutProbe> buildFileProbeMap = new HashMap<>();
    private final List<ProjectLayoutProbe> projectLayoutProbes = new ArrayList<>();

    protected AppScanner() {
    }

    public AppScanner register(ProjectLayoutProbe probe) {
        if (probe instanceof BuildFileProbe) {
            buildFileProbeMap.put(((BuildFileProbe) probe).buildFileName(), probe);
        } else {
            projectLayoutProbes.add(probe);
        }
        return this;
    }

    public int test_probeCnt() {
        return buildFileProbeMap.size() + projectLayoutProbes.size();
    }

    void scan(AppDescriptor descriptor, $.Func1<App, ?> callback) {
        File[] appBases = appBases();
        int size = appBases.length;
        for (int i = 0; i < size; ++i) {
            File appBase = appBases[i];
            scan(descriptor, appBase, callback);
        }
    }

    protected File[] appBases() {
        File appBase = Act.conf().appBase();
        return appBase.listFiles();
    }

    private void scan(AppDescriptor descriptor, File appBase, $.Func1<App, ?> callback) {
        App app;
        ProjectLayout layout = probe(appBase);
        if (null == layout && !Act.isDev()) {
            layout = ProjectLayout.PredefinedLayout.PKG;
        }
        if (null != layout) {
            app = App.create(appBase, descriptor.getVersion(), layout);
            if (null != descriptor.getAppName()) {
                app.name(descriptor.getAppName());
            }
            callback.apply(app);
        } else {
            logger.warn("%s is not a valid app base", appBase.getPath());
        }
    }

    private ProjectLayout probe(File appBase) {
        ProjectLayout layout = probeLayoutPropertiesFile(appBase);
        if (null != layout) {
            return layout;
        }
        layout = probeLayoutPlugin(appBase);
        if (null != layout) {
            return layout;
        }
        return probeDefaultLayouts(appBase);
    }

    private ProjectLayout probeLayoutPlugin(File appBase) {
        Iterator<ProjectLayoutProbe> probes = pluginProbes();
        while (probes.hasNext()) {
            ProjectLayoutProbe probe = probes.next();
            ProjectLayout layout = probe.probe(appBase);
            if (null != layout) {
                return layout;
            }
        }
        return null;
    }

    private ProjectLayout probeDefaultLayouts(File appBase) {
        ProjectLayout[] predefined = ProjectLayout.PredefinedLayout.values();
        for (int i = predefined.length - 1; i >= 0; --i) {
            ProjectLayout layout = predefined[i];
            if (ProjectLayout.util.probeAppBase(appBase, layout)) {
                return layout;
            }
        }
        return null;
    }

    private ProjectLayout probeLayoutPropertiesFile(File appBase) {
        File f = new File(appBase, ProjectLayout.PROJ_LAYOUT_FILE);
        if (f.exists() && f.canRead()) {
            Properties p = new Properties();
            InputStream is = IO.is(f);
            try {
                p.load(is);
            } catch (IOException e) {
                throw E.ioException(e);
            } finally {
                IO.close(is);
            }
            return ProjectLayout.util.build(p);
        } else {
            return null;
        }
    }

    private Iterator<ProjectLayoutProbe> pluginProbes() {
        return Iterators.composite(buildFileProbeMap.values().iterator(), projectLayoutProbes.iterator());
    }
}
