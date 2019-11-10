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
import static act.app.RuntimeDirs.CLASSES;
import static act.app.RuntimeDirs.CONF;
import static act.route.RouteTableRouterBuilder.ROUTES_FILE;
import static act.route.RouteTableRouterBuilder.ROUTES_FILE2;

import act.Act;
import act.app.util.NamedPort;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Defines the project file structure supported by Act.
 * <p>Used ONLY at dev time for Act to decide where to pick up
 * the application srccode files, configuration files etc</p>
 */
public interface ProjectLayout {

    /**
     * The className of the properties file sit directly in {@code appBase} dir that
     * specifies the project layout of the application.
     * <p>This properties file is only required if the app project uses a
     * customized project layout, i.e. not one of the
     * {@link ProjectLayout.PredefinedLayout predefined layouts}:
     * </p>
     * <ul>
     * <li>{@link ProjectLayout.PredefinedLayout#MAVEN maven} or</li>
     * <li>{@link ProjectLayout.PredefinedLayout#PLAY play}</li>
     * </ul>
     */
    String PROJ_LAYOUT_FILE = "proj.layout";

    enum PredefinedLayout implements ProjectLayout {
        /**
         * The standard maven style project layout looks like:
         * <pre>
         *     /&lt;app_root&gt;
         *     | --- src/
         *              | --- main/
         *                        | --- java/
         *                                  | --- ... # your java packages
         *                        | --- lib/
         *                                  | --- ... # your lib folder (contains jar files)
         *                        | --- resources/
         *                                  | --- app.conf # the application configuration file
         *                                  | --- routes # the routing table
         *                                  | --- ... # other resources
         *              | --- test/
         *                        | --- java/
         *                                  | --- ... your test packages
         *                        | --- resources/
         *                                  | --- ...
         *     | --- target/ # the build target folder
         * </pre>
         * <b>{@code MAVEN} layout is used only when Act running in {@link Act.Mode#DEV dev mode}</b>
         */
        MAVEN() {
            @Override
            public File source(File appBase) {
                return file(appBase, "src/main/java");
            }

            @Override
            public File testSource(File appBase) {
                return file(appBase, "src/test/java");
            }

            @Override
            public File resource(File appBase) {
                String resources = Act.isDev() ? "src/main/resources" : "classes";
                return file(appBase, resources);
            }

            @Override
            public File testResource(File appBase) {
                String resources = Act.isDev() ? "src/test/resources" : "test-classes";
                return file(appBase, resources);
            }

            @Override
            public File lib(File appBase) {
                String lib = Act.isDev() ? "src/main/lib" : "lib";
                return file(appBase, lib);
            }

            @Override
            public File testLib(File appBase) {
                String lib = Act.isDev() ? "src/test/lib" : "test-lib";
                return file(appBase, lib);
            }

            @Override
            public String classes() {
                return RuntimeDirs.CLASSES;
            }

            @Override
            public File target(File appBase) {
                return file(appBase, "target");
            }

        },

        GRADLE_JAVA() {
            @Override
            public File source(File appBase) {
                return file(appBase, "src/main/java");
            }

            @Override
            public File testSource(File appBase) {
                return file(appBase, "src/test/java");
            }

            @Override
            public File resource(File appBase) {
                String resources = Act.isDev() ? "src/main/resources" : "classes";
                return file(appBase, resources);
            }

            @Override
            public File testResource(File appBase) {
                String resources = Act.isDev() ? "src/test/resources" : "test-classes";
                return file(appBase, resources);
            }

            @Override
            public File lib(File appBase) {
                String lib = Act.isDev() ? "src/main/lib" : "lib";
                return file(appBase, lib);
            }

            @Override
            public File testLib(File appBase) {
                String lib = Act.isDev() ? "src/test/lib" : "test-lib";
                return file(appBase, lib);
            }

            @Override
            public String classes() {
                return RuntimeDirs.CLASSES;
            }

            @Override
            public File target(File appBase) {
                return file(appBase, "out/production");
            }

        },

