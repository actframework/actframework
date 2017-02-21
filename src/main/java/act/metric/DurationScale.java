package act.metric;

import org.osgl.util.E;
import org.osgl.util.S;

enum DurationScale {
    ns(1),
    ms(1000L * 1000L),
    second("s", 1000L * 1000L * 1000L),
    minute("min", 1000L * 1000L * 1000L * 60L),
    hour("h", 1000L * 1000L * 1000L * 60L * 60L),
    day("d", 1000L * 1000L * 1000L * 60L * 60L * 24L),
    week(" week(s)", 1000L * 1000L * 1000L * 60L * 60L * 24L * 7L),
    mon(" month(s)", 1000L * 1000L * 1000L * 60L * 60L * 24L * 30L),
    year(" year(s)", 1000L * 1000L * 1000L * 60L * 60L * 24L * 365L)
    ;
    long value;
    String suffix;
    DurationScale(long value) {
        this.value = value;
    }
    DurationScale(String suffix, long value) {
        this.value = value;
        this.suffix = suffix;
    }
    public static String format(long ns) {
        E.illegalArgumentIf(ns < 0, "duration cannot be negative number");
        if (ns == 0) {
            return "0";
        }
        DurationScale last = DurationScale.year;
        for (DurationScale s : DurationScale.values()) {
            if (s.value <= ns) {
                last = s;
            } else {
                break;
            }
        }
        String s;
        if (last == DurationScale.ns) {
            if (ns > 1000L * 10) {
                last = DurationScale.ms;
                long l = (ns * 100L) / last.value;
                s = S.str(l).prepend("0").insert(-2, '.').toString();
            } else {
                s = S.string(ns);
            }

        } else {
            long l = ns / (last.value / 100L);
            s = S.str(l).insert(-2, '.').toString();
        }
        if (s.startsWith(".")) {
            s = S.concat("0", s);
        }
        return S.concat(s, last.suffix);
    }

    private String suffix() {
        String suffix = this.suffix;
        if (null == suffix) {
            suffix = this.name();
        }
        return suffix;
    }

}
