package act.app.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.osgl.$;
import org.osgl.util.C;

import java.lang.reflect.Type;
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
    private static Map<Class, Object> prototypes = C.Map(
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

    public static boolean isSimpleType(Type type) {
        return prototypes.containsKey(type);
    }

    public static <T> T newInstance(Class<T> c) {
        return $.cast(prototypes.get(c));
    }
}
