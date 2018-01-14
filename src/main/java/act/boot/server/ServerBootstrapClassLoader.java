package act.boot.server;

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
import act.asm.ClassReader;
import act.asm.ClassWriter;
import act.boot.PluginClassProvider;
import act.boot.app.FullStackAppBootstrapClassLoader;
import act.util.ByteCodeVisitor;
import act.util.Jars;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.*;

import static act.Constants.*;

/**
 * This class loader is responsible for loading Act classes
 */
public class ServerBootstrapClassLoader extends ClassLoader implements PluginClassProvider {

    private static Logger logger = L.get(ServerBootstrapClassLoader.class);
    protected final Class<?> PLUGIN_CLASS;

    private File lib;
    private File plugin;

    private Map<String, byte[]> libBC = new HashMap<>();
    private Map<String, byte[]> pluginBC = new HashMap<>();
    private List<Class<?>> pluginClasses = new ArrayList<>();

    public ServerBootstrapClassLoader(ClassLoader parent) {
        super(parent);
        preload();
        PLUGIN_CLASS = $.classForName("act.plugin.Plugin", this);
    }

    public ServerBootstrapClassLoader() {
        this(_getParent());
    }

    private static ClassLoader _getParent() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (null == cl) {
            cl = ClassLoader.getSystemClassLoader();
        }
        return cl;
    }

    protected void preload() {
        String actHome = System.getProperty(ACT_HOME);
        if (null == actHome) {
            actHome = System.getenv(ACT_HOME);
        }
        if (null == actHome) {
            actHome = guessHome();
        }
        File home = new File(actHome);
        lib = new File(home, "lib");
        plugin = new File(home, "plugin");
        verifyHome();
        buildIndex();
    }

    private String guessHome() {
        return new File(".").getAbsolutePath();
    }

    private void verifyHome() {
        if (!verifyDir(lib) || !verifyDir(plugin)) {
            throw E.unexpected("Cannot load Act: can't find lib or plugin dir");
        }
    }

    private boolean verifyDir(File dir) {
        return dir.exists() && dir.canExecute() && dir.isDirectory();
    }

    private void buildIndex() {
        libBC.putAll(Jars.buildClassNameIndex(lib));
        pluginBC.putAll(Jars.buildClassNameIndex(plugin));
        File actJar = Jars.probeJarFile(Act.class);
        if (null == actJar) {
            logger.warn("Cannot find jar file for Act");
        } else {
            pluginBC.putAll(Jars.buildClassNameIndex(C.list(actJar)));
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        if (!protectedClasses.contains(name)) {
            c = loadActClass(name, resolve, false);
        }

        if (null == c) {
            return super.loadClass(name, resolve);
        } else {
            return c;
        }
    }

    public List<Class<?>> pluginClasses() {
        if (pluginClasses.isEmpty()) {
            for (String className : C.list(pluginBC.keySet())) {
                Class<?> c = loadActClass(className, true, true);
                assert null != c;
                int modifier = c.getModifiers();
                if (Modifier.isAbstract(modifier) || !Modifier.isPublic(modifier) || c.isInterface()) {
                    continue;
                }
                if (PLUGIN_CLASS.isAssignableFrom(c)) {
                    pluginClasses.add(c);
                }
            }
        }
        return C.list(pluginClasses);
    }

    protected byte[] tryLoadResource(String name) {
        return null;
    }

    protected Class<?> loadActClass(String name, boolean resolve, boolean pluginOnly) {
        boolean fromPlugin = false;
        byte[] ba = pluginBC.remove(name);
        if (null == ba) {
            if (!pluginOnly) {
                ba = libBC.remove(name);
            }
        } else {
            fromPlugin = true;
        }

        if (null == ba) {
            ba = tryLoadResource(name);
        }

        if (null == ba) {
            if (pluginOnly) {
                return findLoadedClass(name);
            }
            return null;
        }

        Class<?> c = null;
        if (!name.startsWith(ACT_PKG) || name.startsWith(ASM_PKG)) {
            // skip bytecode enhancement for asm classes or non Act classes
            c = super.defineClass(name, ba, 0, ba.length, DOMAIN);
        }

        if (null == c) {
            $.Var<ClassWriter> cw = $.val(null);
            ByteCodeVisitor enhancer = Act.enhancerManager().generalEnhancer(name, cw);
            if (null == enhancer) {
                c = super.defineClass(name, ba, 0, ba.length, DOMAIN);
            } else {
                ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                cw.set(w);
                enhancer.commitDownstream();
                ClassReader r;
                r = new ClassReader(ba);
                try {
                    r.accept(enhancer, 0);
                    byte[] baNew = w.toByteArray();
                    c = super.defineClass(name, baNew, 0, baNew.length, DOMAIN);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Error e) {
                    throw e;
                } catch (Exception e) {
                    throw E.unexpected("Error processing class " + name);
                }
            }
        }
        if (resolve) {
            super.resolveClass(c);
        }
        return c;
    }

    public Class<?> createClass(String name, byte[] b) throws ClassFormatError {
        return super.defineClass(name, b, 0, b.length, DOMAIN);
    }

    private static java.security.ProtectionDomain DOMAIN;

    static {
        DOMAIN = (java.security.ProtectionDomain)
                java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction() {
                            public Object run() {
                                return ServerBootstrapClassLoader.class.getProtectionDomain();
                            }
                        });
    }

    private static final Set<String> protectedClasses = C.set(
            ServerBootstrapClassLoader.class.getName(),
            FullStackAppBootstrapClassLoader.class.getName(),
            PluginClassProvider.class.getName()
            //Plugin.class.getName(),
            //ClassFilter.class.getName()
    );
}
