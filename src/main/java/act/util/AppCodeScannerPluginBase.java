package act.util;

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
import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.AppSourceCodeScanner;
import act.plugin.Plugin;

public abstract class AppCodeScannerPluginBase extends LogSupportedDestroyableBase implements Plugin {

    @Override
    public void register() {
        if (!load()) {
            Act.LOGGER.warn("Scanner plugin cannot be loaded: " + getClass().getName());
            return;
        }
        Act.scannerPluginManager().register(this);
        Act.LOGGER.debug("Plugin registered: %s", getClass().getName());
    }

    public abstract AppSourceCodeScanner createAppSourceCodeScanner(App app);

    public abstract AppByteCodeScanner createAppByteCodeScanner(App app);

    public abstract boolean load();
}
