package act.di.param;

import org.osgl.$;
import org.osgl.util.E;
import org.rythmengine.utils.S;

import java.util.Arrays;

/**
 * `ParamKey` is composed of a sequenced of String
 */
class ParamKey {

    private String[] seq;
    private int hc;
    private int size;
    private ParamKey(String[] seq) {
        this.seq = seq;
        this.size = seq.length;
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
        return obj instanceof ParamKey && Arrays.equals(((ParamKey) obj).seq, seq);
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

    String name() {
        return seq[size - 1];
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
        this.hc = $.hc(seq);
    }

    static ParamKey of(String[] seq) {
        E.illegalArgumentIf(seq.length == 0);
        return new ParamKey(seq);
    }

    static ParamKey of(String one) {
        return new ParamKey(one);
    }
}
