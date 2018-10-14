package act.util;

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

import act.app.ProjectLayout;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.ListBuilder;

import java.io.File;
import java.util.ArrayList;
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
            } else if (null == filter || filter.apply(file.getPath())) {
                visitor.apply(file);
            }
        }
    }

    public static List<File> filter(List<File> baseDirs, $.F1<String, Boolean> filter) {
        ListBuilder<File> list = ListBuilder.create(500);
        filter(baseDirs, filter, C.F.addTo(list));
        return list.toList();
    }

    public static void filter(List<File> baseDirs, $.F1<String, Boolean> filter, $.F1<File, ?> visitor) {
        List<File> files = new ArrayList<>();
        for (File baseDir : baseDirs) {
            files.addAll(C.listOf(baseDir.listFiles()));
        }
        for (File file: files) {
            if (isValidDir(file)) {
                filter(file, filter, visitor);
            } else {
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
