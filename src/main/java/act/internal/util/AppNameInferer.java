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

import org.osgl.util.C;
import org.osgl.util.Keyword;
import org.osgl.util.S;

import java.util.List;

/**
 * Infer app name from main class or scan package
 */
class AppNameInferer {

    /**
     * Infer app name from entry class
     *
     * @param entryClass
     *      the entry class
     * @return
     *      app name inferred from the entry class
     */
    static String from(Class<?> entryClass) {
        List<String> tokens = tokenOf(entryClass);
        return fromTokens(tokens);
    }

    /**
     * Infer app name from scan package
     *
     * @param packageName
     *      the package name
     * @return
     *      app name inferred from the package name
     */
    static String fromPackageName(String packageName) {
        List<String> tokens = tokenOf(packageName);
        return fromTokens(tokens);
    }

    private static String fromTokens(List<String> tokens) {
        List<String> filtered = NoisyWordsFilter.filter(tokens);
        String joined = S.join(".", filtered);
        return Keyword.of(joined).httpHeader();
    }

    private static List<String> tokenOf(Class<?> entryClass) {
        C.List<String> tokens = C.newList();
        tokens.addAll(classNameTokensReversed(entryClass));
        Class<?> enclosingClass = entryClass.getEnclosingClass();
        while (null != enclosingClass) {
            tokens.addAll(classNameTokensReversed(enclosingClass));
            enclosingClass = entryClass.getEnclosingClass();
        }
        String pkgName = JavaNames.packageNameOf(entryClass);
        tokens.append(S.fastSplit(pkgName, ".").reverse());
        return tokens.reverse();
    }

    private static List<String> tokenOf(String packageName) {
        return S.fastSplit(packageName, ".");
    }

    private static List<String> classNameTokensReversed(Class theClass) {
        return C.list(Keyword.of(theClass.getSimpleName()).tokens()).reverse();
    }

}
