package playground;

import org.osgl.oms.app.BuildFileProbe;
import org.osgl.oms.app.ProjectLayout;

import java.util.List;

public enum Layouts {
    ;

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

    public static class MyLinesParser extends BuildFileProbe.LinesParser {
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
