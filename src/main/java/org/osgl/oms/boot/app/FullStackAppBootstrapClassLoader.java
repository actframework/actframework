package org.osgl.oms.boot.app;

import org.osgl._;
import org.osgl.oms.BytecodeEnhancerManager;
import org.osgl.oms.OMS;
import org.osgl.oms.asm.ClassReader;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.boot.BootstrapClassLoader;
import org.osgl.oms.boot.PluginClassProvider;
import org.osgl.oms.util.BytecodeVisitor;
import org.osgl.oms.util.Jars;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.osgl.oms.Constants.*;

/**
 * This class loader is responsible for loading OMS classes
 */
public class FullStackAppBootstrapClassLoader extends BootstrapClassLoader {

    private static final String KEY_CLASSPATH = "java.class.path";

    private Map<String, byte[]> libBC = C.newMap();
    private List<Class<?>> omsClasses = C.newList();

    public FullStackAppBootstrapClassLoader(ClassLoader parent) {
        super(parent);
        preload();
    }

    public FullStackAppBootstrapClassLoader() {
    }

    @Override
    public List<Class<?>> pluginClasses() {
        if (omsClasses.isEmpty()) {
            for (String className : C.list(libBC.keySet())) {
                try {
                    Class<?> c = loadClass(className, true);
                    omsClasses.add(c);
                } catch (ClassNotFoundException e) {
                    // ignore
                } catch (NoClassDefFoundError e) {
                    // ignore
                }
            }
        }
        return C.list(omsClasses);
    }

    protected void preload() {
        buildIndex();
    }

    private List<File> jars() {
        C.List<String> path = C.listOf(System.getProperty(KEY_CLASSPATH).split(File.pathSeparator));
        path = path.filter(S.F.contains("jre" + File.separator + "lib").negate().and(S.F.endsWith(".jar")));
        return path.map(new _.Transformer<String, File>() {
            @Override
            public File transform(String s) {
                return new File(s);
            }
        });
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

        if (name.startsWith("java") || name.startsWith("org.springframework") || name.startsWith("org.apache")) {
            return super.loadClass(name, resolve);
        }

        if (!protectedClasses.contains(name)) {
            c = loadOmsClass(name, resolve);
        }

        if (null == c) {
            if (name.contains("springframework")) {
                System.out.println(name);
            }
            return super.loadClass(name, resolve);
        } else {
            return c;
        }
    }

    protected byte[] tryLoadResource(String name) {
        return null;
    }

    protected Class<?> loadOmsClass(String name, boolean resolve) {

        if ("io.undertow.UndertowLogger".equals(name)) {
            System.out.println("about to load Undertow logger");
        }

        byte[] ba = libBC.remove(name);

        if (null == ba) {
            ba = tryLoadResource(name);
        }

        if (null == ba) {
            return findLoadedClass(name);
        }

        Class<?> c = null;
        if (name.startsWith(OMS_PKG) || name.startsWith(ASM_PKG)) {
            // skip bytecode enhancement for asm classes or non oms classes
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
