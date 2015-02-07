package playground;

import org.osgl.oms.app.BuildFileProbe;
import org.osgl.oms.app.ProjectLayout;
import org.osgl.oms.plugin.Extends;
import org.osgl.oms.plugin.Plugin;

import java.util.List;

/**
 * Created by luog on 7/02/2015.
 */
public enum Layouts {
    ;
    @Extends(Plugin.class)
    public static class MyStringParser extends BuildFileProbe.StringParser {

        @Override
        protected ProjectLayout parse(String fileContent) {
            return null;
        }

        @Override
        public String buildFileName() {
            return "mystring.layout";
        }
    }

    public static class MyLinesParser extends BuildFileProbe.LinesParser implements Plugin {
        @Override
        protected ProjectLayout parse(List<String> lines) {
            return null;
        }

        @Override
        public String buildFileName() {
            return "mylines.layout";
        }
    }
}
