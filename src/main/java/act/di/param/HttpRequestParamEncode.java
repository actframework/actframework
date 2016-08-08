package act.di.param;

import org.osgl.util.E;

public enum HttpRequestParamEncode {
    /**
     * Example: `foo[bar][id]`
     */
    JQUERY() {
        @Override
        protected String _concat(String[] path, int len) {
            StringBuilder sb = new StringBuilder(path[0]);
            for (int i = 1; i < len; ++i) {
                sb.append("[").append(path[i]).append("]");
            }
            return sb.toString();
        }
    },

    /**
     * Example: `foo.bar.id`
     */
    DOT_NOTATION() {
        @Override
        protected String _concat(String[] path, int len) {
            StringBuilder sb = new StringBuilder(path[0]);
            for (int i = 1; i < len; ++i) {
                sb.append(".").append(path[i]);
            }
            return sb.toString();
        }
    };

    public final String concat(ParamKey key) {
        return concat(key.seq());
    }

    public final String concat(String[] path) {
        int len = path.length;
        E.illegalArgumentIf(len < 1);
        if (len == 1) {
            return path[0];
        }
        return _concat(path, len);
    }

    protected abstract String _concat(String[] path, int len);

    public static HttpRequestParamEncode next(HttpRequestParamEncode encode) {
        HttpRequestParamEncode[] all = values();
        int id = encode.ordinal();
        if (id < all.length - 1) {
            return all[id + 1];
        } else {
            return all[0];
        }
    }
}
