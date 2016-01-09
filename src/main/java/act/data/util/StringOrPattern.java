package act.data.util;

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
        return isPattern() ? p.matcher(s).matches() : S.eq(s(), s);
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
