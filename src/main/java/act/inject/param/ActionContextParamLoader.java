package act.inject.param;

import act.app.App;
import act.inject.HeaderVariable;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.util.AnnotationUtil;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.annotation.Resolve;
import org.osgl.mvc.util.Binder;
import org.osgl.util.StringValueResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Responsible for loading param value for {@link act.app.ActionContext}
 */
class ActionContextParamLoader extends ParamValueLoaderService {

    ActionContextParamLoader(App app) {
        super(app);
    }

    @Override
    protected ParamValueLoader findContextSpecificLoader(
            String bindName,
            Class rawType,
            BeanSpec spec,
            Type type,
            Annotation[] annotations
    ) {
        HeaderVariable headerVariable = filter(annotations, HeaderVariable.class);
        if (null != headerVariable) {
            return new HeaderValueLoader(headerVariable.value(), spec);
        }

        ParamValueLoader loader = null;
        {
            Bind bind = spec.getAnnotation(Bind.class);
            if (null != bind) {
                for (Class<? extends Binder> binderClass : bind.value()) {
                    Binder binder = injector.get(binderClass);
                    if (rawType.isAssignableFrom(binder.targetType())) {
                        loader = new BoundedValueLoader(binder, bindName);
                        break;
                    }
                }
            }
        }
        if (null == loader) {
            Annotation[] aa = spec.allAnnotations();
            for (Annotation a : aa) {
                Bind bind = AnnotationUtil.tagAnnotation(a, Bind.class);
                if (null != bind) {
                    for (Class<? extends Binder> binderClass : bind.value()) {
                        Binder binder = injector.get(binderClass);
                        binder.attributes($.evaluate(a));
                        if (rawType.isAssignableFrom(binder.targetType())) {
                            loader = new BoundedValueLoader(binder, bindName);
                            break;
                        }
                    }
                }
            }
        }
        if (null == loader) {
            Binder binder = binderManager.binder(rawType);
            if (null != binder) {
                loader = new BoundedValueLoader(binder, bindName);
            }
        }
        if (null == loader) {
            Resolve resolve = spec.getAnnotation(Resolve.class);
            if (null != resolve) {
                Class<? extends StringValueResolver>[] resolvers = resolve.value();
                for (Class<? extends StringValueResolver> resolverClass : resolvers) {
                    StringValueResolver resolver = injector.get(resolverClass);
                    if (rawType.isAssignableFrom(resolver.targetType())) {
                        loader = new StringValueResolverValueLoader(ParamKey.of(bindName), resolver, null, rawType);
                    }
                }
            }
        }

        if (null == loader) {
            Annotation[] aa = spec.allAnnotations();
            for (Annotation a : aa) {
                Resolve resolve = AnnotationUtil.tagAnnotation(a, Resolve.class);
                if (null != resolve) {
                    Class<? extends StringValueResolver>[] resolvers = resolve.value();
                    for (Class<? extends StringValueResolver> resolverClass : resolvers) {
                        StringValueResolver resolver = injector.get(resolverClass);
                        resolver.attributes($.evaluate(a));
                        if (rawType.isAssignableFrom(resolver.targetType())) {
                            loader = new StringValueResolverValueLoader(ParamKey.of(bindName), resolver, null, rawType);
                            break;
                        }
                    }
                }
            }
        }

        if (null == loader) {
            StringValueResolver resolver = null;
            Param param = spec.getAnnotation(Param.class);
            if (null != param) {
                Class<? extends StringValueResolver> resolverClass = param.resolverClass();
                if (Param.DEFAULT_RESOLVER.class != resolverClass) {
                    resolver = injector.get(resolverClass);
                }
            }

            if (null == resolver) {
                resolver = resolverManager.resolver(rawType, spec);
            }

            loader = (null != resolver) ? new StringValueResolverValueLoader(ParamKey.of(bindName), resolver, param, rawType) : buildLoader(ParamKey.of(bindName), type);
        }

        return loader;
    }

    @Override
    protected boolean supportJsonDecorator() {
        return true;
    }
}
