package act.di.loader;

import act.Act;
import act.di.BeanLoader;
import act.di.BeanLoaderTag;
import org.osgl.$;
import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * A helper class to provide a list of bean instance based on
 * container's type parameter and annotation information.
 *
 * If there is no annotation, then it will check the container's
 * type parameter and use it as base class or interface to find
 * all implementations and load them as bean instances
 */
public class BeanLoaderHelper {

    private BeanLoader beanLoader;
    private Object hint;
    private Object[] options;

    public BeanLoaderHelper(Annotation[] annotations, Type[] typeParameters) {
        this.resolveAnnotations(annotations);
        if (null == beanLoader) {
            this.resolveTypeParameters(typeParameters);
        }
    }

    public List load() {
        return beanLoader.loadMultiple(hint, options);
    }

    public $.Function<?, Boolean> filter() {
        return beanLoader.filter(hint, options);
    }

    private void resolveAnnotations(Annotation[] annotations) {
        if (null == annotations || annotations.length == 0) {
            return;
        }
        for (Annotation anno : annotations) {
            Annotation[] metaAnnotations = anno.getClass().getAnnotations();
            for (Annotation metaAnno : metaAnnotations) {
                if (metaAnno instanceof BeanLoaderTag) {
                    BeanLoaderTag tag = (BeanLoaderTag) metaAnno;
                    beanLoader = Act.newInstance(tag.value());
                    Class<? extends Annotation> annoClass = anno.getClass();
                    Method[] ma = annoClass.getMethods();
                    List optionList = C.newList();
                    for (Method m: ma) {
                        if (isStandardAnnotationMethod(m)) {
                            continue;
                        }
                        if ("value".equals(m.getName())) {
                            hint = $.invokeInstanceMethod(anno, m.getName());
                        } else {
                            optionList.add($.invokeInstanceMethod(anno, m.getName()));
                        }
                    }
                    options = optionList.toArray(new Object[optionList.size()]);
                    return;
                }
            }
        }
    }

    private void resolveTypeParameters(Type[] typeParameters) {
        if (null == typeParameters || typeParameters.length == 0) {
            return;
        }
        // always choose the last one in the array as there are two possibilities:
        // 1. Collection type: there is only one element in the array
        // 2. Map: the second one is the value type
        Type effectiveType = typeParameters[typeParameters.length - 1];
        if (effectiveType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) effectiveType;
        }
    }

    private static Set<String> standards = C.newSet(C.list("equals", "hashCode", "toString", "annotationType"));

    private boolean isStandardAnnotationMethod(Method m) {
        return standards.contains(m.getName());
    }
}
