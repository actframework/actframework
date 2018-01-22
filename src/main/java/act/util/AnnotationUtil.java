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

import org.osgl.$;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationUtil {
    public static <T extends Annotation> T declaredAnnotation(Class c, Class<T> annoClass) {
        Annotation[] aa = c.getDeclaredAnnotations();
        if (null == aa) {
            return null;
        }
        for (Annotation a : aa) {
            if (annoClass.isInstance(a)) {
                return (T) a;
            }
        }
        return null;
    }

    /**
     * Returns the {@link Annotation} tagged on another annotation instance
     * @param annotation the annotation instance
     * @param tagClass the expected annotation class
     * @param <T> the generic type of the expected annotation
     * @return the annotation tagged on annotation of type `tagClass`
     */
    public static <T extends Annotation> T annotation(Annotation annotation, Class<T> tagClass) {
        Class<?> c = annotation.annotationType();
        return c.getAnnotation(tagClass);
    }

    /**
     * **Note** this method is deprecated. Please use {@link Annotation#annotationType} instead
     *
     * Returns the class of an annotation instance
     * @param annotation the annotation instance
     * @param <T> the generic type of the annotation
     * @return the real annotation class
     */
    @Deprecated
    public static <T extends Annotation> Class<T> classOf(Annotation annotation) {
        return (Class<T>) annotation.annotationType();
    }

    public static <T extends Annotation> T getAnnotation(Class<?> targetClass, Class<T> annotationClass) {
        if (Object.class == targetClass) {
            return null;
        }
        T annotation = targetClass.getAnnotation(annotationClass);
        if (null != annotation) {
            return annotation;
        }
        targetClass = targetClass.getSuperclass();
        return null != targetClass ? getAnnotation(targetClass, annotationClass) : null;
    }

    public static Object defaultValue(Class<? extends Annotation> annoType, String methodName) {
        Method method = $.getMethod(annoType, methodName);
        return null == method ? null : method.getDefaultValue();
    }

    public static Object tryGetDefaultValue(Class<? extends Annotation> annoType, String methodName) {
        try {
            return defaultValue(annoType, methodName);
        } catch (Exception e) {
            // ignore it
            return null;
        }
    }
}
