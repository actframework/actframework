package act.apidoc;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import act.apidoc.sampledata.NamedListProvider;
import act.app.App;
import act.app.AppServiceBase;
import act.app.event.SysEventId;
import act.job.OnSysEvent;
import act.util.SubClassFinder;

import java.util.*;

public class SampleDataProviderManager extends AppServiceBase<SampleDataProviderManager> {

    private static class Key {
        String className;
        ISampleDataCategory category;
        Locale locale;

        Key(String className, ISampleDataCategory category, Locale locale) {
            this.className = className;
            this.category = category;
            this.locale = locale;
        }

        Key(Class type, ISampleDataCategory category, Locale locale) {
            this.className = type.getName();
            this.category = category;
            this.locale = locale;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(className, key.className) &&
                    category == key.category &&
                    Objects.equals(locale, key.locale);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, category, locale);
        }

        List<Key> broaderKeys() {
            List<Key> list = new ArrayList<>();
            if (locale != null) {
                String country = locale.getCountry();
                String language = locale.getLanguage();
                String variant = locale.getVariant();
                if (null != variant) {
                    list.add(new Key(className, category, new Locale(language, country)));
                } else if (null != country) {
                    list.add(new Key(className, category, new Locale(language)));
                } else {
                    list.add(new Key(className, category, null));
                }
            }
            if (category != null) {
                list.add(new Key(className, null, locale));
            }
            return list;
        }
    }

    private Map<Key, SampleDataProvider> repo = new HashMap<>();
    private Map<Key, SampleDataProvider> repoWithBroderKey = new HashMap<>();

    public SampleDataProviderManager(App app) {
        super(app);
    }

    @Override
    protected void releaseResources() {
        repo.clear();
        repoWithBroderKey.clear();
    }

    @OnSysEvent(SysEventId.EVENT_BUS_INITIALIZED)
    public void reset() {
        releaseResources();
    }

    @SubClassFinder
    public void foundSampleDataProvider(SampleDataProvider provider) {
        Class<?> targetType = provider.targetType();
        if (targetType == NamedListProvider.class) {
            return;
        }
        if (null != targetType) {
            Key key = new Key(targetType, provider.category(), null);
            SampleData.Category c = provider.getClass().getAnnotation(SampleData.Category.class);
            if (null != c) {
                key.category = c.value();
            }
            SampleData.Locale l = provider.getClass().getAnnotation(SampleData.Locale.class);
            if (null != l) {
                String s = l.value();
                Locale locale = Locale.forLanguageTag(s);
                key.locale = locale;
            }
            repo.put(key, provider);
            for (Key k2 : key.broaderKeys()) {
                registerForBroaderKey(k2, provider);
            }
        }
    }

    private void registerForBroaderKey(Key key, SampleDataProvider provider) {
        if (!repoWithBroderKey.containsKey(key)) {
            repoWithBroderKey.put(key, provider);
        }
        for (Key k2 : key.broaderKeys()) {
            registerForBroaderKey(k2, provider);
        }
    }

    public <T> T getSampleData(ISampleDataCategory category, String fieldName, Class<T> type) {
        return getSampleData(category, fieldName, type, true);
    }

    public <T> T getSampleData(ISampleDataCategory category, String fieldName, Class<T> type, boolean useBroaderKey) {
        return getSampleData(category, fieldName, null, type, useBroaderKey);
    }

    public <T> T getSampleData(ISampleDataCategory category, String fieldName, Locale locale, Class<T> type) {
        return getSampleData(category, fieldName, locale, type, true);
    }

    public <T> T getSampleData(ISampleDataCategory category, String fieldName, Locale locale, Class<T> type, boolean useBroaderKey) {
        Key key = new Key(type, categoryOf(category, fieldName), locale);
        return getSampleData(key, useBroaderKey);
    }

    private <T> T getSampleData(Key key, boolean useBroderKey) {
        SampleDataProvider<T> provider = getProvider(key);
        if (null == provider && useBroderKey) {
            provider = getProviderFromBroderKeyRepo(key);
        }
        return null == provider ? null : provider.get();
    }

    private <T> SampleDataProvider<T> getProvider(Key key) {
        return repo.get(key);
    }

    private <T> SampleDataProvider<T> getProviderFromBroderKeyRepo(Key key) {
        SampleDataProvider<T> provider = repoWithBroderKey.get(key);
        if (null == provider) {
            List<Key> keys = key.broaderKeys();
            for (Key k2 : keys) {
                provider = getProviderFromBroderKeyRepo(k2);
                if (null != provider) {
                    return provider;
                }
            }
        }
        return provider;
    }

    static ISampleDataCategory categoryOf(ISampleDataCategory category, String name) {
        return null != category ? category : SampleDataCategory.of(name);
    }
}
