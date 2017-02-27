package playground;

import act.plugin.Plugin;

public class MyNiceProjectLayoutProbe extends MyProjectLayoutProbe implements Plugin {
    @Override
    public String buildFileName() {
        return "myniceproj.layout";
    }
}

