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
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filter noisy words from a class or package name to get
 */
class NoisyWordsFilter {

    /**
     * The key (`act.noise-words`) to fetch user defined noise words
     * from system property.
     *
     * User can define noise words in a string separated by comma, e.g.
     *
     * ```
     * "buzz,it,admin"
     * ```
     */
    static final String PROP_NOISE_WORDS = "act.noise-words";

    // built-in noise words
    private static final Set<String> NOISE_WORDS = C.set("app", "application", "demo", "entry", "main");

    private static Set<String> noiseWords() {
        Set<String> retVal = new HashSet<>(NOISE_WORDS);

        String userDefined = System.getProperty(PROP_NOISE_WORDS);
        if (null != userDefined) {
            retVal.addAll(C.listOf(userDefined.split(S.COMMON_SEP)).map(S.F.TO_LOWERCASE));
        }

        return retVal;
    }

    /**
     * Filter a list of string tokens and get rid of tokens that are
     * {@link #noiseWords() noise}
     *
     * The tokens in the return list is in lower case
     *
     * @param tokens
     *      the list of string token
     * @return
     *      filtered list of lowercase string tokens
     */
    static List<String> filter(List<String> tokens) {
        List<String> result = new ArrayList<>();
        Set<String> noise = noiseWords();
        for (String token : tokens) {
            if (S.blank(token)) {
                continue;
            }
            token = token.trim().toLowerCase();
            if (!noise.contains(token)) {
                result.add(token);
            }
        }
        return result;
    }


}
