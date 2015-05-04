package org.osgl.oms.util;

import org.osgl.util.FastStr;
import org.osgl.util.S;

import java.io.File;

/**
 * utilities to manipulate class names
 */
public enum ClassNames {
    ;

    public static String classFileNameToClassName(String fileName) {
        if (File.separatorChar != '/') {
            fileName = fileName.replace(File.separatorChar, '/');
        }
        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }
        return S.beforeLast(fileName.replace('/', '.'), ".");
    }

    public static String classNameToClassFileName(String className) {
        return classNameToClassFileName(className, false);
    }

    public static String classNameToClassFileName(String className, boolean keepInnerClass) {
        FastStr fs = FastStr.of(className);
        if (!keepInnerClass && className.contains("$")) {
            fs = fs.beforeFirst('$');
        }
        fs = fs.replace('.', '/').append(".class").prepend('/');
        return fs.toString();
    }

    public static String sourceFileNameToClassName(File baseDir, String filePath) {
        if (!filePath.endsWith(".java")) {
            return null;
        }
        FastStr path0 = FastStr.of(baseDir.getAbsolutePath());
        FastStr path1 = FastStr.of(filePath);
        if (File.separatorChar != '/') {
            path0 = path0.replace(File.separatorChar, '/');
            path1 = path1.replace(File.separatorChar, '/');
        }
        return path1.afterFirst(path0).replace('/', '.').beforeLast('.').toString();
    }
}
