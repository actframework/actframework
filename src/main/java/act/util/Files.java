package act.util;

import act.app.ProjectLayout;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.ListBuilder;

import java.io.File;
import java.util.List;

/**
 * A collection of file IO utilities
 */
public enum Files {
    ;

    public static File file(File parent, String path) {
        return ProjectLayout.Utils.file(parent, path);
    }

    public static List<File> filter(File baseDir, $.F1<String, Boolean> filter) {
        ListBuilder<File> list = ListBuilder.create(500);
        filter(baseDir, filter, C.F.addTo(list));
        return list.toList();
    }

    public static void filter(File baseDir, $.F1<String, Boolean> filter, $.F1<File, ?> visitor) {
        File[] files = baseDir.listFiles();
        if (null == files) {
            return;
        }
        int n = files.length;
        for (int i = 0; i < n; ++i) {
            File file = files[i];
            if (isValidDir(file)) {
                filter(file, filter, visitor);
            } else if (null == filter || filter.apply(file.getName())) {
                visitor.apply(file);
            }
        }
    }

    private static boolean isValidDir(File file) {
        return file.isDirectory() && !isHiddenDir(file);
    }

    private static boolean isHiddenDir(File file) {
        return file.getName().startsWith(".");
    }
}
