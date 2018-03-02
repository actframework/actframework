package playground;

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

import act.app.BuildFileProbe;
import act.app.ProjectLayout;
import act.plugin.Plugin;

import java.io.File;
import java.util.List;
import java.util.Map;

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
            public String classes() {
                return null;
            }

            @Override
            public File target(File appBase) {
                return null;
            }

            @Override
            public Map<String, List<File>> routeTables(File appBase) {
                return null;
            }

            @Override
            public File conf(File appBase) {
                return null;
            }
        };
    }
}
