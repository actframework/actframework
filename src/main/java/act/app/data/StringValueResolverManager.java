package act.app.data;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.app.App;
import act.app.AppServiceBase;
import act.conf.AppConfig;
import act.data.SObjectResolver;
import org.osgl.$;
import org.osgl.storage.ISObject;
import org.osgl.util.AnnotationAware;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringValueResolverManager extends AppServiceBase<StringValueResolverManager> {

    private Map<Class, StringValueResolver> resolvers = new HashMap<>();

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

    public <T> T resolve(String strVal, Class<T> targetType) {
        StringValueResolver<T> r = resolver(targetType);
        return null != r ? r.resolve(strVal) : null;
    }

    public <T extends Collection> StringValueResolver<T> collectionResolver(final Class<T> targetType, final Class<?> elementType, final char separator) {
        final StringValueResolver elementResolver = resolver(elementType);
        return new StringValueResolver<T>(targetType) {
            @Override
            public T resolve(String value) {
                T col = app().getInstance(targetType);
                List<String> parts = S.fastSplit(value, String.valueOf(separator));
                for (String part : parts) {
                    col.add(elementResolver.resolve(part));
                }
                return col;
            }
        };
    }

    public <T extends Collection> StringValueResolver<T> collectionResolver(final Class<T> targetType, final Class<?> elementType, final String separator) {
        final StringValueResolver elementResolver = resolver(elementType);
        return new StringValueResolver<T>(targetType) {
            @Override
            public T resolve(String value) {
                T col = app().getInstance(targetType);
                String[] parts = value.split(separator);
                for (String part : parts) {
                    col.add(elementResolver.resolve(part));
                }
                return col;
            }
        };
    }


    public <T> StringValueResolver<T> resolver(final Class<T> targetType) {
        StringValueResolver<T> r = resolvers.get(targetType);
        final Class<? extends Enum> clazz = $.cast(targetType);
        if (null == r && Enum.class.isAssignableFrom(targetType)) {
            if (app().config().enumResolvingCaseSensitive()) {
                r = new StringValueResolver<T>(targetType) {
                    @Override
                    public T resolve(String value) {
                        //TODO should we handle exception here?
                        return (T)Enum.valueOf(clazz, value);
                    }
                };
            } else {
                r = new StringValueResolver<T>(targetType) {
                    @Override
                    public T resolve(String value) {
                        return (T) $.asEnum(clazz, value);
                    }
                };
            }
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
