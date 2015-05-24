package act.app;

import act.Act;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import act.plugin.Plugin;
import org.osgl.util.IO;

import java.io.File;
import java.util.List;

/**
 * Common implementation of {@link ProjectLayoutProbe}
 * by inspecting a certain application build file, e.g. pom.xml for maven
 * based application
 */
public class BuildFileProbe extends ProjectLayoutProbe {

    public interface BuildFileNameProvider {
        String buildFileName();
    }

    public static abstract class FileParser
            extends _.F1<File, ProjectLayout>
            implements BuildFileNameProvider, Plugin {

        @Override
        public ProjectLayout apply(File file) throws NotAppliedException, _.Break {
            return parse(file);
        }

        @Override
        public void register() {
            Act.mode().appScanner().register(new BuildFileProbe(this));
        }

        protected abstract ProjectLayout parse(File file);
    }

    public static abstract class StringParser
            extends _.F1<String, ProjectLayout>
            implements BuildFileNameProvider, Plugin {


        @Override
        public ProjectLayout apply(String s) throws NotAppliedException, _.Break {
            return parse(s);
        }

        @Override
        public void register() {
            Act.mode().appScanner().register(new BuildFileProbe(this));
        }

        protected abstract ProjectLayout parse(String fileContent);
    }

    public static abstract class LinesParser
            extends _.F1<List<String>, ProjectLayout>
            implements BuildFileNameProvider, Plugin {

        @Override
        public ProjectLayout apply(List<String> lines) throws NotAppliedException, _.Break {
            return parse(lines);
        }

        @Override
        public void register() {
            Act.mode().appScanner().register(new BuildFileProbe(this));
        }

        protected abstract ProjectLayout parse(List<String> lines);
    }

    private String buildFileName;
    private FileParser fileParser = null;
    private StringParser stringParser = null;
    private LinesParser linesParser = null;

    public BuildFileProbe(FileParser fp) {
        buildFileName = fp.buildFileName();
        fileParser = fp;
    }

    public BuildFileProbe(StringParser sp) {
        buildFileName = sp.buildFileName();
        stringParser = sp;
    }

    public BuildFileProbe(LinesParser lp) {
        buildFileName = lp.buildFileName();
        linesParser = lp;
    }

    public String buildFileName() {
        return buildFileName;
    }

    @Override
    public ProjectLayout probe(File appBase) {
        File buildFile = new File(appBase, buildFileName);
        if (buildFile.exists() && buildFile.canRead()) {
            if (null != fileParser) {
                return fileParser.parse(buildFile);
            } else if (null != stringParser) {
                return stringParser.parse(IO.readContentAsString(buildFile));
            } else if (null != linesParser) {
                return linesParser.parse(IO.readLines(buildFile));
            }
            assert false;
        }
        return null;
    }
}
