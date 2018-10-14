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

import static act.app.ProjectLayout.Utils.file;
import static act.route.RouteTableRouterBuilder.ROUTES_FILE;

import act.Act;
import act.app.util.NamedPort;
import org.osgl.util.S;

import java.io.File;
import java.util.*;

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

    public static File resource(App app) {
        //return Act.isDev() ? app.layout().resource(app.base()) : classes(app);
        return app.layout().resource(Act.isDev() ? app.base() : app.home());
    }

    public static File conf(App app) {
        File confBase = app.layout().resource(Act.isDev() ? app.base() : app.home()); //Act.isDev() ? app.layout().resource(app.base()) : classes(app);
        File file = new File(confBase, CONF);
        return file.exists() ? file : confBase;
    }

    public static Map<String, List<File>> routes(App app) {
        Map<String, List<File>> map = new HashMap();
        File base = resource(app);
        map.put(NamedPort.DEFAULT, routes(base, ROUTES_FILE));
        for (NamedPort np : app.config().namedPorts()) {
            String npName = np.name();
            String routesConfName = S.concat("routes.", npName, ".conf");
            map.put(npName, routes(base, routesConfName));
        }
        return map;
    }

    private static List<File> routes(File base, String name) {
        List<File> routes = new ArrayList<>();
        routes.add(file(base, name));
        File confRoot = file(base, CONF);
        routes.add(file(confRoot, name));
        File profileRooot = file(confRoot, Act.profile());
        routes.add(file(profileRooot, name));
        return routes;
    }

    public static File classes(App app) {
        File file = new File(app.home(), app.layout().classes());
        if (!file.exists()) {
            // suppose we starts PROD mode from IDE
            file = new File(app.layout().target(app.home()), app.layout().classes());
        }
        return file;
    }

    public static File lib(App app) {
        return new File(app.home(), LIB);
    }

}
