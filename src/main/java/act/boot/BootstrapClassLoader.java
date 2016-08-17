package act.boot;

import act.BytecodeEnhancerManager;
import act.asm.ClassReader;
import act.asm.ClassWriter;
import act.boot.app.FullStackAppBootstrapClassLoader;
import act.boot.server.ServerBootstrapClassLoader;
import act.util.ActClassLoader;
import act.util.ByteCodeVisitor;
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;

/**
 * Base class for Act class loaders
 */
public abstract class BootstrapClassLoader extends ClassLoader implements PluginClassProvider, ActClassLoader {

    public static final String FILE_SCAN_LIST = "act.scan.list";
    protected final Class<?> PLUGIN_CLASS;

    protected static final Logger logger = L.get(BootstrapClassLoader.class);

    private BytecodeEnhancerManager enhancerManager = new BytecodeEnhancerManager();
    protected ClassInfoRepository classInfoRepository = new ClassInfoRepository();

    protected BootstrapClassLoader(ClassLoader parent) {
        super(parent);
        PLUGIN_CLASS = $.classForName("act.plugin.Plugin", this);
    }

    @Override
    public ClassInfoRepository classInfoRepository() {
        return classInfoRepository;
    }

    public Class<?> loadedClass(String name) {
        Class<?> c = findLoadedClass(name);
        if (null == c) {
            ClassLoader p = getParent();
            if (null != p && p instanceof ActClassLoader) {
                return ((ActClassLoader)p).loadedClass(name);
            }
        }
        return c;
    }

    private static ClassLoader _getParent() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (null == cl) {
            cl = ClassLoader.getSystemClassLoader();
        }
        return cl;
    }

    protected Class<?> defineClassX(String name, byte[] b, int off, int len,
                                         ProtectionDomain protectionDomain) {
        int i = name.lastIndexOf('.');
        if (i != -1) {
            String pkgName = name.substring(0, i);
            // Check if package already loaded.
            if (getPackage(pkgName) == null) {
                try {
                        definePackage(pkgName, null, null, null, null, null, null, null);
                } catch (IllegalArgumentException iae) {
                        throw new AssertionError("Cannot find package " +
                                pkgName);
                }
            }
        }
        return super.defineClass(name, b, off, len, protectionDomain);
    }

    protected Class<?> defineClass(String name, byte[] ba) {
        Class<?> c;
        $.Var<ClassWriter> cw = $.val(null);
        ByteCodeVisitor enhancer = enhancerManager.generalEnhancer(name, cw);
        if (null == enhancer) {
            c = defineClassX(name, ba, 0, ba.length, DOMAIN);
        } else {
            ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cw.set(w);
            enhancer.commitDownstream();
            ClassReader r;
            r = new ClassReader(ba);
            try {
                r.accept(enhancer, 0);
                byte[] baNew = w.toByteArray();
                c = defineClassX(name, baNew, 0, baNew.length, DOMAIN);
            } catch (RuntimeException e) {
                throw e;
            } catch (Error e) {
                throw e;
            } catch (Exception e) {
                throw E.unexpected("Error processing class " + name);
            }
        }
        return c;
    }

    protected static java.security.ProtectionDomain DOMAIN;

    static {
        DOMAIN = (java.security.ProtectionDomain)
                java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction() {
                            public Object run() {
                                return BootstrapClassLoader.class.getProtectionDomain();
                            }
                        });
    }

    protected static final Set<String> protectedClasses = C.set(
            BootstrapClassLoader.class.getName(),
            ClassInfoRepository.class.getName(),
            ClassNode.class.getName(),
            ServerBootstrapClassLoader.class.getName(),
            FullStackAppBootstrapClassLoader.class.getName(),
            ActClassLoader.class.getName(),
            PluginClassProvider.class.getName()
            //Plugin.class.getName(),
            //ClassFilter.class.getName()
    );

    public Set<String> scanList() {
        Set<String> scanList = new HashSet<>();
        try {
            final Enumeration<URL> systemResources = this.getResources(FILE_SCAN_LIST);
            while (systemResources.hasMoreElements()) {
                InputStream is = systemResources.nextElement().openStream();
                String s = IO.readContentAsString(is);
                scanList.addAll(C.listOf(s.split("[\r\n]+")).filter(S.F.startsWith("#").negate()));
            }
        } catch (IOException e) {
            throw E.ioException(e);
        }
        return scanList;
    }

}
