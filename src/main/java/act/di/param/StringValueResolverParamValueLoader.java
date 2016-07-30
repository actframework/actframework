package act.di.param;

import act.util.ActContext;
import org.osgl.mvc.annotation.Param;
import org.osgl.util.StringValueResolver;

import java.lang.reflect.Type;

class StringValueResolverParamValueLoader implements ParamValueLoader {

    private StringValueResolver<?> stringValueResolver;
    private String bindName;
    private Object defVal;

    StringValueResolverParamValueLoader(StringValueResolver<?> resolver, String name, Param param, Type type) {
        this.stringValueResolver = resolver;
        this.bindName = name;
        this.defVal = defVal(param, type);
    }

    @Override
    public Object load(ActContext context) {
        String value = context.paramVal(bindName);
        if (null == value && null != defVal) {
            return defVal;
        }
        return stringValueResolver.resolve(value);
    }

    private static Object defVal(Param param, Class<?> rawType) {
        if (boolean.class == rawType) {
            return param.defBooleanVal();
        } else if (int.class == rawType) {
            return param.defIntVal();
        } else if (double.class == rawType) {
            return param.defDoubleVal();
        } else if (long.class == rawType) {
            return param.defLongVal();
        } else if (float.class == rawType) {
            return param.defFloatVal();
        } else if (char.class == rawType) {
            return param.defCharVal();
        } else if (byte.class == rawType) {
            return param.defByteVal();
        } else if (short.class == rawType) {
            return param.defShortVal();
        }
        return null;
    }
}
