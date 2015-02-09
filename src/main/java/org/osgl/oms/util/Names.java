package org.osgl.oms.util;

import org.osgl.util.FastStr;
import org.osgl.util.S;

import java.io.File;

/**
 * utilities to manipulate class names
 */
public enum Names {
    ;
    public static String fileToClass(String fileName) {
        if (File.separatorChar != '/') {
            fileName = fileName.replace(File.separatorChar, '/');
        }
        return S.beforeLast(fileName.replace('/', '.'), ".");
    }

    public static String fileToClass(File baseDir, String filePath) {
        FastStr path0 = FastStr.of(baseDir.getAbsolutePath());
        FastStr path1 = FastStr.of(filePath);
        if (File.separatorChar != '/') {
            path0 = path0.replace(File.separatorChar, '/');
            path1 = path1.replace(File.separatorChar, '/');
        }
        return path1.afterFirst(path0).replace('/', '.').beforeLast('.').toString();
    }
}
