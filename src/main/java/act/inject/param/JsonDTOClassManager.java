package act.inject.param;

import act.app.App;
import act.app.AppClassLoader;
import act.app.AppServiceBase;
import act.inject.DependencyInjector;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JsonDTOClassManager extends AppServiceBase<JsonDTOClassManager> {

    static class DynamicClassLoader extends ClassLoader {
        private DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    private ConcurrentMap<String, Class<? extends JsonDTO>> dtoClasses = new ConcurrentHashMap<>();

    private DependencyInjector<?> injector;
    private DynamicClassLoader dynamicClassLoader;


    public JsonDTOClassManager(App app) {
        super(app);
        this.injector = app.injector();
        this.dynamicClassLoader = new DynamicClassLoader(app.classLoader());
    }

    @Override
    protected void releaseResources() {

    }

    public Class<? extends JsonDTO> get(Class<?> host, Method method) {
        List<BeanSpec> beanSpecs = beanSpecs(host, method);
        String key = key(beanSpecs);
        if (S.blank(key)) {
            return null;
        }
        Class<? extends JsonDTO> c = dtoClasses.get(key);
        if (null == c) {
            c = generate(key, beanSpecs);
            dtoClasses.putIfAbsent(key, c);
        }
        return c;
    }

    private Class<? extends JsonDTO> generate(String name, List<BeanSpec> beanSpecs) {
        return new JsonDTOClassGenerator(name, beanSpecs, dynamicClassLoader).generate();
    }

    public List<BeanSpec> beanSpecs(Class<?> host, Method method) {
        List<BeanSpec> list = new ArrayList<>();
        extractBeanSpec(list, $.fieldsOf(host, true));
        extractBeanSpec(list, method);
        Collections.sort(list, CMP);
        return list;
    }

    private void extractBeanSpec(List<BeanSpec> beanSpecs, List<Field> fields) {
        for (Field field : fields) {
            beanSpecs.add(BeanSpec.of(field.getDeclaringClass(), field.getDeclaredAnnotations(), field.getName(), injector));
        }
    }

    private void extractBeanSpec(List<BeanSpec> beanSpecs, Method method) {
        Type[] paramTypes = method.getGenericParameterTypes();
        int sz = paramTypes.length;
        if (0 == sz) {
            return;
        }
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < sz; ++i) {
            Type type = paramTypes[i];
            Annotation[] anno = annotations[i];
            beanSpecs.add(BeanSpec.of(type, anno, injector));
        }
    }

    private static final Comparator<BeanSpec> CMP = new Comparator<BeanSpec>() {
        @Override
        public int compare(BeanSpec o1, BeanSpec o2) {
            return o1.name().compareTo(o2.name());
        }
    };

    private static String key(List<BeanSpec> beanSpecs) {
        StringBuilder sb = new StringBuilder();
        for (BeanSpec beanSpec : beanSpecs) {
            sb.append(beanSpec.name()).append(beanSpec.type().hashCode());
        }
        return sb.toString();
    }

}