        GRADLE_GROOVY() {
            @Override
            public File source(File appBase) {
                return file(appBase, "src/main/groovy");
            }

            @Override
            public File testSource(File appBase) {
                return file(appBase, "src/test/groovy");
            }

            @Override
            public File resource(File appBase) {
                if (Act.isDev()) {
                    return file(appBase, "src/main/resources");
                }
                return file(target(appBase), "resources/main");
            }

            @Override
            public File testResource(File appBase) {
                if (Act.isDev()) {
                    return file(appBase, "src/test/resources");
                }
                return file(target(appBase), "resources/test");
            }

            @Override
            public File lib(File appBase) {
                String lib = Act.isDev() ? "src/main/lib" : "libs";
                return file(appBase, lib);
            }

            @Override
            public File testLib(File appBase) {
                String lib = Act.isDev() ? "src/test/lib" : "test-libs";
                return file(appBase, lib);
            }

            @Override
            public String classes() {
                return "classes/groovy/main";
            }

            @Override
            public File target(File appBase) {
                return file(appBase, "build");
            }
        },

        GRADLE_GROOVY_IDEA() {
            @Override
            public File source(File appBase) {
                return file(appBase, "src/main/groovy");
            }

            @Override
            public File testSource(File appBase) {
                return file(appBase, "src/test/groovy");
            }

            @Override
            public File resource(File appBase) {
                if (Act.isDev()) {
                    return file(appBase, "src/main/resources");
                }
                return file(appBase, "out/production/resources");
            }

            @Override
            public File testResource(File appBase) {
                if (Act.isDev()) {
                    return file(appBase, "src/test/resources");
                }
                return file(appBase, "out/test/resources");
            }

            @Override
            public File lib(File appBase) {
                String lib = Act.isDev() ? "src/main/lib" : "libs";
                return file(appBase, lib);
            }

            @Override
            public File testLib(File appBase) {
                String lib = Act.isDev() ? "src/test/lib" : "test-libs";
                return file(appBase, lib);
            }

            @Override
            public String classes() {
                return "classes";
            }

            @Override
            public File target(File appBase) {
                return file(appBase, "out/production");
            }
        },

        PKG() {
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
                return file(appBase, "classes");
            }

            @Override
            public File lib(File appBase) {
                return file(appBase, "lib");
            }

            @Override
            public String classes() {
                return RuntimeDirs.CLASSES;
            }

            @Override
            public File target(File appBase) {
                return appBase;
            }
        },

        /**
         * The Playframework v1.x project layout, looks like:
         * <pre>
         *     /&lt;app_root&gt;
         *     | --- app/
         *              | --- # your java packages
         *     | --- lib/
         *              | --- # your lib
         *     | --- conf/
         *               | --- app.conf # the application configuration file
         *               | --- routes # the routing table
         *               | --- ... # other resources
         *     | --- test/
         *               | --- # your test packages
         *     | --- tmp/ # the build target folder
         * </pre>
         * <b>{@code PLAY} layout is used only when Act running in {@link Act.Mode#DEV dev mode}</b>
         */
        PLAY() {
            @Override
            public File source(File appBase) {
                return file(appBase, "app");
            }

            @Override
            public File testSource(File appBase) {
                return file(appBase, "test");
            }

            @Override
            public File resource(File appBase) {
                return file(appBase, "conf");
            }

            @Override
            public File testResource(File appBase) {
                return resource(appBase);
            }

            @Override
            public File lib(File appBase) {
                return file(appBase, "lib");
            }

            @Override
            public File testLib(File appBase) {
                return lib(appBase);
            }

            @Override
            public String classes() {
                return RuntimeDirs.CLASSES;
            }

            @Override
            public File target(File appBase) {
                return file(appBase, "tmp");
            }
        },
        ;

        @Override
        public File conf(File appBase) {
            File confBase = Act.isDev() ? resource(appBase) : new File(appBase, CLASSES);
            File file = new File(confBase, CONF);
            return file.exists() ? file : confBase;
        }

        @Override
        public Map<String, List<File>> routeTables(File appBase) {
            Map<String, List<File>> map = new HashMap<>();
            map.put(NamedPort.DEFAULT, routeTables(appBase, ROUTES_FILE, ROUTES_FILE2));
            for (NamedPort np : Act.app().config().namedPorts()) {
                String npName = np.name();
                String routesFile = S.concat("routes.", npName, ".conf");
                map.put(npName, routeTables(appBase, routesFile));
            }
            return map;
        }

