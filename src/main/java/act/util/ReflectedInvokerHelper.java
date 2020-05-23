package act.util;

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
import act.apidoc.ApiManager;
import act.app.*;
import act.cli.Command;
import act.inject.util.LoadConfig;
import act.inject.util.LoadResource;
import act.job.JobContext;
import act.route.RouteSource;
import act.route.RoutedContext;
import org.osgl.$;
import org.osgl.inject.annotation.Configuration;
import org.osgl.mvc.annotation.*;
import org.osgl.util.C;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.*;
import java.util.*;
import javax.inject.Singleton;

public class ReflectedInvokerHelper {

    public static void classInit(App app) {
        requestHandlerMethodParamAnnotationCache = new IdentityHashMap<>();
    }

    /**
     * If the `invokerClass` specified is singleton, or without field or all fields are
     * stateless, then return an instance of the invoker class. Otherwise, return null
     * @param invokerClass the invoker class
     * @param app the app
     * @return an instance of the invokerClass or `null` if invoker class is stateful class
     */
    public static Object tryGetSingleton(Class<?> invokerClass, App app) {
        Object singleton = app.singleton(invokerClass);
        if (null == singleton) {
            if (isGlobalOrStateless(invokerClass, new HashSet<Class>())) {
                singleton = app.getInstance(invokerClass);
            }
        }
        if (null != singleton) {
            app.registerSingleton(singleton);
        }
        return singleton;
    }

    private static Set<Class<? extends Annotation>> ACTION_ANNO_TYPES = C.set(
            Action.class, GetAction.class, PostAction.class, PutAction.class,
            DeleteAction.class, PatchAction.class, WsAction.class,
            Catch.class, Before.class, After.class, Finally.class,
            Command.class
    );

    private static Map<Method, Annotation[][]> requestHandlerMethodParamAnnotationCache;

    public static Annotation[][] requestHandlerMethodParamAnnotations(Method method) {
        if (!ApiManager.inProgress() && null == ActionContext.current()) {
            return method.getParameterAnnotations();
        }
        Annotation[][] paramAnnotations = requestHandlerMethodParamAnnotationCache.get(method);
        if (null == paramAnnotations) {
            boolean searchForOverwrittenMethod = false;
            RoutedContext ctx = RoutedContext.Current.get();
            if (null != ctx) {
                searchForOverwrittenMethod = RouteSource.ACTION_ANNOTATION == ctx.routeSource() && !hasActionAnnotation(method);
            } else {
                E.illegalStateIf(null == JobContext.current(), "Unable to detected current RoutedContext");
            }
            if (searchForOverwrittenMethod) {
                Method overwrittenMethod = overwrittenMethodOf(method);
                paramAnnotations = overwrittenMethod == method ? method.getParameterAnnotations() : requestHandlerMethodParamAnnotations(overwrittenMethod);
            } else {
                paramAnnotations = method.getParameterAnnotations();
            }
            requestHandlerMethodParamAnnotationCache.put(method, paramAnnotations);
        }
        return paramAnnotations;
    }

    public static Method overwrittenMethodOf(Method method) {
        Class host = method.getDeclaringClass();
        Class base = host.getSuperclass();
        try {
            return base.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw E.unexpected("Unable to find the overwritten method of " + method);
        }
    }

