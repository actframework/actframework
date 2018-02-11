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
import act.util.DescendantClassFilter;
import org.osgl.$;

import java.io.File;

/**
 * Base class defines the application project layout probe contract
 * and utilities
 */
public abstract class ProjectLayoutProbe {

    /**
     * Check if the given folder contains an application with certain layout
     *
     * @param appBase the folder supposed to be an application's base
     * @return a {@link ProjectLayout} of the app base or
     * {@code null} if can't figure out the project layout
     */
    public abstract ProjectLayout probe(File appBase);

    public static final DescendantClassFilter<ProjectLayoutProbe> PLUGIN_FILTER = new DescendantClassFilter<ProjectLayoutProbe>(true, true, ProjectLayoutProbe.class) {
        @Override
        public void found(Class<? extends ProjectLayoutProbe> clazz) {
            Act.mode().appScanner().register($.newInstance(clazz));
        }
    };

}
