package act.app.data;

import act.app.App;
import act.app.AppServiceBase;
import act.conf.AppConfig;
import act.data.DateResolver;
import act.data.JodaDateResolver;
import org.joda.time.DateTime;
import org.osgl.mvc.util.StringValueResolver;
import org.osgl.util.C;

import java.util.Date;
import java.util.Map;

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
        StringValueResolver r = resolvers.get(targetType);
        return null == r ? null : r.resolve(strVal);
    }

    private void registerPredefinedResolvers() {
        resolvers.putAll(StringValueResolver.predefined());
    }

    private void registerBuiltInResolvers(AppConfig config) {
        resolvers.put(Date.class, new DateResolver(config));
        resolvers.put(DateTime.class, new JodaDateResolver(config));
    }
}
