package act.app.conf;

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

import act.app.App;
import act.app.event.SysEventId;
import act.util.SubClassFinder;

/**
 * {@code AppConfigPlugin} scan source code or byte code to detect if there are
 * any user defined {@link AppConfigurator} implementation and use it to populate
 * {@link act.conf.AppConfig} default values
 */
public class AppConfigPlugin  {

    @SubClassFinder(callOn = SysEventId.CLASS_LOADED)
    public static void foundConfigurator(final Class<? extends AppConfigurator> configuratorClass) {
        final App app = App.instance();
        AppConfigurator configurator = app.getInstance(configuratorClass);
        configurator.app(app);
        configurator.configure();
        app.config()._merge(configurator);
    }

}
