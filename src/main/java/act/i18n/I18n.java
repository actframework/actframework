package act.i18n;

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

import act.Act;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class I18n {

    private static final Logger logger = LogManager.get(I18n.class);

    private static final String DEF_RESOURCE_BUNDLE_NAME = "messages";

    public static Locale locale() {
        ActContext context = ActContext.Base.currentContext();
        return null != context ? context.locale(true) : Act.appConfig().locale();
    }

    public static String i18n(String msgId, Object... args) {
        return _i18n(true, locale(), DEF_RESOURCE_BUNDLE_NAME, msgId, args);
    }

    public static String i18n(boolean ignoreError, String msgId, Object... args) {
        return _i18n(ignoreError, locale(), DEF_RESOURCE_BUNDLE_NAME, msgId, args);
    }

    public static String i18n(Locale locale, String msgId, Object... args) {
        return _i18n(true, locale, DEF_RESOURCE_BUNDLE_NAME, msgId, args);
    }

    public static String i18n(boolean ignoreError, Locale locale, String msgId, Object... args) {
        return _i18n(ignoreError, locale, DEF_RESOURCE_BUNDLE_NAME, msgId, args);
    }

    public static String i18n(Class<?> bundleSpec, String msgId, Object... args) {
        return _i18n(true, locale(), bundleSpec.getName(), msgId, args);
    }

    public static String i18n(boolean ignoreError, Class<?> bundleSpec, String msgId, Object... args) {
        return _i18n(ignoreError, locale(), bundleSpec.getName(), msgId, args);
    }

    public static String i18n(Locale locale, Class<?> bundleSpec, String msgId, Object... args) {
        return _i18n(true, locale, bundleSpec.getName(), msgId, args);
    }

    public static String i18n(boolean ignoreError, Locale locale, Class<?> bundleSpec, String msgId, Object... args) {
        return _i18n(ignoreError, locale, bundleSpec.getName(), msgId, args);
    }

    private static String _i18n(boolean ignoreError, Locale locale, String bundleName, String msgId, Object... args) {
        if (null == msgId) {
            return "";
        }
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle(bundleName, $.notNull(locale), Act.app().classLoader());
        } catch (MissingResourceException e) {
            return msgId;
        }
        String msg = msgId;
        if (ignoreError) {
            if (bundle.containsKey(msgId)) {
                msg = bundle.getString(msgId);
            }
        } else {
            try {
                msg = bundle.getString(msgId);
            } catch (MissingResourceException e) {
                logger.warn("Cannot find i18n message key: %s", msgId);
            }
        }
        int len = args.length;
        if (len > 0) {
            Object[] resolvedArgs = new Object[len];
            for (int i = 0; i < len; ++i) {
                Object arg = args[i];
                if (arg instanceof String) {
                    resolvedArgs[i] = _i18n(true, locale, bundleName, (String) arg);
                } else {
                    resolvedArgs[i] = arg;
                }
            }
            MessageFormat formatter = new MessageFormat(msg, locale);
            msg = formatter.format(resolvedArgs);
        }

        return msg;
    }

    public static String i18n(Enum<?> msgId) {
        return i18n(locale(), msgId);
    }

    public static String i18n(Locale locale, Enum<?> msgId) {
        return i18n(locale, DEF_RESOURCE_BUNDLE_NAME, msgId);
    }

    public static String i18n(Class<?> bundleSpec, Enum<?> msgId) {
        return i18n(locale(), bundleSpec, msgId);
    }

    public static String i18n(Locale locale, Class<?> bundleSpec, Enum<?> msgId) {
        String key = S.newBuffer("enum.").append(msgId.getDeclaringClass().getSimpleName().toLowerCase()).append(".").append(msgId.name().toLowerCase()).toString();
        return _i18n(true, locale, bundleSpec.getName(), key);
    }

    public static Map<String, Object> i18n(Class<? extends Enum> enumClass) {
        return i18n(locale(), enumClass);
    }

    public static Map<String, Object> i18n(Locale locale, Class<? extends Enum> enumClass) {
        return i18n(locale, DEF_RESOURCE_BUNDLE_NAME, enumClass);
    }

    public static Map<String, Object> i18n(Class<?> bundleSpec, Class<? extends Enum> enumClass) {
        return i18n(locale(), bundleSpec, enumClass);
    }

    public static Map<String, Object> i18n(Locale locale, Class<?> bundleSpec, Class<? extends Enum> enumClass) {
        return i18n(locale, bundleSpec.getName(), enumClass);
    }

    public static Map<String, Object> i18n(Locale locale, String bundleName, Class<? extends Enum> enumClass) {
        return i18n(locale, bundleName, enumClass, false);
    }


    public static Map<String, Object> i18n(Class<? extends Enum> enumClass, boolean outputProperties) {
        return i18n(locale(), enumClass, outputProperties);
    }

    public static Map<String, Object> i18n(Locale locale, Class<? extends Enum> enumClass, boolean outputProperties) {
        return i18n(locale, DEF_RESOURCE_BUNDLE_NAME, enumClass, outputProperties);
    }

    public static Map<String, Object> i18n(Class<?> bundleSpec, Class<? extends Enum> enumClass, boolean outputProperties) {
        return i18n(locale(), bundleSpec, enumClass, outputProperties);
    }


    public static Map<String, Object> i18n(Locale locale, Class<?> bundleSpec, Class<? extends Enum> enumClass, boolean outputProperties) {
        return i18n(locale, bundleSpec.getName(), enumClass, outputProperties);
    }

    public static Map<String, Object> i18n(Locale locale, String bundleName, Class<? extends Enum> enumClass, boolean outputProperties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        for (Enum<?> enumInstance : enumClass.getEnumConstants()) {
            String name = enumInstance.name();
            String val = i18n(locale, bundleName, enumInstance);
            if (outputProperties) {
                Map<String, Object> values = new HashMap<>();
                map.put(name, values);
                values.put("message", val);
                Map<String, $.Function<Object, Object>> getters = enumPropertyGetters(enumClass);
                for (Map.Entry<String, $.Function<Object, Object>> entry : getters.entrySet()) {
                    values.put(entry.getKey(), entry.getValue().apply(enumInstance));
                }
            } else {
                map.put(name, val);
            }
        }
        return map;
    }

    private static Set<String> standardsAnnotationMethods = C.newSet(C.list("declaringClass", "hashCode", "toString", "ordinal", "name", "class"));
    private static ConcurrentMap<Class<? extends Enum>, Map<String, $.Function<Object, Object>>> enumPropertyGetterCache = new ConcurrentHashMap<>();

    private static Map<String, $.Function<Object, Object>> enumPropertyGetters(Class<? extends Enum> enumClass) {
        Map<String, $.Function<Object, Object>> map = enumPropertyGetterCache.get(enumClass);
        if (null == map) {
            Map<String, $.Function<Object, Object>> newMap = buildEnumPropertyGetters(enumClass);
            map = enumPropertyGetterCache.putIfAbsent(enumClass, newMap);
            if (null == map) {
                map = newMap;
            }
        }
        return map;
    }

    private static Map<String, $.Function<Object, Object>> buildEnumPropertyGetters(Class<? extends Enum> enumClass) {
        Map<String, $.Function<Object, Object>> map = new HashMap<>();
        for (final Method method : enumClass.getMethods()) {
            if (void.class == method.getReturnType() || Void.class == method.getReturnType() || Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length > 0) {
                continue;
            }
            String name = propertyName(method);
            if (standardsAnnotationMethods.contains(name)) {
                continue;
            }
            map.put(method.getName(), new Osgl.Function<Object, Object>() {
                @Override
                public Object apply(Object o) throws NotAppliedException, Osgl.Break {
                    return $.invokeVirtual(o, method);
                }
            });
        }
        return map;
    }

    private static String propertyName(Method method) {
        String name = method.getName();
        int len = name.length();
        if (len > 3 && name.startsWith("get")) {
            name = S.lowerFirst(name.substring(3));
        } else if (len > 2 && name.startsWith("is")) {
            name = S.lowerFirst(name.substring(2));
        }
        return name;
    }
}
