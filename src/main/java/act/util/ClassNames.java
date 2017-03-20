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
        FastStr ret = path1.afterFirst(path0);
        if (ret.startsWith("/")) {
            ret = ret.substr(1);
        }
        return ret.replace('/', '.').beforeLast('.').toString();
    }
}
