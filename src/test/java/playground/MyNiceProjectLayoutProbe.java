package playground;

import org.osgl.oms.app.BuildFileProbe;
import org.osgl.oms.plugin.Extends;
import org.osgl.oms.plugin.Plugin;

/**
 * Created by luog on 7/02/2015.
 */
public class MyNiceProjectLayoutProbe extends MyProjectLayoutProbe implements Plugin {
    @Override
    public String buildFileName() {
        return "myniceproj.layout";
    }
}

