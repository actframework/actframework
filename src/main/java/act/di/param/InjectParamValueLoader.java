package act.di.param;

import act.app.App;
import act.di.DependencyInjector;
import act.util.ActContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Provide param value via DI
 */
public class InjectParamValueLoader implements ParamValueLoader {

    private Type type;
    private Annotation[] annotations;
    private DependencyInjector injector;

    public InjectParamValueLoader(Type paramType, Annotation[] paramAnnotations) {
        this.type = paramType;
        this.annotations = paramAnnotations;
        this.injector = App.instance().injector();
    }

    @Override
    public Object load(ActContext context) {
        return injector.get(type, annotations);
    }

}
