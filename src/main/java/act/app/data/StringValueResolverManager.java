package act.app.data;

import act.ActComponent;
import act.app.App;
import act.app.AppServiceBase;
import act.conf.AppConfig;
import act.data.SObjectResolver;
import org.osgl.storage.ISObject;
import org.osgl.util.AnnotationAware;
import org.osgl.util.C;
import org.osgl.util.StringValueResolver;

import java.util.Map;

@ActComponent
public class StringValueResolverManager extends AppServiceBase<StringValueResolverManager> {

    private Map<Class, StringValueResolver> resolvers = C.newMap();

    public StringValueResolverManager(App app) {
        super(app);
        registerPredefinedResolvers();
        registerBuiltInResolvers(app.config());
    }

    @Override
    protected void releaseResources() {
        resolvers.clear();
    }

    public <T> StringValueResolverManager register(Class<T> targetType, StringValueResolver<T> resolver) {
        resolvers.put(targetType, resolver);
        return this;
    }

    public Object resolve(String strVal, Class<?> targetType) {
        StringValueResolver r = resolver(targetType);
        return null != r ? r.resolve(strVal) : null;
    }

    public <T> StringValueResolver<T> resolver(final Class<T> targetType) {
        StringValueResolver<T> r = resolvers.get(targetType);
        if (null == r && Enum.class.isAssignableFrom(targetType)) {
            r = new StringValueResolver<T>(targetType) {
                @Override
                public T resolve(String value) {
                    return (T) Enum.valueOf(((Class<Enum>) targetType), value);
                }
            };
            resolvers.put(targetType, r);
        }
        return r;
    }

    public <T> StringValueResolver<T> resolver(Class<T> targetType, AnnotationAware annotationAware) {
        StringValueResolver<T> resolver = resolver(targetType);
        if (null != resolver) {
            resolver = resolver.amended(annotationAware);
        }
        return resolver;
    }

    private void registerPredefinedResolvers() {
        resolvers.putAll(StringValueResolver.predefined());
    }

    private void registerBuiltInResolvers(AppConfig config) {
// We have the StringValueResolverFinder to handle built in resolver registration
//        put(Date.class, new DateResolver(config));
//        put(LocalDate.class, new JodaLocalDateCodec(config));
//        put(LocalDateTime.class, new JodaLocalDateTimeCodec(config));
//        put(LocalTime.class, new JodaLocalTimeCodec(config));
//        put(DateTime.class, new JodaDateTimeCodec(config));
        // rebind SObjectResolver to ISObject.class in addition to SObject.class
        put(ISObject.class, SObjectResolver.INSTANCE);
    }

    private void put(Class type, StringValueResolver resolver) {
        app().registerSingleton(resolver);
        resolvers.put(type, resolver);
    }
}
