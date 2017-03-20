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
import act.plugin.Plugin;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
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
            extends $.F1<File, ProjectLayout>
            implements BuildFileNameProvider, Plugin {

        @Override
        public ProjectLayout apply(File file) throws NotAppliedException, $.Break {
            return parse(file);
        }

        @Override
        public void register() {
            Act.mode().appScanner().register(new BuildFileProbe(this));
        }

        protected abstract ProjectLayout parse(File file);
    }

    public static abstract class StringParser
            extends $.F1<String, ProjectLayout>
            implements BuildFileNameProvider, Plugin {


        @Override
        public ProjectLayout apply(String s) throws NotAppliedException, $.Break {
            return parse(s);
        }

        @Override
        public void register() {
            Act.mode().appScanner().register(new BuildFileProbe(this));
        }

        protected abstract ProjectLayout parse(String fileContent);
    }

    public static abstract class LinesParser
            extends $.F1<List<String>, ProjectLayout>
            implements BuildFileNameProvider, Plugin {

        @Override
        public ProjectLayout apply(List<String> lines) throws NotAppliedException, $.Break {
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
