package act.di.feather;

import act.di.loader.BeanLoaderHelper;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.List;

public class Key<T> { //implements Provider<T> {


//    final Class<T> type;
//    final Provider<T> provider;
//    final List<Annotation> annotations;
//    final List<Class> typeParameters;
//
//    private transient volatile BeanLoaderHelper beanLoader;
//
//    private Key(Class<T> type, Provider<T> provider) {
//        this.type = type;
//        this.provider = provider;
//    }
//
//    private Key(Class<T> type, List<Annotation> annotations, List<Class> typeParameters) {
//        this.type = type;
//        this.annotations = annotations;
//        this.typeParameters = typeParameters;
//    }
//
//    public T get() {
//        return provider.get();
//    }
//
//    /**
//     * @return Key for a given type
//     */
//    public static <T> Key<T> of(Class<T> type) {
//        return new Key<T>(type, null, null);
//    }
//
//    /**
//     * @return Key for a given type and provider
//     */
//    public static <T> Key<T> of(Class<T> type, Provider<T> provider) {
//        return new Key(type, provider);
//    }
//
//    /**
//     * @return Key for a given type and qualifier annotations
//     */
//    public static <T> Key<T> of(Class<T> type, List<Annotation> annotations) {
//        return new Key<T>(type, annotations, C.<Class>list());
//    }
//
//    /**
//     * @return Key for a given type, qualifier annotations and type parameters
//     */
//    public static <T> Key<T> of(Class<T> type, List<Annotation> annotations, List<Class> typeParameters) {
//        return new Key(type, annotations, typeParameters);
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Key<?> that = (Key<?>) o;
//        return $.eq(type, that.type) && $.eq2(annotations, that.annotations) && $.eq2(typeParameters, that.typeParameters);
//    }
//
//    @Override
//    public int hashCode() {
//        return $.hc(type, annotations, typeParameters);
//    }
//
//    @Override
//    public String toString() {
//        StringBuilder sb = S.builder(type);
//        if (!annotations.isEmpty()) {
//            sb.append("@").append(S.join(",", annotations));
//        }
//        if (!typeParameters.isEmpty()) {
//            sb.append("<").append(S.join(",", typeParameters)).append(">");
//        }
//        return sb.toString();
//    }


}