package act.metric;

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

enum CountScale {
    nil("", 1L),
    kilo("k", 1000L),
    million("m", 1000L * 1000L),
    billion("b", 1000L * 1000L * 1000L),
    tera("t", 1000L * 1000L * 1000L * 1000L),
    peta("p", 1000L * 1000L * 1000L * 1000L * 1000L),
    exa("e", 1000L * 1000L * 1000L * 1000L * 1000L * 1000L)
    ;
    long value;
    String suffix;
    CountScale(long value) {
        this.value = value;
    }
    CountScale(String suffix, long value) {
        this.value = value;
        this.suffix = suffix;
    }
    public static String format(long count) {
        E.illegalArgumentIf(count < 0, "counts cannot be negative number");
        if (count == 0) {
            return "0";
        }
        CountScale last = CountScale.exa;
        for (CountScale s : CountScale.values()) {
            if (s.value <= count) {
                last = s;
            } else {
                break;
            }
        }
        if (last == nil) {
            return S.string(count);
        }
        long l = count / (last.value / 100L);
        String s = S.str(l).insert(-2, '.').toString();
        if (s.endsWith(".00")) {
            s = s.substring(0, s.length() - 3);
        }
        return S.fmt("%s%s", s, last.suffix());
    }

    private String suffix() {
        String suffix = this.suffix;
        if (null == suffix) {
            suffix = this.name();
        }
        return suffix;
    }

}
