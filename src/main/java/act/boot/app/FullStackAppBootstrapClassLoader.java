package act.boot.app;

import act.Constants;
import act.boot.BootstrapClassLoader;
import act.util.ActClassLoader;
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import act.util.Jars;
import org.osgl._;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

import static act.util.ClassInfoRepository.canonicalName;

/**
 * This class loader is responsible for loading Act classes
 */
public class FullStackAppBootstrapClassLoader extends BootstrapClassLoader implements ActClassLoader {

    private static final String KEY_CLASSPATH = "java.class.path";

    private Map<String, byte[]> libBC = C.newMap();
    private List<Class<?>> actClasses = C.newList();

    public FullStackAppBootstrapClassLoader(ClassLoader parent) {
        super(parent);
        preload();
    }

    public FullStackAppBootstrapClassLoader() {
    }

    @Override
    public List<Class<?>> pluginClasses() {
        if (actClasses.isEmpty()) {
            for (String className : C.list(libBC.keySet())) {
                try {
                    Class<?> c = loadClass(className, true);
                    cache(c);
                } catch (ClassNotFoundException e) {
                    // ignore
                } catch (NoClassDefFoundError e) {
                    // ignore
                }
            }
        }
        return C.list(actClasses);
    }

    protected void preload() {
        buildIndex();
    }

    private List<File> jars() {
        C.List<String> path = C.listOf(System.getProperty(KEY_CLASSPATH).split(File.pathSeparator));
        if (path.size() < 10) {
            ClassLoader cl = getClass().getClassLoader();
            if (cl instanceof URLClassLoader) {
                URLClassLoader realm = (URLClassLoader) cl;
                C.List<URL> urlList = C.listOf(realm.getURLs());
                urlList = urlList.filter(new _.Predicate<URL>() {
                    @Override
                    public boolean test(URL url) {
                        return url.getFile().endsWith(".jar");
                    }
                });
                return urlList.map(new _.Transformer<URL, File>() {
                    @Override
                    public File transform(URL url) {
                        try {
                            return new File(url.toURI());
                        } catch (Exception e) {
                            throw E.unexpected(e);
                        }
                    }
                });
            }
        }
        path = path.filter(S.F.contains("jre" + File.separator + "lib").negate().and(S.F.endsWith(".jar")));
        return path.map(new _.Transformer<String, File>() {
            @Override
            public File transform(String s) {
                return new File(s);
            }
        });
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
        ClassNode node = repo.node(cname);
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

        if (name.startsWith("java") || name.startsWith("org.springframework") || name.startsWith("org.apache") || name.startsWith("org.osgl")) {
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

}
