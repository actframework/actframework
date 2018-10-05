package act.plugin;

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
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;

import java.util.List;

/**
 * Responsible for scanning/loading Act server plugins.
 * <p>A server plugin shall be packaged in jar file and put into
 * <code>${ACT_HOME}/plugin folder</code></p>
 */
public class PluginScanner {

    private static final Logger logger = L.get(PluginScanner.class);

    public PluginScanner() {
    }

    public int scan() {
        Plugin.InfoRepo.clear();
        List<Class<?>> pluginClasses = Act.pluginClasses();
        int sz = pluginClasses.size();
        for (int i = 0; i < sz; ++i) {
            Class<?> c = pluginClasses.get(i);
            try {
                Plugin p = (Plugin) $.newInstance(c);
                Plugin.InfoRepo.register(p);
            } catch (UnexpectedException e) {
                // ignore: some plugin does not provide default constructor
                logger.warn(e, "failed to register plugin: %s", c);
            }
        }
        return sz;
    }

    public void unload() {
        Plugin.InfoRepo.clear();
    }

}
