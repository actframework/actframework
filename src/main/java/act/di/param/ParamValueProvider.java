package act.di.param;

import act.app.App;
import act.di.DependencyInjector;
import act.util.ActContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Provides parameter values for a given method
 */
public class ParamValueProvider {

    private Method method;
    private ParamValueLoader[] loaders;
    private int len;
    protected final DependencyInjector<?> injector;


    public ParamValueProvider(Method method) {
        this.method = method;
        this.injector = App.instance().injector();
        this.buildLoaders();
    }

    public Object[] provide() {
        Object[] retVal = new Object[len];
        if (0 == len) {
            return retVal;
        }
        ActContext context = injector.get(ActContext.class);
        for (int i = 0; i < len; ++i) {
            retVal[i] = loaders[i].load(context);
        }
        return retVal;
    }


    private void buildLoaders() {
        Type[] paramTypes = method.getGenericParameterTypes();
        len = paramTypes.length;
        loaders = new ParamValueLoader[len];
        if (0 == len) {
            return;
        }
        Annotation[][] paramAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < len; ++i) {
            loaders[i] = new ParamValueLoaderBuilder(paramTypes[i], paramAnnotations[i], injector).build();
        }
    }

}
