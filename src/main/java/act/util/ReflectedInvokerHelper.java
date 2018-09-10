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
import act.inject.util.LoadResource;
import org.osgl.$;
import org.osgl.inject.annotation.Configuration;
import org.osgl.mvc.annotation.*;
import org.osgl.util.C;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
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
            if (_isGlobalOrStateless(invokerClass, new HashSet<Class>())) {
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
            Catch.class, Before.class, After.class, Finally.class
    );

    private static Map<Method, Annotation[][]> requestHandlerMethodParamAnnotationCache;

    public static Annotation[][] requestHandlerMethodParamAnnotations(Method method) {
        if (!ApiManager.inProgress() && null == ActionContext.current()) {
            return method.getParameterAnnotations();
        }
        Annotation[][] paramAnnotations = requestHandlerMethodParamAnnotationCache.get(method);
        if (null == paramAnnotations) {
            if (!hasActionAnnotation(method)) {
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
            if (ApiManager.inProgress()) {
                return method;
            }
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

    private static boolean isGlobalOrStateless(Class type, Set<Class> circularReferenceDetector) {
        if ($.isSimpleType(type)) {
            return false;
        }
        if (Act.app().isSingleton(type) || AppServiceBase.class.isAssignableFrom(type) || _hasGlobalOrStatelessAnnotations(type)) {
            return true;
        }
        if (circularReferenceDetector.contains(type)) {
            return false;
        }
        circularReferenceDetector.add(type);
        return _isGlobalOrStateless(type, circularReferenceDetector);
    }

    private static boolean _isGlobalOrStateless(Class type, Set<Class> circularReferenceDetector) {
        List<Field> fields = $.fieldsOf(type);
        if (fields.isEmpty()) {
            return true;
        }
        for (Field field : fields) {
            if (!isGlobalOrStateless(field, circularReferenceDetector)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isGlobalOrStateless(Field field, Set<Class> circularReferenceDetector) {
        Class<?> fieldType = field.getType();
        if (_hasGlobalOrStatelessAnnotations(field)) {
            return true;
        }
        return isGlobalOrStateless(fieldType, circularReferenceDetector);
    }

    private final static List<Class<? extends Annotation>> statelessMarkers = C.list(Singleton.class, Stateless.class, Global.class, Configuration.class, LoadResource.class);

    private static boolean _hasGlobalOrStatelessAnnotations(AnnotatedElement element) {
        for (Class<? extends Annotation> type : statelessMarkers) {
            if (null != element.getAnnotation(type)) {
                return true;
            }
        }
        return false;
    }

}
