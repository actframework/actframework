package act.inject.param;

import org.osgl.mvc.annotation.Param;
import org.osgl.util.E;
import org.osgl.util.StringValueResolver;

abstract class StringValueResolverValueLoaderBase implements ParamValueLoader {

    protected final StringValueResolver<?> stringValueResolver;
    protected final ParamKey paramKey;
    protected final Object defVal;

    public StringValueResolverValueLoaderBase(ParamKey key, StringValueResolver<?> resolver, Param param, Class<?> type, boolean simpleKeyOnly) {
        E.illegalArgumentIf(simpleKeyOnly && !key.isSimple());
        this.paramKey = key;
        this.stringValueResolver = resolver;
        this.defVal = defVal(param, type);
    }

    static Object defVal(Param param, Class<?> rawType) {
        if (boolean.class == rawType) {
            return null != param && param.defBooleanVal();
        } else if (int.class == rawType) {
            return null != param ? param.defIntVal() : 0;
        } else if (double.class == rawType) {
            return null != param ? param.defDoubleVal() : 0d;
        } else if (long.class == rawType) {
            return null != param ? param.defLongVal() : 0L;
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
