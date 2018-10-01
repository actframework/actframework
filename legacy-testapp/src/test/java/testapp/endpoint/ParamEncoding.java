package testapp.endpoint;

import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.List;
import java.util.Map;

/**
 * There are two ways to pass array/list element in GET request. For detail refers to
 * http://stackoverflow.com/questions/11889997/how-to-send-a-array-in-url-request.
 *
 * **Note** the comma separated style (first style in the accepted answer post) cannot
 * be supported correctly
 */
public enum ParamEncoding {

    /**
     * Encode for JSON body post
     */
    JSON() {
        @Override
        public List<$.T2<String, Object>> encode(String paramName, List<?> elements) {
            return C.list($.T2(paramName, (Object)elements));
        }

        @Override
        public List<$.T2<String, Object>> encode(String paramName, Map<?, ?> elements) {
            return C.list($.T2(paramName, (Object)elements));
        }
    },

    /**
     * The first style:
     * List: `name=Actor1&name=Actor2&name=Actor3`
     * Map: not supported
     */
    ONE() {
        @Override
        public List<$.T2<String, Object>> encode(final String paramName, final List<?> elements) {
            return C.list(elements).map(new $.Transformer<Object, $.T2<String, Object>>() {
                @Override
                public $.T2<String, Object> transform(Object o) {
                    return $.T2(paramName, o);
                }
            });
        }

        @Override
        public List<$.T2<String, Object>> encode(String paramName, Map<?, ?> elements) {
            throw E.unsupport();
        }
    },

    /**
     * The second style:
     * List: `name[0]=Actor1&name[1]=Actor2&name[2]=Actor3`
     * Map: `name[k1]=v1&name[k2]=v2`
     */
    TWO() {
        @Override
        public List<$.T2<String, Object>> encode(String paramName, List<?> elements) {
            List<$.T2<String, Object>> retList = C.newSizedList(elements.size());
            for (int i = 0; i < elements.size(); ++i) {
                retList.add($.T2(S.fmt("%s[%d]", paramName, i), elements.get(i)));
            }
            return retList;
        }

        @Override
        public List<$.T2<String, Object>> encode(String paramName, Map<?, ?> elements) {
            List<$.T2<String, Object>> retList = C.newList();
            if (null != elements) {
                for (Map.Entry<?, ?> entry : elements.entrySet()) {
                    retList.add($.T2(S.fmt("%s[%s]", paramName, entry.getKey().toString()), entry.getValue()));
                }
            }
            return retList;
        }
    },

    /**
     * The third style:
     * List: `name[]=Actor&name[]=Actor2&name[]=Actor3`
     * Map: not supported
     */
    THREE() {
        @Override
        public List<$.T2<String, Object>> encode(String paramName, List<?> elements) {
            List<$.T2<String, Object>> retList = C.newSizedList(elements.size());
            for (int i = 0; i < elements.size(); ++i) {
                retList.add($.T2(S.fmt("%s[]", paramName, i), elements.get(i)));
            }
            return retList;
        }

        @Override
        public List<$.T2<String, Object>> encode(String paramName, Map<?, ?> elements) {
            throw E.unsupport();
        }
    },

    /**
     * The fourth style:
     *
     * List: `name.1=Actor&name.2=Actor2&name.3=Actor3`
     * Map: `name.k1=v1&name.k2=v2`
     */
    FOUR() {
        @Override
        public List<$.T2<String, Object>> encode(String paramName, List<?> elements) {
            E.illegalStateIf(paramName.contains("."), "Param encoding four does not support param name with \".\" inside");
            List<$.T2<String, Object>> retList = C.newList();
            for (int i = 0; i < elements.size(); ++i) {
                retList.add($.T2(S.fmt("%s.%d", paramName, i), elements.get(i)));
            }
            return retList;
        }

        @Override
        public List<$.T2<String, Object>> encode(String paramName, Map<?, ?> elements) {
            E.illegalStateIf(paramName.contains("."), "Param encoding four does not support param name with \".\" inside");
            List<$.T2<String, Object>> retList = C.newList();
            if (null != elements) {
                for (Map.Entry<?, ?> entry : elements.entrySet()) {
                    retList.add($.T2(S.fmt("%s.%s", paramName, entry.getKey().toString()), entry.getValue()));
                }
            }
            return retList;
        }
    }
    ;

    /**
     * Returns the list of (k,v) pairs to be fed into GET/POST request
     *
     * @param paramName the name of the parameter
     * @param elements  the list of values
     * @return A list of (key,val) pairs to be feed into GET/POST request
     */
    public abstract List<$.T2<String, Object>> encode(String paramName, List<?> elements);

    public abstract List<$.T2<String, Object>> encode(String paramName, Map<?, ?> elements);
}
