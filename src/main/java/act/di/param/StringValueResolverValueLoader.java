package act.di.param;

import act.util.ActContext;
import org.osgl.mvc.annotation.Param;
import org.osgl.util.E;
import org.osgl.util.StringValueResolver;

public class StringValueResolverValueLoader implements ParamValueLoader {

    private static final ThreadLocal<HttpRequestParamEncode> encodeShare = new ThreadLocal<>();

    private StringValueResolver<?> stringValueResolver;
    private ParamKey paramKey;
    private Object defVal;
    private HttpRequestParamEncode encode;

    public StringValueResolverValueLoader(ParamKey key, StringValueResolver<?> resolver, Param param, Class<?> type) {
        this.paramKey = key;
        this.stringValueResolver = resolver;
        this.defVal = defVal(param, type);
    }

    @Override
    public Object load(Object bean, ActContext context, boolean noDefaultValue) {
        ParamTree paramTree = ParamValueLoaderManager.paramTree();
        if (null != paramTree) {
            return load(paramTree);
        }
        HttpRequestParamEncode encode = encodeShare.get();
        if (null == encode) {
            encode = this.encode;
            if (null == encode) {
                encode = save(HttpRequestParamEncode.JQUERY);
            }
        }
        Object obj = load(context, encode);
        if (null == obj && !paramKey.isSimple()) {
            HttpRequestParamEncode encode0 = encode;
            do {
                encode0 = HttpRequestParamEncode.next(encode0);
                obj = load(context, encode0);
            } while (null == obj && encode0 != encode);
            if (null != obj) {
                save(encode0);
            }
        }
        return null == obj && !noDefaultValue ? defVal : obj;
    }

    private Object load(ActContext context, HttpRequestParamEncode encode) {
        String bindName = encode.concat(paramKey);
        String value = context.paramVal(bindName);
        return (null == value) ? null : stringValueResolver.resolve(value);
    }

    private Object load(ParamTree tree) {
        ParamTreeNode node = tree.node(paramKey);
        if (null == node) {
            return null;
        }
        if (!node.isLeaf()) {
            throw E.unexpected("Expect leaf node, found: \n%s", node.debug());
        }
        String value = node.value();
        return (null == value) ? null : stringValueResolver.resolve(value);
    }

    private HttpRequestParamEncode save(HttpRequestParamEncode encode) {
        this.encode = encode;
        encodeShare.set(encode);
        return encode;
    }

    private static Object defVal(Param param, Class<?> rawType) {
        if (boolean.class == rawType) {
            return null != param && param.defBooleanVal();
        } else if (int.class == rawType) {
            return null != param ? param.defIntVal() : 0;
        } else if (double.class == rawType) {
            return null != param ? param.defDoubleVal() : 0d;
        } else if (long.class == rawType) {
            return null != param ? param.defLongVal() : 0l;
        } else if (float.class == rawType) {
            return null != param ? param.defFloatVal() : 0f;
        } else if (char.class == rawType) {
            return null != param ? param.defCharVal() : '\0';
        } else if (byte.class == rawType) {
            return null != param ? param.defByteVal() : 0;
        } else if (short.class == rawType) {
            return null != param ? param.defShortVal() : 0;
        }
        return null;
    }
}