    private static boolean hasActionAnnotation(Method method) {
        Annotation[] aa = method.getDeclaredAnnotations();
        for (Annotation a: aa) {
            if (ACTION_ANNO_TYPES.contains(a.annotationType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGlobalOrStateless(Class type) {
        return isGlobalOrStateless(type, new HashSet<Class>());
    }

    public static boolean isGlobalOrStateless(Field field) {
        return isGlobalOrStateless(field, new HashSet<Class>());
    }

    public static <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationClass, Method method) {
        return null != getAnnotation(annotationClass, method);
    }

    public static <T extends Annotation> T getAnnotation(Class<T> annotationClass, Method method) {
        T anno = method.getAnnotation(annotationClass);
        if (null != anno) {
            return anno;
        }
        if (!annotationClass.isAnnotationPresent(Inherited.class)) {
            return null;
        }
        Method overridenMethod = getOverridenMethod(method);
        return null == overridenMethod ? null : getAnnotation(annotationClass, overridenMethod);
    }

    private static Method getOverridenMethod(Method method) {
        Class<?> host = method.getDeclaringClass();
        host = host.getSuperclass();
        if (null == host || Object.class == host) {
            return null;
        }
        String name = method.getName();
        Class<?>[] params = method.getParameterTypes();
        return $.getMethod(host, name, params);
    }

    private static boolean isGlobalOrStateless(Class type, Set<Class> circularReferenceDetector) {
        if ($.isSimpleType(type)) {
            return false;
        }
        if (type.getAnnotation(Stateful.class) != null) {
            return false;
        }
        if (Act.app().isSingleton(type) || AppServiceBase.class.isAssignableFrom(type) || _hasGlobalOrStatelessAnnotations(type)) {
            return true;
        }
        // when type is interface or abstract class we need to infer it as stateful
        // as we can't determine the implementation class
        if ((Modifier.isAbstract(type.getModifiers()) || type.isInterface()) && !isStatelessType(type)) {
            return false;
        }
        if (circularReferenceDetector.contains(type)) {
            return false;
        }
        circularReferenceDetector.add(type);
        try {
            return _isGlobalOrStateless(type, circularReferenceDetector);
        } finally {
            circularReferenceDetector.remove(type);
        }
    }


    private static List<String> statelessTypeTraits = C.list("Logger", "CacheService");
    private static boolean isStatelessType(Class<?> intfType) {
        String typeName = intfType.getName();
        for (String s : statelessTypeTraits) {
            if (typeName.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private static $.Predicate<Class<?>> STATEFUL_CLASS = new $.Predicate<Class<?>>() {
        @Override
        public boolean test(Class<?> aClass) {
            return !_hasGlobalOrStatelessAnnotations(aClass);
        }
    };

    private static $.Predicate<Field> NON_STATIC_FIELD = new $.Predicate<Field>() {
        @Override
        public boolean test(Field field) {
            return !Modifier.isStatic(field.getModifiers());
        }
    };

    private static boolean _isGlobalOrStateless(Class type, Set<Class> circularReferenceDetector) {
        List<Field> fields = $.fieldsOf(type, STATEFUL_CLASS, NON_STATIC_FIELD);
        if (fields.isEmpty()) {
            return true;
        }
        for (Field field : fields) {
            if (Modifier.isFinal(field.getModifiers())) {
                // suppose final field
                continue;
            }
            if (!Modifier.isFinal(field.getModifiers()) && !isGlobalOrStateless(field, circularReferenceDetector)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isGlobalOrStateless(Field field, Set<Class> circularReferenceDetector) {
        if (_hasGlobalOrStatelessAnnotations(field)) {
            return true;
        }
        Class<?> fieldType = field.getType();
        return isGlobalOrStateless(fieldType, circularReferenceDetector);
    }

    private final static List<Class<? extends Annotation>> statelessMarkersForClass = C.list(
            Singleton.class, Stateless.class, InheritedStateless.class
    );

    private final static List<Class<? extends Annotation>> statelessMarkersForFields = C.list(
            Stateless.class, Global.class, Configuration.class,
            LoadResource.class, LoadConfig.class
    );

    private static boolean _hasGlobalOrStatelessAnnotations(Class<?> type) {
        return _hasAnnotations(type, statelessMarkersForClass);
    }

    private static boolean _hasGlobalOrStatelessAnnotations(Field field) {
        return _hasAnnotations(field, statelessMarkersForFields);
    }

    private static boolean _hasAnnotations(AnnotatedElement element, List<Class<? extends Annotation>> annotations) {
        for (Class<? extends Annotation> type : annotations) {
            if (null != element.getAnnotation(type)) {
                return true;
            }
        }
        return false;
    }

}
