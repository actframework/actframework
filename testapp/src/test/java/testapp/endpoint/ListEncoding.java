package testapp.endpoint;

import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

/**
 * There are two ways to pass array/list element in GET request. For detail refers to
 * http://stackoverflow.com/questions/11889997/how-to-send-a-array-in-url-request.
 *
 * **Note** the comma separated style (first style in the accepted answer post) cannot
 * be supported correctly
 */
public enum ListEncoding {

    /**
     * Encode for JSON body post
     */
    JSON() {
        @Override
        public List<$.T2<String, Object>> encode(String paramName, List<?> elements) {
            return C.list($.T2(paramName, (Object)elements));
        }
    },

    /**
     * The first style:
     * `name=Actor1&name=Actor2&name=Actor3`
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
    },

    /**
     * The second style:
     * `name[0]=Actor1&name[1]=Actor2&name[2]=Actor3`
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
    },

    /**
     * The third style:
     * `name[]=Actor&name[]=Actor2&name[]=Actor3`
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
    },

    /**
     * The fourth style:
     * `name.1=Actor&name.2=Actor2&name.3=Actor3`
     */
    FOUR() {
        @Override
        public List<$.T2<String, Object>> encode(String paramName, List<?> elements) {
            List<$.T2<String, Object>> retList = C.newSizedList(elements.size());
            for (int i = 0; i < elements.size(); ++i) {
                retList.add($.T2(S.fmt("%s.%d", paramName, i), elements.get(i)));
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
}
