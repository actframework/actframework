package act.app.util;

import org.osgl.$;
import org.osgl.util.C;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * Some simple type does not have default constructor, e.g. Integer etc.
 * Let's put their factory method here.
 *
 * Note only Immutable simple type supported
 */
public class SimpleTypeInstanceFactory {
    private static Map<Class, Object> prototypes = C.map(
            Boolean.class, Boolean.FALSE,
            boolean.class, Boolean.FALSE,
            Character.class, '\0',
            char.class, '\0',
            Byte.class, 0,
            byte.class, 0,
            Short.class, 0,
            short.class, 0,
            Integer.class, 0,
            int.class, 0,
            Float.class, 0F,
            float.class, 0F,
            Long.class, 0L,
            long.class, 0L,
            Double.class, 0D,
            double.class, 0D,
            BigInteger.class, BigInteger.valueOf(0L),
            BigDecimal.class, BigDecimal.valueOf(0L),
            String.class, ""
    );

    public static <T> T newInstance(Class<T> c) {
        return $.cast(prototypes.get(c));
    }
}
