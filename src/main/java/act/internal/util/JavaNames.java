package act.internal.util;

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

import org.osgl.util.E;
import org.osgl.util.S;

/**
 * Provides utility methods relevant to Java names, e.g. package name validator etc
 */
public final class JavaNames {

    /**
     * Check if a given string is a valid java package or class name.
     *
     * This method use the technique documented in
     * [this SO question](https://stackoverflow.com/questions/13557195/how-to-check-if-string-is-a-valid-class-identifier)
     * with the following extensions:
     *
     * * if the string does not contain `.` then assume it is not a valid package or class name
     *
     * @param s
     *      the string to be checked
     * @return
     *      `true` if `s` is a valid java package or class name
     */
    public static boolean isPackageOrClassName(String s) {
        if (S.blank(s)) {
            return false;
        }
        S.List parts = S.fastSplit(s, ".");
        if (parts.size() < 2) {
            return false;
        }
        for (String part: parts) {
            if (!Character.isJavaIdentifierStart(part.charAt(0))) {
                return false;
            }
            for (int i = 1, len = part.length(); i < len; ++i) {
                if (!Character.isJavaIdentifierPart(part.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns package name of a class
     *
     * @param clazz
     *      the class
     * @return
     *      the package name of the class
     */
    public static String packageNameOf(Class<?> clazz) {
        String name = clazz.getName();
        int pos = name.lastIndexOf('.');
        E.unexpectedIf(pos < 0, "Class does not have package: " + name);
        return name.substring(0, pos);
    }

}
