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

import static act.util.ClassInfoRepository.canonicalName;

import act.Constants;
import act.boot.BootstrapClassLoader;
import act.util.*;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * This class loader is responsible for loading Act classes
 */
public class FullStackAppBootstrapClassLoader extends BootstrapClassLoader implements ActClassLoader {

    private static final Logger LOGGER = LogManager.get(FullStackAppBootstrapClassLoader.class);

    public static final String KEY_CLASSPATH = "java.class.path";

    /**
     * the {@link System#getProperty(String) system property} key to get
     * the ignored jar file name prefix; multiple prefixes can be specified
     * with comma `,`
     *
     * The default value is defined in {@link #DEF_JAR_IGNORE}
     * ``
     */
    public static final String KEY_SYS_JAR_IGNORE = "act.jar.sys.ignore";

    /**
     * the {@link System#getProperty(String) system property} key to get
     * the ignored jar file name prefix; multiple prefixes can be specified
     * with comma `,`
     */
    public static final String KEY_APP_JAR_IGNORE = "act.jar.app.ignore";

    public static final String DEF_JAR_IGNORE = "act-asm,activation-,antlr," +
            "asm-,byte-buddy,byte-buddy-agent," +
            "cdi-api,cglib,commons-,core-,curvesapi," +
            "debugger-agent,ecj-,guava-,hibernate-," +
            "idea_rt,image4j-," +
            "jansi-,javase-,javaparser,javax.,jboss-,jcl-,jcommander-," +
            "jfiglet,jline-,jsoup-,jxls-,joda-,kryo-,logback-," +
            "mail-,mongo-,mvel,newrelic," +
            "okio-,okhttp,org.apache.," +
            "pat-,patchca,poi-,proxytoys," +
            "reflectasm-,rythm-engine," +
            "slf4j-,snakeyaml,stax-,undertow-,xmlbeans-,xnio";

    private final Class<?> PLUGIN_CLASS;

    private List<File> jars;
    private Long jarsChecksum;
    private Map<String, byte[]> libBC = new HashMap<>();
    private List<Class<?>> actClasses = new ArrayList<>();
    private List<Class<?>> pluginClasses = new ArrayList<>();
    private String lineSeparator = OS.get().lineSeparator();
    private final $.Predicate<File> jarFilter = jarFilter();

    public FullStackAppBootstrapClassLoader(ClassLoader parent) {
        super(parent);
        preload();
        PLUGIN_CLASS = $.classForName("act.plugin.Plugin", this);
    }

