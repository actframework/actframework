package act.app;

import act.Act;
import act.ActComponent;
import act.plugin.Plugin;
import act.util.DescendantClassFilter;
import org.osgl._;

import java.io.File;

/**
 * Base class defines the application project layout probe contract
 * and utilities
 */
@ActComponent
public abstract class ProjectLayoutProbe implements Plugin {

    /**
     * Check if the given folder contains an application with certain layout
     *
     * @param appBase the folder supposed to be an application's base
     * @return a {@link ProjectLayout} of the app base or
     * {@code null} if can't figure out the project layout
     */
    public abstract ProjectLayout probe(File appBase);

    @Override
    public void register() {
        Act.mode().appScanner().register(this);
    }

    public static final DescendantClassFilter<ProjectLayoutProbe> PLUGIN_FILTER = new DescendantClassFilter<ProjectLayoutProbe>(true, true, ProjectLayoutProbe.class) {
        @Override
        public void found(Class<? extends ProjectLayoutProbe> clazz) {
            Act.mode().appScanner().register(_.newInstance(clazz));
        }
    };

}
