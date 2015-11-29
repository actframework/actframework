package act.util;

import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Provides utilities manipulating jar files
 */
public enum Jars {
    ;

    private static Logger logger = L.get(Jars.class);

    public static Map<String, byte[]> buildClassNameIndex(File dir) {
        return buildClassNameIndex(dir, $.F.FALSE);
    }

    public static Map<String, byte[]> buildClassNameIndex(File dir, final $.Func1<String, Boolean> ignoredClassNames) {
        final Map<String, byte[]> idx = C.newMap();
        _F.JarEntryVisitor visitor = _F.classNameIndexBuilder(idx, ignoredClassNames);
        scanDir(dir, visitor);
        return idx;
    }

    public static Map<String, byte[]> buildClassNameIndex(List<File> jars) {
        return buildClassNameIndex(jars, $.F.FALSE);
    }

    public static Map<String, byte[]> buildClassNameIndex(List<File> jars, final $.Func1<String, Boolean> ignoredClassNames) {
        final Map<String, byte[]> idx = C.newMap();
        _F.JarEntryVisitor visitor = _F.classNameIndexBuilder(idx, ignoredClassNames);
        scanList(jars, visitor);
        return idx;
    }

    /**
     * If the class is loaded from a Jar file, then return that file. Otherwise
     * return {@code null}
     */
    public static File probeJarFile(Class<?> clazz) {
        String fileName = ClassNames.classNameToClassFileName(clazz.getName());
        URL url = clazz.getClassLoader().getResource(fileName);
        if (null != url && "jar".equals(url.getProtocol())) {
            String path = url.getPath();
            String file = S.str(path).afterFirst("file:").beforeFirst(".jar!").append(".jar").toString();
            return new File(file);
        } else {
            return null;
        }
    }

    /**
     * @param file
     */
    public static void scanForBytecode(File file, final $.F1<byte[], ?> bytecodeHandler) {
        _F.JarEntryVisitor visitor = new _F.JarEntryVisitor() {
            @Override
            public Void apply(JarFile jarFile, JarEntry entry) throws NotAppliedException, $.Break {
                try {
                    byte[] ba = getBytes(jarFile, entry);
                    bytecodeHandler.apply(ba);
                } catch (IOException e) {
                    throw E.ioException(e);
                }
                return null;
            }
        };
        if (file.isDirectory()) {
            scanDir(file, visitor);
        } else {
            try {
                scanFile(file, visitor);
            } catch (IOException e) {
                logger.warn(e, "Error scanning jar file: %s", file.getName());
            }
        }
    }

    private static void scanList(List<File> jars, _F.JarEntryVisitor visitor) {
        for (int i = 0, j = jars.size(); i < j; ++i) {
            File jar = jars.get(i);
            try {
                scanFile(jar, visitor);
            } catch (IOException e) {
                logger.warn(e, "Error scanning jar file: %s", jar.getName());
            }
        }
    }

    private static void scanDir(File jarDir, _F.JarEntryVisitor visitor) {
        File[] jars = jarDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        int n = jars.length;
        for (int i = 0; i < n; ++i) {
            File file = jars[i];
            try {
                scanFile(file, visitor);
            } catch (IOException e) {
                logger.warn(e, "Error scanning jar file: %s", file.getName());
            }
        }
    }

    private static void scanFile(File file, _F.JarEntryVisitor visitor) throws IOException {
        JarFile jar = new JarFile(file);
        try {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".class")) {
                    continue;
                }
                visitor.apply(jar, entry);
            }
        } finally {
            jar.close();
        }
    }

    private static byte[] getBytes(JarFile jar, JarEntry entry) throws IOException {
        InputStream is = jar.getInputStream(entry);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IO.copy(is, baos);
        return baos.toByteArray();
    }

    private enum _F {
        ;

        static abstract class JarEntryVisitor extends $.F2<JarFile, JarEntry, Void> {
        }

        static JarEntryVisitor classNameIndexBuilder(final Map<String, byte[]> map, final $.Function<String, Boolean> ignoredClassNames) {
            return new _F.JarEntryVisitor() {
                @Override
                public Void apply(JarFile jarFile, JarEntry entry) throws NotAppliedException, $.Break {
                    try {
                        String className = ClassNames.classFileNameToClassName(entry.getName());
                        if (!ignoredClassNames.apply(className)) {
                            map.put(className, getBytes(jarFile, entry));
                        }
                    } catch (IOException e) {
                        throw E.ioException(e);
                    }
                    return null;
                }
            };
        }
    }

}
