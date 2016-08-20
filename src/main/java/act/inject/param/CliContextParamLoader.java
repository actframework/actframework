package act.inject.param;

import act.app.ActionContext;
import act.app.App;
import act.app.CliContext;
import act.cli.Optional;
import act.cli.Required;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;
import org.osgl.util.E;
import org.osgl.util.StringValueResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Responsible for loading param value for {@link ActionContext}
 */
public class CliContextParamLoader extends ParamValueLoaderService {

    CliContextParamLoader(App app) {
        super(app);
    }

    public CliContext.ParsingContext buildParsingContext(Class commander, Method method) {
        CliContext.ParsingContextBuilder.start();
        ParamValueLoader loader = findBeanLoader(commander);
        classRegistry.putIfAbsent(commander, loader);
        ParamValueLoader[] loaders = findMethodParamLoaders(method);
        methodRegistry.putIfAbsent(method, loaders);
        return CliContext.ParsingContextBuilder.finish();
    }

    @Override
    protected ParamValueLoader findContextSpecificLoader(
            String bindName,
            Class rawType,
            BeanSpec spec,
            Type type,
            Annotation[] annotations
    ) {
        boolean isArray = rawType.isArray();
        StringValueResolver resolver = isArray ? resolverManager.resolver(rawType.getComponentType()) : resolverManager.resolver(rawType);
        Required required = filter(annotations, Required.class);
        Optional optional = null;
        if (null == required) {
            optional = filter(annotations, Optional.class);
        }
        if (null != required) {
            return new OptionLoader(bindName, required, resolver);
        } else if (null != optional) {
            return new OptionLoader(bindName, optional, resolver);
        }
        if (!isArray) {
            return new CliArgumentLoader(resolver);
        }
        return new CliVarArgumentLoader(rawType.getComponentType(), resolver);
    }
}
