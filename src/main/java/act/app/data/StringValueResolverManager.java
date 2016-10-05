package act.app.data;

import act.ActComponent;
import act.app.App;
import act.app.AppServiceBase;
import act.conf.AppConfig;
import act.data.*;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.AnnotationAware;
import org.osgl.util.C;
import org.osgl.util.StringValueResolver;

import java.io.File;
import java.util.Date;
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

    public StringValueResolver resolver(final Class<?> targetType) {
        StringValueResolver r = resolvers.get(targetType);
        if (null == r && Enum.class.isAssignableFrom(targetType)) {
            r = new StringValueResolver() {
                @Override
                public Object resolve(String value) {
                    return Enum.valueOf(((Class<Enum>) targetType), value);
                }
            };
            resolvers.put(targetType, r);
        }
        return r;
    }

    public StringValueResolver resolver(Class<?> targetType, AnnotationAware annotationAware) {
        StringValueResolver resolver = resolver(targetType);
        if (null != resolver) {
            resolver = resolver.amended(annotationAware);
        }
        return resolver;
    }

    private void registerPredefinedResolvers() {
        resolvers.putAll(StringValueResolver.predefined());
    }

    private void registerBuiltInResolvers(AppConfig config) {
        put(Date.class, new DateResolver(config));
        put(LocalDate.class, new JodaLocalDateCodec(config));
        put(LocalDateTime.class, new JodaLocalDateTimeCodec(config));
        put(LocalTime.class, new JodaLocalTimeCodec(config));
        put(DateTime.class, new JodaDateTimeCodec(config));
        // rebind SObjectResolver to ISObject.class in addition to SObject.class
        put(ISObject.class, SObjectResolver.INSTANCE);
    }

    private void put(Class type, StringValueResolver resolver) {
        app().registerSingleton(resolver);
        resolvers.put(type, resolver);
    }
}
