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
import act.route.RouteTableRouterBuilder;

import java.io.File;

/**
 * Define application dir structure at runtime
 */
public enum RuntimeDirs {
    ;

    public static final String CONF = "/conf";
    public static final String ASSET = "/asset";
    public static final String CLASSES = "/classes";
    public static final String LIB = "/lib";

    public static File home(App app) {
        if (Act.isDev()) {
            return app.layout().target(app.base());
        } else {
            return app.base();
        }
    }

    public static File conf(App app) {
        File confBase = Act.isDev() ? app.layout().resource(app.base()) : classes(app);
        File file = new File(confBase, CONF);
        return file.exists() ? file : confBase;
    }

    public static File routes(App app) {
        return new File(classes(app), RouteTableRouterBuilder.ROUTES_FILE);
    }

    public static File classes(App app) {
        return new File(app.home(), CLASSES);
    }

    public static File lib(App app) {
        return new File(app.home(), LIB);
    }

}
