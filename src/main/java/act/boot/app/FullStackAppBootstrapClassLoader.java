package act.boot.app;

import act.Constants;
import act.boot.BootstrapClassLoader;
import act.plugin.Plugin;
import act.util.ActClassLoader;
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import act.util.Jars;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static act.util.ClassInfoRepository.canonicalName;

/**
 * This class loader is responsible for loading Act classes
 */
public class FullStackAppBootstrapClassLoader extends BootstrapClassLoader implements ActClassLoader {

    private static final String KEY_CLASSPATH = "java.class.path";

    private List<File> jars;
    private Long jarsChecksum;
    private Map<String, byte[]> libBC = C.newMap();
    private List<Class<?>> actClasses = C.newList();
    private List<Class<?>> pluginClasses = new ArrayList<>();

    public FullStackAppBootstrapClassLoader(ClassLoader parent) {
        super(parent);
        preload();
    }

    @Override
    public List<Class<?>> pluginClasses() {
        if (classInfoRepository().isEmpty()) {
            restoreClassInfoRegistry();
            restorePluginClasses();
            if (classInfoRepository.isEmpty()) {
                for (String className : C.list(libBC.keySet())) {
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

    protected void preload() {
        buildIndex();
    }

    protected List<File> jars() {
        if (null == jars) {
            jars = jars(FullStackAppBootstrapClassLoader.class.getClassLoader());
            jarsChecksum = calculateChecksum(jars);
        }
        return jars;
    }

    protected long jarsChecksum() {
        jars();
        return jarsChecksum;
    }

    public static List<File> jars(ClassLoader cl) {
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
                return urlList.map(new $.Transformer<URL, File>() {
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
        path = path.filter(S.F.contains("jre" + File.separator + "lib").negate().and(S.F.endsWith(".jar")));
        return path.map(new $.Transformer<String, File>() {
            @Override
            public File transform(String s) {
                return new File(s);
            }
        }).sorted();
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
            sb.append(c.getName()).append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        saveToFile(".act.plugins", sb.toString());
    }

    private void saveToFile(String name, String content) {
        StringBuilder sb = S.builder("#").append(jarsChecksum);
        sb.append("\n").append(content);
        File file = new File(name);
        IO.writeContent(sb.toString(), file);
    }

    private void restoreClassInfoRegistry() {
        List<String> list = restoreFromFile(".act.class-registry");
        if (list.isEmpty()) {
            return;
        }
        String json = S.join("\n", list);
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
            String[] sa = content.split("\n");
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
        if (c != null) {
            return c;
        }

        if (name.startsWith("java") || name.startsWith("org.osgl")) {
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
