package act.inject.param;

import act.app.App;
import act.inject.HeaderVariable;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
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

        ParamValueLoader loader;
        Bind bind = filter(annotations, Bind.class);
        if (null != bind) {
            Binder binder = injector.get(bind.value());
            loader = new BoundedValueLoader(binder, bindName);
        } else {
            Binder binder = binderManager.binder(rawType);
            if (null != binder) {
                loader = new BoundedValueLoader(binder, bindName);
            } else {
                Param param = filter(annotations, Param.class);
                StringValueResolver resolver = null;
                if (null != param) {
                    Class<? extends StringValueResolver> resolverClass = param.resolverClass();
                    if (Param.DEFAULT_RESOLVER.class != resolverClass) {
                        resolver = injector.get(resolverClass);
                    }
                }
                if (null == resolver) {
                    resolver = resolverManager.resolver(rawType);
                }
                loader = (null != resolver) ? new StringValueResolverValueLoader(ParamKey.of(bindName), resolver, param, rawType) : buildLoader(ParamKey.of(bindName), type);
            }
        }
        return loader;
    }

    @Override
    protected boolean supportJsonDecorator() {
        return true;
    }
}
