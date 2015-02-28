package org.osgl.oms.util;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.*;
import java.util.Enumeration;
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
        return buildClassNameIndex(dir, _.F.FALSE);
    }

    public static Map<String, byte[]> buildClassNameIndex(File dir, final _.Func1<String, Boolean> ignoredClassNames) {
        final Map<String, byte[]> idx = C.newMap();
        _F.JarEntryVisitor visitor = new _F.JarEntryVisitor() {
            @Override
            public Void apply(JarFile jarFile, JarEntry entry) throws NotAppliedException, _.Break {
                try {
                    String className = ClassNames.classFileNameToClassName(entry.getName());
                    if (!ignoredClassNames.apply(className)) {
                        idx.put(className, getBytes(jarFile, entry));
                    }
                } catch (IOException e) {
                    throw E.ioException(e);
                }
                return null;
            }
        };
        scanDir(dir, visitor);
        return idx;
    }

    /**
     *
     * @param file
     */
    public static void scanForBytecode(File file, final _.F1<byte[], ?> bytecodeHandler) {
        _F.JarEntryVisitor visitor = new _F.JarEntryVisitor() {
            @Override
            public Void apply(JarFile jarFile, JarEntry entry) throws NotAppliedException, _.Break {
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

    private static enum _F {
        ;

        static abstract class JarEntryVisitor extends _.F2<JarFile, JarEntry, Void> {
        }
    }

}
