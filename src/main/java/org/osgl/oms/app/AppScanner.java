package org.osgl.oms.app;

import org.osgl._;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.OMS;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.Iterators;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Scan file system and construct {@link App} instances
 */
public class AppScanner {

    private static Logger logger = L.get(AppScanner.class);

    public static AppScanner DEF_SCANNER = new AppScanner();
    public static AppScanner DEV_MODE_SCANNER = new DevModeAppScanner();

    private final Map<String, ProjectLayoutProbe> buildFileProbeMap = C.newMap();
    private final List<ProjectLayoutProbe> projectLayoutProbes = C.newList();

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

    void scan(_.Func1<App, ?> callback) {
        File[] appBases = appBases();
        int size = appBases.length;
        for (int i = 0; i < size; ++i) {
            File appBase = appBases[i];
            scan(appBase, callback);
        }
    }

    protected File[] appBases() {
        File appBase = OMS.conf().appBase();
        return appBase.listFiles();
    }

    private void scan(File appBase, _.Func1<App, ?> callback) {
        ProjectLayout layout = probe(appBase);
        if (null != layout) {
            createApp(appBase, layout, callback);
        }
        ProjectLayout[] predefined = ProjectLayout.PredefinedLayout.values();
        for (int i = predefined.length - 1; i >= 0; --i) {
            layout = predefined[i];
            if (ProjectLayout.util.probeAppBase(appBase, layout)) {
                createApp(appBase, layout, callback);
            }
        }
        logger.warn("%s is not a valid app base", appBase.getPath());
    }

    private ProjectLayout probe(File appBase) {
        ProjectLayout layout = probeLayoutPropertiesFile(appBase);
        if (null != layout) {
            return layout;
        }
        Iterator<ProjectLayoutProbe> probes = probes();
        while (probes.hasNext()) {
            ProjectLayoutProbe probe = probes.next();
            layout = probe.probe(appBase);
            if (null != layout) {
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

    private void createApp(File appBase, ProjectLayout layout, _.Func1<App, ?> callback) {
        App app = App.create(appBase, layout);
        callback.apply(app);
    }

    private Iterator<ProjectLayoutProbe> probes() {
        return Iterators.composite(buildFileProbeMap.values().iterator(), projectLayoutProbes.iterator());
    }
}
