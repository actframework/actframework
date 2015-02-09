package org.osgl.oms.util;

import org.osgl._;
import org.osgl.util.C;
import org.osgl.util.ListBuilder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

/**
 * A collection of file IO utilities
 */
public enum Files {
    ;
    public static List<File> filter(File baseDir, _.F1<String, Boolean> filter) {
        ListBuilder<File> list = ListBuilder.create(500);
        filter(baseDir, filter, C.F.addTo(list));
        return list.toList();
    }

    public static void filter(File baseDir, _.F1<String, Boolean> filter, _.F1<File, ?> visitor) {
        File[] files = baseDir.listFiles();
        if (null == files) {
            return;
        }
        int n = files.length;
        for (int i = 0; i < n; ++i) {
            File file = files[i];
            if (file.isDirectory() && !file.getName().startsWith(".")) {
                filter(file, filter, visitor);
            } else if (filter.apply(file.getName())) {
                visitor.apply(file);
            }
        }
    }
}
