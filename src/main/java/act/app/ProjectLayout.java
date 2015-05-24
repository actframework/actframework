package act.app;

import act.Act;
import org.osgl.util.E;

import java.io.File;
import java.util.Properties;

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
    public static final String PROJ_LAYOUT_FILE = "proj.layout";

    public static enum PredefinedLayout implements ProjectLayout {
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
                return new File(appBase, "src/main/java");
            }

            @Override
            public File resource(File appBase) {
                return new File(appBase, "src/main/resources");
            }

            @Override
            public File lib(File appBase) {
                return new File(appBase, "src/main/webapp/WEB-INF/lib");
            }

            @Override
            public File asset(File appBase) {
                return new File(appBase, "src/main/webapp/WEB-INF/asset");
            }

            @Override
            public File target(File appBase) {
                return new File(appBase, "target");
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
                return new File(appBase, "app");
            }

            @Override
            public File resource(File appBase) {
                return new File(appBase, "conf");
            }

            @Override
            public File lib(File appBase) {
                return new File(appBase, "lib");
            }

            @Override
            public File asset(File appBase) {
                return new File(appBase, "public");
            }

            @Override
            public File target(File appBase) {
                return new File(appBase, "tmp");
            }
        },;

        @Override
        public File conf(File appBase) {
            File file = new File(resource(appBase), "app.conf");
            if (file.exists()) {
                return file;
            }
            return new File(resource(appBase), "app.conf");
        }

        @Override
        public File routeTable(File appBase) {
            return new File(resource(appBase), "routes");
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
     * {@code appBase} specified
     */
    File source(File appBase);

    /**
     * Returns Resource files root in relation to the
     * {@code appBase} specified
     *
     * @param appBase
     * @return
     */
    File resource(File appBase);

    /**
     * Returns lib folder which contains arbitrary jar files in relation to the
     * {@code appBase} specified
     */
    File lib(File appBase);

    /**
     * Returns asset folder which contains public accessible files like js/css/img etc
     * in relation to the {@code appBase} specified
     */
    File asset(File appBase);

    /**
     * Returns the build target folder in relation to the
     * {@code appBase} specified
     */
    File target(File appBase);

    /**
     * Returns the routing table file in relation to the
     * {@code appBase} specified
     */
    File routeTable(File appBase);

    /**
     * Returns the app configuration location in relation to the
     * {@code appBase} specified.
     * <p>The configuration location could be either a File or
     * a directory that contains a list of properties files or
     * contains sub directories of a list of properties files</p>
     */
    File conf(File appBase);

    public static enum util {
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
                return true;
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
            String lib = _get("lib", p);
            String resource = _get("resource", p);
            String asset = _get("asset", p);
            String routes = _get("routes", p);
            String conf = _get("conf", p);
            String target = _get("target", p);
            return new CustomizedProjectLayout(source, resource, lib, asset, target, routes, conf);
        }

        private static String _get(String key, Properties p) {
            String s = p.getProperty(key);
            E.invalidConfigurationIf(null == s, "cannot find '%s' setting in project layout properties", key);
            return s;
        }
    }

    public static class CustomizedProjectLayout implements ProjectLayout {
        private String source;
        private String lib;
        private String routeTable;
        private String conf;
        private String target;
        private String asset;
        private String resource;

        public CustomizedProjectLayout(String src, String resource, String lib, String asset, String tgt, String routeTable, String conf) {
            this.source = src;
            this.resource = resource;
            this.lib = lib;
            this.asset = asset;
            this.target = tgt;
            this.routeTable = routeTable;
            this.conf = conf;
        }

        @Override
        public File source(File appBase) {
            return new File(appBase, source);
        }

        @Override
        public File lib(File appBase) {
            return new File(appBase, lib);
        }

        @Override
        public File routeTable(File appBase) {
            return new File(appBase, routeTable);
        }

        @Override
        public File resource(File appBase) {
            return new File(appBase, resource);
        }

        @Override
        public File asset(File appBase) {
            return new File(appBase, asset);
        }

        @Override
        public File conf(File appBase) {
            return new File(appBase, conf);
        }

        @Override
        public File target(File appBase) {
            return new File(appBase, target);
        }
    }
}
