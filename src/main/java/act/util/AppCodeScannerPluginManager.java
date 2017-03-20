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

import act.Destroyable;
import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.AppCodeScannerManager;
import act.app.AppSourceCodeScanner;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

public class AppCodeScannerPluginManager extends DestroyableBase {

    private static final Logger logger = L.get(AppCodeScannerPluginManager.class);

    private Map<Class<? extends AppCodeScannerPluginBase>, AppCodeScannerPluginBase> registry = C.newMap();

    public void register(AppCodeScannerPluginBase plugin) {
        Class<? extends AppCodeScannerPluginBase> clz = plugin.getClass();
        if (registry.containsKey(clz)) {
            logger.warn("%s has already been registered", clz);
            return;
        }
        registry.put(clz, plugin);
    }

    public void initApp(App app) {
        AppCodeScannerManager manager = app.scannerManager();
        for (AppCodeScannerPluginBase plugin : registry.values()) {
            AppSourceCodeScanner sourceCodeScanner = plugin.createAppSourceCodeScanner(app);
            if (null != sourceCodeScanner) {
                manager.register(sourceCodeScanner);
            }
            AppByteCodeScanner byteCodeScanner = plugin.createAppByteCodeScanner(app);
            if (null != byteCodeScanner) {
                manager.register(byteCodeScanner);
            }
        }
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(registry.values(), ApplicationScoped.class);
        registry.clear();
    }
}
