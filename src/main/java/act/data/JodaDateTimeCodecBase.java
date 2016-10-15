package act.data;

import org.osgl.util.StringValueResolver;
import org.osgl.util.ValueObject;

public abstract class JodaDateTimeCodecBase<T> extends StringValueResolver<T> implements ValueObject.Codec<T> {

    public static boolean isIsoStandard(String pattern) {
        return null == pattern || pattern.contains("iso") || pattern.contains("ISO") || pattern.contains("8601");
    }

    @Override
    public Class<T> targetClass() {
        return super.targetType();
    }

}
