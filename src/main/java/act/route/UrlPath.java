package act.route;

import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

/**
 * Encapsulate the logic to compare an incoming URL path with a route entry path.
 * <p>
 *     Note we can't do simple String match as route entry path might contains the dynamic part
 * </p>
 */
class UrlPath {

    static final String DYNA_PART = "";

    private List<String> parts = C.newList();

    UrlPath(CharSequence path) {
        String s = path.toString();
        String[] sa = s.split("/");
        for (String item: sa) {
            if (S.notBlank(item)) {
                if (item.startsWith("{") || item.contains(":")) {
                    item = DYNA_PART;
                }
                parts.add(item);
            }
        }
    }

    boolean matches(CharSequence path) {
        return equals(new UrlPath(path));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof UrlPath) {
            UrlPath that = (UrlPath) obj;
            if (parts.size() != that.parts.size()) {
                return false;
            }
            for (int i = parts.size() - 1; i >= 0; --i) {
                if (!matches(parts.get(i), that.parts.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean matches(CharSequence cs1, CharSequence cs2) {
        if (DYNA_PART.equals(cs1)) {
            return true;
        }
        int len = cs1.length();
        if (len != cs2.length()) {
            return false;
        }
        for (int i = len - 1; i >= 0; --i) {
            if (cs1.charAt(i) != cs2.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
