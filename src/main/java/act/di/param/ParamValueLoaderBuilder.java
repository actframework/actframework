package act.di.param;

import act.di.ActProviders;
import act.di.Context;
import act.di.DependencyInjector;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.util.Binder;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Build a {@link ParamValueLoader}
 */
class ParamValueLoaderBuilder {

    private Type type;
    private Annotation[] annotations;
    private DependencyInjector<?> injector;

    private boolean isContext;
    private Binder<?> binder;
    private String bindName;
    private StringValueResolver stringValueResolver;
    private Param paramAnnotation;

    @Inject
    ParamValueLoaderBuilder(Type type, Annotation[] annotations, DependencyInjector<?> injector) {
        this.injector = injector;
        this.type = type;
        this.annotations = annotations;
        Arrays.sort(annotations, ANNO_CMP);
    }

    ParamValueLoader build() {
        final Class rawType = rawType(type);
        if (ActProviders.isContextType(rawType)) {
            return new InjectParamValueLoader(type, annotations);
        }
        try {
            C.listOf(annotations)
                    .accept(CTX_DETECTOR)
                    .accept(BINDER_DETECTOR);
        } catch (Osgl.Break e) {
            // ignore
        }
        if (isContext) {
            return new InjectParamValueLoader(type, annotations);
        } else if (null != binder) {
            return new BinderParamValueLoader(binder, bindName);
        } else if (null != stringValueResolver) {
            return new StringValueResolverParamValueLoader(stringValueResolver, bindName, paramAnnotation, rawType);
        }
        throw E.tbd();
    }

    static Class rawType(Type type) {
        if(type instanceof Class) {
            return (Class) type;
        } else if(type instanceof ParameterizedType) {
            return (Class)((ParameterizedType) type).getRawType();
        } else {
            throw E.unexpected("type not recognized: %s", new Object[]{type});
        }
    }

    private $.Visitor<Annotation> CTX_DETECTOR = new $.Visitor<Annotation>() {
        @Override
        public void visit(Annotation annotation) throws Osgl.Break {
            if (Context.class == annotation.annotationType()) {
                isContext = true;
                throw $.BREAK;
            }
        }
    };

    private $.Visitor<Annotation> BINDER_DETECTOR = new $.Visitor<Annotation>() {
        @Override
        public void visit(Annotation annotation) throws Osgl.Break {
            if (Bind.class == annotation.annotationType()) {
                Bind bind = $.cast(annotation);
                binder = injector.get(bind.value());
                bindName = bind.model();
                throw $.BREAK;
            }
        }
    };

    private $.Visitor<Annotation> NAMED_DETECTOR = new $.Visitor<Annotation>() {
        @Override
        public void visit(Annotation annotation) throws Osgl.Break {
            if (Named.class == annotation.annotationType()) {
                Named named = $.cast(annotation);
                String s = named.value();
                if (S.notBlank(s)) {
                    bindName = s;
                }
            }
        }
    };

    private $.Visitor<Annotation> PARAM_DETECTOR = new $.Visitor<Annotation>() {
        @Override
        public void visit(Annotation annotation) throws Osgl.Break {
            if (Param.class == annotation.annotationType()) {
                paramAnnotation = $.cast(annotation);
                String s = paramAnnotation.value();
                if (S.notBlank(s)) {
                    bindName = s;
                }
                Class<? extends StringValueResolver> resolverClass = paramAnnotation.resolverClass();
                if (Param.DEFAULT_RESOLVER.class != resolverClass) {
                    stringValueResolver = injector.get(resolverClass);
                }
                throw $.BREAK;
            }
        }
    };

    private static $.Comparator<Annotation> ANNO_CMP = new $.Comparator<Annotation>() {
        @Override
        public int compare(Annotation o1, Annotation o2) {
            Class<? extends Annotation> c1 = o1.annotationType();
            Class<? extends Annotation> c2 = o2.annotationType();
            if (Context.class == c1) {
                return -1024;
            } else if (Context.class == c2) {
                return 1024;
            }
            if (Named.class == c1) {
                return -768;
            } else if (Named.class == c2) {
                return 768;
            }
            if (Bind.class == c1) {
                return -512;
            } else if (Bind.class == c2) {
                return 512;
            }
            if (Param.class == c1) {
                return -256;
            } else if (Param.class == c2) {
                return 256;
            }
            return 0;
        }
    };

}