        private List<File> routeTables(File appBase, String ... routesFiles) {
            List<File> files = new ArrayList<>();
            File resourceBase = resource(appBase);
            File routeFile = routeFile(resourceBase, routesFiles);
            if (null != routeFile) {
                files.add(routeFile);
            }
            File confBase = conf(appBase);
            routeFile = routeFile(confBase, routesFiles);
            if (null != routeFile && !files.contains(routeFile)) {
                files.add(routeFile);
            }
            File commonBase = file(confBase, "common");
            routeFile = routeFile(commonBase, routesFiles);
            if (null != routeFile && !files.contains(routeFile)) {
                files.add(routeFile);
            }
            File profileBase = file(confBase, Act.profile());
            routeFile = routeFile(profileBase, routesFiles);
            if (null != routeFile && !files.contains(routeFile)) {
                files.add(routeFile);
            }
            return files;
        }

        private static File routeFile(File base, String ... routesFiles) {
            for (String s : routesFiles) {
                File routeFile = file(base, s);
                if (routeFile.canRead()) {
                    return routeFile;
                }
            }
            return null;
        }

        public static ProjectLayout valueOfIgnoreCase(String s) {
            s = s.trim().toUpperCase();
            if (MAVEN.name().equals(s)) return MAVEN;
            if (PLAY.name().equals(s)) return PLAY;
            return null;
        }
    }

    /**
     * Returns Java srccode file root in relation to the
     * {@code appBase}
     */
    File source(File appBase);

    /**
     * Returns test source code file root in test scope in relation
     * to the {@code appBase}
     * @param appBase
     * @return test source code file root
     */
    File testSource(File appBase);

    /**
     * Returns Resource files root in relation to the
     * {@code appBase} specified
     *
     * @param appBase
     * @return resource file root
     */
    File resource(File appBase);

    /**
     * Returns test resource files root in test scope in relation to the
     * {@code appBase} specified
     *
     * @param appBase
     * @return test resource file root
     */
    File testResource(File appBase);

    /**
     * Returns lib folder which contains arbitrary jar files in relation to the
     * {@code appBase} specified
     */
    File lib(File appBase);

    /**
     * Returns lib folder which contains arbitrary jar files in relation to the
     * {@code appBase} specified in test scope
     */
    File testLib(File appBase);

    /**
     * Returns classes folder path that is related to the app base
     */
    String classes();

    /**
     * Returns the build target folder in relation to the
     * {@code appBase} specified
     */
    File target(File appBase);

    /**
     * Returns the routing table file in relation to the
     * {@code appBase} specified. Files are organized by
     * named port names
     */
    Map<String, List<File>> routeTables(File appBase);

    /**
     * Returns the app configuration location in relation to the
     * {@code appBase} specified.
     * <p>The configuration location could be either a File or
     * a directory that contains a list of properties files or
     * contains sub directories of a list of properties files</p>
     */
    File conf(File appBase);

    enum util {
        ;

        /**
         * check if a dir is application base as per given project layout
         *
         * @param dir    the folder to be probed
         * @param layout the project layout used to probe the folder
         * @return {@code true if the folder is app base as per given project layout}
         */
        public static boolean probeAppBase(File dir, ProjectLayout layout) {
            // try conf file
            File conf = layout.conf(dir);
            if (null != conf && conf.canRead() && conf.isFile()) {
                return true;
            }
            // try source path
            File src = layout.source(dir);
            if (null != src && src.canRead() && src.isDirectory()) {
                // try target path
                File tgt = layout.target(dir);
                return (null != tgt && tgt.canRead() && tgt.isDirectory());
            }
            return false;
        }

        /**
         * Build project layout from properties file. The file content shall match the
         * project layout interface structure, e.g:
         * <pre>
         * srccode=src/main/java
         * lib=lib
         * routes=src/main/resources/routes
         * conf=src/main/resources/conf
         * target=tmp
         * </pre>
         *
         * @param p
         * @return a ProjectLayout instance
         */
        public static ProjectLayout build(Properties p) {
            String source = _get("source", p);
            String testSource = _get("testSource", p);
            String lib = _get("lib", p);
            String testLib = _get("testLib", p);
            String resource = _get("resource", p);
            String testResource = _get("testResource", p);
            String routes = _get("routes", p);
            String conf = _get("conf", p);
            String target = _get("target", p);
            return new CustomizedProjectLayout(source, testSource, resource, testResource, lib, testLib, target, routes, conf);
        }

        private static String _get(String key, Properties p) {
            String s = p.getProperty(key);
            E.invalidConfigurationIf(null == s, "cannot findBy '%s' setting in project layout properties", key);
            return s;
        }
    }

    class CustomizedProjectLayout implements ProjectLayout {
        private String source;
        private String testSource;
        private String lib;
        private String testLib;
        private String routeTable;
        private String conf;
        private String target;
        private String resource;
        private String testResource;

        public CustomizedProjectLayout(String src, String testSource, String resource, String testResource, String lib, String testLib, String tgt, String routeTable, String conf) {
            this.source = src;
            this.testSource = testSource;
            this.resource = resource;
            this.testResource = testResource;
            this.lib = lib;
            this.testLib = testLib;
            this.target = tgt;
            this.routeTable = routeTable;
            this.conf = conf;
        }

        @Override
        public File source(File appBase) {
            return file(appBase, source);
        }

        @Override
        public File testSource(File appBase) {
            return file(appBase, testSource);
        }

        @Override
        public File lib(File appBase) {
            return file(appBase, lib);
        }

        @Override
        public File testLib(File appBase) {
            return file(appBase, testLib);
        }

        @Override
        public Map<String, List<File>> routeTables(File appBase) {
            Map<String, List<File>> map = new HashMap<>();
            map.put(NamedPort.DEFAULT, routeTables(appBase, routeTable));
            // TODO support extended route table (i.e for named ports)
            return map;
        }

        private List<File> routeTables(File appBase, String routeTable) {
            List<File> files = new ArrayList<>();
            files.add(file(appBase, routeTable));
            File confRoot = file(appBase, conf);
            files.add(file(confRoot, routeTable));
            File profileRoot = file(confRoot, Act.profile());
            files.add(file(profileRoot, routeTable));
            return files;
        }

        @Override
        public File resource(File appBase) {
            return file(appBase, resource);
        }

        @Override
        public File testResource(File appBase) {
            return file(appBase, testResource);
        }

        @Override
        public String classes() {
            return RuntimeDirs.CLASSES;
        }

        @Override
        public File conf(File appBase) {
            return file(appBase, conf);
        }

        @Override
        public File target(File appBase) {
            return file(appBase, target);
        }
    }

    enum Utils {
        ;
        public static File file(File parent, String path) {
            try {
                return new File(parent, path).getCanonicalFile();
            } catch (IOException e) {
                throw E.ioException(e);
            }
        }
    }


    enum F {
        ;
        public static $.F2<File, ProjectLayout, File> SRC = new $.F2<File, ProjectLayout, File>() {
            @Override
            public File apply(File base, ProjectLayout layout) throws NotAppliedException, $.Break {
                return layout.source(base);
            }
        };
        public static $.F2<File, ProjectLayout, File> RSRC = new $.F2<File, ProjectLayout, File>() {
            @Override
            public File apply(File base, ProjectLayout layout) throws NotAppliedException, $.Break {
                return layout.resource(base);
            }
        };
        public static $.F2<File, ProjectLayout, File> LIB = new $.F2<File, ProjectLayout, File>() {
            @Override
            public File apply(File base, ProjectLayout layout) throws NotAppliedException, $.Break {
                return layout.lib(base);
            }
        };
        public static $.F2<File, ProjectLayout, File> TST_SRC = new $.F2<File, ProjectLayout, File>() {
            @Override
            public File apply(File base, ProjectLayout layout) throws NotAppliedException, $.Break {
                return layout.testSource(base);
            }
        };
        public static $.F2<File, ProjectLayout, File> TST_RSRC = new $.F2<File, ProjectLayout, File>() {
            @Override
            public File apply(File base, ProjectLayout layout) throws NotAppliedException, $.Break {
                return layout.testResource(base);
            }
        };
        public static $.F2<File, ProjectLayout, File> TST_LIB = new $.F2<File, ProjectLayout, File>() {
            @Override
            public File apply(File base, ProjectLayout layout) throws NotAppliedException, $.Break {
                return layout.testLib(base);
            }
        };
    }
}
