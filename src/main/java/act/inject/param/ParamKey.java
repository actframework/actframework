package act.inject.param;

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

import act.Act;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.Keyword;
import org.rythmengine.utils.S;

import java.util.Arrays;

/**
 * `ParamKey` is composed of a sequenced of String
 */
class ParamKey {

    static final ParamKey ROOT_KEY = new ParamKey(new String[0]);

    private String[] seq;
    private Keyword[] keywordSeq;
    private int hc;
    private int size;
    private ParamKey(String[] seq) {
        this.seq = seq;
        this.size = seq.length;
        if (Act.appConfig().paramBindingKeywordMatching()) {
            keywordSeq = new Keyword[seq.length];
            for (int i = 0; i < seq.length; ++i) {
                keywordSeq[i] = Keyword.of(seq[i]);
            }
        }
        calcHashCode();
    }
    private ParamKey(String one) {
        this.seq = new String[]{one};
        this.size = 1;
        calcHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ParamKey) {
            ParamKey that = (ParamKey) obj;
            if (null != keywordSeq) {
                return Arrays.equals(keywordSeq, that.keywordSeq);
            } else {
                return Arrays.equals(seq, that.seq);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hc;
    }

    @Override
    public String toString() {
        return S.join(".", seq);
    }

    String[] seq() {
        return seq;
    }

    int size() {
        return size;
    }

    boolean isSimple() {
        return size == 1;
    }

    String name() {
        return seq[size - 1];
    }

    ParamKey withoutNamespace() {
        if (1 == size) {
            return null;
        }
        String[] sa = new String[size - 1];
        System.arraycopy(seq, 1, sa, 0, size - 1);
        return ParamKey.of(sa);
    }

    ParamKey parent() {
        if (1 == size) {
            return null;
        }
        String[] sa = new String[size - 1];
        System.arraycopy(seq, 0, sa, 0, size - 1);
        return ParamKey.of(sa);
    }

    ParamKey child(String name) {
        String[] sa = $.concat(seq, name);
        return ParamKey.of(sa);
    }

    private void calcHashCode() {
        this.hc = null == keywordSeq ? $.hc(seq) : $.hc(keywordSeq);
    }

    static ParamKey of(String[] seq) {
        E.illegalArgumentIf(seq.length == 0);
        return new ParamKey(seq);
    }

    static ParamKey of(String one) {
        return new ParamKey(one);
    }
}
