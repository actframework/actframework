package playground;

import org.osgl.oms.app.BuildFileProbe;
import org.osgl.oms.app.ProjectLayout;
import org.osgl.oms.plugin.Extends;
import org.osgl.oms.plugin.Plugin;

import java.io.File;

/**
 * Created by luog on 7/02/2015.
 */
public class MyProjectLayoutProbe extends BuildFileProbe.FileParser implements Plugin {

    @Override
    public String buildFileName() {
        return "myproj.layout";
    }

    @Override
    protected ProjectLayout parse(File file) {
        return new ProjectLayout() {
            @Override
            public File source(File appBase) {
                return null;
            }

            @Override
            public File resource(File appBase) {
                return null;
            }

            @Override
            public File lib(File appBase) {
                return null;
            }

            @Override
            public File asset(File appBase) {
                return null;
            }

            @Override
            public File target(File appBase) {
                return null;
            }

            @Override
            public File routeTable(File appBase) {
                return null;
            }

            @Override
            public File conf(File appBase) {
                return null;
            }
        };
    }
}
