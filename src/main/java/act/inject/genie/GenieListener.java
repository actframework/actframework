package act.inject.genie;
import act.inject.ActProviders;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.InjectListener;

class GenieListener extends InjectListener.Adaptor {

    private GenieInjector injector;

    GenieListener(GenieInjector injector) {
        this.injector = injector;
    }

    @Override
    public void providerRegistered(Class targetType) {
        ActProviders.addProvidedType(targetType);
    }

    @Override
    public void injected(Object bean, BeanSpec beanSpec) {
        injector.fireInjectedEvent(bean, beanSpec);
    }
}
