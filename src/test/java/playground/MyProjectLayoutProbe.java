package playground;

import act.app.BuildFileProbe;
import act.app.ProjectLayout;
import act.plugin.Plugin;

import java.io.File;

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
            public File testSource(File appBase) {
                return null;
            }

            @Override
            public File testResource(File appBase) {
                return null;
            }

            @Override
            public File testLib(File appBase) {
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
