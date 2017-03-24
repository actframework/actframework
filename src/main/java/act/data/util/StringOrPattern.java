package act.data.util;

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

import org.osgl.util.S;

import java.util.regex.Pattern;

// stores either String or Pattern
public class StringOrPattern {
    String s;
    Pattern p;

    public StringOrPattern(String s) {
        this.s = s;
        if (s.contains("*")) {
            p = Pattern.compile(s);
        }
    }

    public boolean matches(String s) {
        return isPattern() ? this.s.startsWith(S.concat(s, "\\.")) || p.matcher(s).matches() : S.eq(s(), s);
    }

    public boolean isPattern() {
        return null != p;
    }

    public Pattern p() {
        return p;
    }

    public String s() {
        return s;
    }

}
