package act.inject.param;

import act.app.App;
import org.osgl.inject.BeanSpec;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class JobContextParamLoader extends ParamValueLoaderService {

    public JobContextParamLoader(App app) {
        super(app);
    }

    @Override
    protected ParamValueLoader findContextSpecificLoader(String bindName, Class rawType, BeanSpec spec, Type type, Annotation[] annotations) {
        return null;
    }
}
