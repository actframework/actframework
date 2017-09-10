package act.boot.app;

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
import org.osgl.logging.Logger;
import org.osgl.util.E;

/**
 * The entry to start an Act full stack app
 */
public class RunApp {

    private static final Logger LOGGER = Act.LOGGER;

    /**
     * Start the application.
     *
     * ActFramework will scan the package defined by system property `act.scan_package`
     *
     * @throws Exception
     */
    public static void start() throws Exception {
        start(null, null, "");
    }

    public static void start(Class<?> anyController) throws Exception {
        start(null, null, anyController);
    }

    public static void start(String packageName) throws Exception {
        start(null, null, packageName);
    }

    public static void start(String appName, String appVersion, Class<?> anyController) throws Exception {
        String pkg = anyController.getPackage().getName();
        start(appName, appVersion, pkg);
    }

    public static void start(String appName, String appVersion, String packageName) throws Exception {
        E.tbd();
    }

    public static void main(String[] args) throws Exception {
        start();
    }
}
