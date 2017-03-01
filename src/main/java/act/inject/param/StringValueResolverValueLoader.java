package act.inject.param;

import act.util.ActContext;
import act.inject.DefaultValue;
import org.osgl.mvc.annotation.Param;
import org.osgl.util.E;
import org.osgl.util.StringValueResolver;

public class StringValueResolverValueLoader extends StringValueResolverValueLoaderBase implements ParamValueLoader {

    private static final ThreadLocal<HttpRequestParamEncode> encodeShare = new ThreadLocal<HttpRequestParamEncode>();

    private HttpRequestParamEncode encode;

    public StringValueResolverValueLoader(ParamKey key, StringValueResolver<?> resolver, Param param, DefaultValue def, Class<?> type) {
        super(key, resolver, param, def, type, false);
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        if (paramKey.isSimple()) {
            String value = context.paramVal(paramKey.name());
            Object obj = (null == value) ? null : stringValueResolver.resolve(value);
            return (null == obj) && !noDefaultValue ? defVal : obj;
        }
        ParamTree paramTree = ParamValueLoaderService.paramTree();
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
        if (null == obj) {
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

}