    @Override
    public List<Class<?>> pluginClasses() {
        if (classInfoRepository().isEmpty()) {
            try {
                restoreClassInfoRegistry();
                restorePluginClasses();
            } catch (RuntimeException e) {
                LOGGER.warn(e, "Error restoring class info registry or plugin classes");
                classInfoRepository.reset();
            }
            if (classInfoRepository.isEmpty()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("classInfoRegistry not recovered, start searching through libBC (with total %s classes)", libBCSize());
                }
                for (String className : C.list(libBC.keySet())) {
                    if (className.endsWith("module-info") || className.startsWith("META-INF.versions.")) {
                        continue;
                    }
                    try {
                        Class<?> c = loadClass(className, true);
                        cache(c);
                        int modifier = c.getModifiers();
                        if (Modifier.isAbstract(modifier) || !Modifier.isPublic(modifier) || c.isInterface()) {
                            continue;
                        }
                        if (PLUGIN_CLASS.isAssignableFrom(c)) {
                            pluginClasses.add(c);
                        }
                    } catch (ClassNotFoundException e) {
                        // ignore
                    } catch (NoClassDefFoundError e) {
                        // ignore
                    }
                }
                saveClassInfoRegistry();
                savePluginClasses();
            }
        }
        return pluginClasses;
    }

    public int libBCSize() {
        return libBC.size();
    }

    @Override
    public URL getResource(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return super.getResources(name);
    }

    /**
     * Returns all jar files in the class loader without filtering.
     * @return all jar files
     */
    public List<File> allJars() {
        return jars(false);
    }

    protected void preload() {
        buildIndex();
    }

    /**
     * Returns filtered jar files in the class loader.
     * @return filtered jar files.
     */
    protected List<File> jars() {
        return jars(true);
    }

    protected List<File> jars(boolean filter) {
        if (null == jars) {
            jars = jars(FullStackAppBootstrapClassLoader.class.getClassLoader(), filter);
            jarsChecksum = calculateChecksum(jars);
        }
        return jars;
    }

    protected long jarsChecksum() {
        jars();
        return jarsChecksum;
    }

    private $.Predicate<File> jarFilter() {
        final Set<String> blackList = jarBlackList();
        final Set<String> whiteList = new HashSet<>();
        for (String s : blackList) {
            if (s.startsWith("-")) {
                whiteList.add(s.substring(1));
            }
        }
        String ignores = System.getProperty(KEY_SYS_JAR_IGNORE);
        if (null == ignores) {
            ignores = DEF_JAR_IGNORE;
        }
        String appIgnores = System.getProperty(KEY_APP_JAR_IGNORE);
        if (null != appIgnores) {
            ignores += ("," + appIgnores);
        }
        blackList.addAll(S.fastSplit(ignores, ","));
        if ($.JAVA_VERSION < 9) {
            blackList.add("jaxb-");
            blackList.add("javax.annotation-");
        }
        return new $.Predicate<File>() {
            @Override
            public boolean test(File file) {
                String name = file.getName();
                for (String prefix : blackList) {
                    if (S.blank(prefix)) {
                        continue;
                    }
                    if (name.startsWith(prefix)) {
                        boolean whiteListed = false;
                        for (String s : whiteList) {
                            if (name.startsWith(s)) {
                                whiteListed = true;
                            }
                        }
                        if (!whiteListed) {
                            return false;
                        }
                    }
                }
                return true;
            }
        };
    }

    private List<File> filterJars(List<File> jars) {
        return C.list(jars).filter(jarFilter);
    }

    public List<File> jars(ClassLoader cl) {
        return jars(cl, true);
    }

    public List<File> jars(ClassLoader cl, boolean filter) {
        List<File> jars = null;
        C.List<String> path = C.listOf(System.getProperty(KEY_CLASSPATH).split(File.pathSeparator));
        if (path.size() < 10) {
            if (cl instanceof URLClassLoader) {
                URLClassLoader realm = (URLClassLoader) cl;
                C.List<URL> urlList = C.listOf(realm.getURLs());
                urlList = urlList.filter(new $.Predicate<URL>() {
                    @Override
                    public boolean test(URL url) {
                        return url.getFile().endsWith(".jar");
                    }
                });
                jars = urlList.map(new $.Transformer<URL, File>() {
                    @Override
                    public File transform(URL url) {
                        try {
                            return new File(url.toURI());
                        } catch (Exception e) {
                            throw E.unexpected(e);
                        }
                    }
                }).sorted();
            }
        }
        if (null == jars) {
            path = path.filter(S.F.contains("jre" + File.separator + "lib").negate().and(S.F.endsWith(".jar")));
            jars = path.map(new $.Transformer<String, File>() {
                @Override
                public File transform(String s) {
                    return new File(s);
                }
            }).sorted();
        }
        return filter ? filterJars(jars) : jars;
    }

    private void saveClassInfoRegistry() {
        saveToFile(".act.class-registry", classInfoRepository().toJSON());
    }

    private void savePluginClasses() {
        if (pluginClasses.isEmpty()) {
            return;
        }
        StringBuilder sb = S.builder();
        for (Class c : pluginClasses) {
            sb.append(c.getName()).append(lineSeparator);
        }
        sb.deleteCharAt(sb.length() - 1);
        saveToFile(".act.plugins", sb.toString());
    }

    private void saveToFile(String name, String content) {
        File file = new File(name);
        String fileContent = S.concat("#", S.string(jarsChecksum), lineSeparator, content);
        IO.write(fileContent, file);
    }

    private void restoreClassInfoRegistry() {
        List<String> list = restoreFromFile(".act.class-registry");
        if (list.isEmpty()) {
            return;
        }
        String json = S.join(lineSeparator, list);
        classInfoRepository = ClassInfoRepository.parseJSON(json);
    }

    private void restorePluginClasses() {
        List<String> list = restoreFromFile(".act.plugins");
        if (list.isEmpty()) {
            return;
        }
        for (String s : list) {
            pluginClasses.add($.classForName(s, this));
        }
    }

    private List<String> restoreFromFile(String name) {
        File file = new File(name);
        if (file.canRead()) {
            String content = IO.readContentAsString(file);
            String[] sa = content.split(lineSeparator);
            long fileChecksum = Long.parseLong(sa[0].substring(1));
            if (jarsChecksum.equals(fileChecksum)) {
                return C.listOf(sa).drop(1);
            }
        }
        return C.list();
    }

    private synchronized ClassNode cache(Class<?> c) {
        String cname = canonicalName(c);
        if (null == cname) {
            return null;
        }
        String name = c.getName();
        ClassInfoRepository repo = (ClassInfoRepository)classInfoRepository();
        if (repo.has(cname)) {
            return repo.node(name, cname);
        }
        actClasses.add(c);
        ClassNode node = repo.node(name);
        node.modifiers(c.getModifiers());
        Class[] ca = c.getInterfaces();
        for (Class pc: ca) {
            if (pc == Object.class) continue;
            String pcname = canonicalName(pc);
            if (null != pcname) {
                cache(pc);
                node.addInterface(pcname);
            }
        }
        Class pc = c.getSuperclass();
        if (null != pc && Object.class != pc) {
            String pcname = canonicalName(pc);
            if (null != pcname) {
                cache(pc);
                node.parent(pcname);
            }
        }
        return node;
    }

    private void buildIndex() {
        libBC.putAll(Jars.buildClassNameIndex(jars()));
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (null != c) {
            return c;
        }

        if (name.startsWith("java") || name.startsWith("org.slf4j")) {
            return super.loadClass(name, resolve);
        }

        if (!protectedClasses.contains(name)) {
            c = loadActClass(name, resolve);
        }

        if (null == c) {
            return super.loadClass(name, resolve);
        } else {
            return c;
        }
    }

    protected byte[] tryLoadResource(String name) {
        return null;
    }

    protected Class<?> loadActClass(String name, boolean resolve) {
        byte[] ba = libBC.remove(name);

        if (null == ba) {
            ba = tryLoadResource(name);
        }

        if (null == ba) {
            return findLoadedClass(name);
        }

        Class<?> c = null;
        if (name.startsWith(Constants.ACT_PKG) || name.startsWith(Constants.ASM_PKG)) {
            // skip bytecode enhancement for asm classes or non Act classes
            try {
                c = super.defineClass(name, ba, 0, ba.length, DOMAIN);
            } catch (NoClassDefFoundError e) {
                return null;
            }
        }

        if (null == c) {
            c = defineClass(name, ba);
        }
        if (resolve) {
            super.resolveClass(c);
        }
        return c;
    }

    public Class<?> createClass(String name, byte[] b) throws ClassFormatError {
        return super.defineClass(name, b, 0, b.length, DOMAIN);
    }

    public static long calculateChecksum(List<File> files) {
        long l = 0;
        for (File file : files) {
            l += file.hashCode() + file.lastModified();
        }
        return l;
    }

}
