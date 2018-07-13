package act.i18n;

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

import act.app.App;
import act.job.OnAppStart;
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import act.util.LogSupport;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.util.Keyword;

import java.util.*;
import javax.inject.Singleton;

@Singleton
public class EnumLookupCache extends LogSupport {

    private Map<String, Class<? extends Enum>> eligibleEnums = new HashMap<>();
    private int eligibleEnumSize;

    // locale
    // -> enum type name (as keyword)
    // --> enum name
    // ---> enum message
    private Map<Locale, Map<Keyword, Map<String, String>>> withoutProperties = new HashMap<>();

    // locale
    // -> enum type name (as keyword)
    // --> enum name
    // ---> enum field name
    // ----> enum field value
    private Map<Locale, Map<Keyword, Map<String, Map<String, Object>>>> withProperties = new HashMap<>();

    public Map<Keyword, Map<String, String>> withoutProperties(Locale locale) {
        return ensureWithoutProperties(locale);
    }

    public Map<Keyword, Map<String, Map<String, Object>>> withProperties(Locale locale) {
        return ensureWithProperties(locale);
    }

    public Map<String, String> withoutProperties(String enumTypeName, Locale locale) {
        Map<Keyword, Map<String, String>> map = ensureWithoutProperties(locale);
        return map.get(Keyword.of(enumTypeName));
    }

    public Map<String, Map<String, Object>> withProperties(String enumTypeName, Locale locale) {
        Map<Keyword, Map<String, Map<String, Object>>> map = ensureWithProperties(locale);
        return map.get(Keyword.of(enumTypeName));
    }

    @OnAppStart(async = true)
    public void loadEligibleEnums(ClassInfoRepository repo, final App app) {
        final ClassNode enumRoot = repo.node(Enum.class.getName());
        final $.Predicate<String> tester = app.config().appClassTester();
        enumRoot.visitSubTree(new Lang.Visitor<ClassNode>() {
            @Override
            public void visit(ClassNode classNode) throws Lang.Break {
                String name = classNode.name();
                if (tester.test(name) && !name.startsWith("act.")) {
                    Class<? extends Enum> enumType = $.cast(app.classForName(name));
                    Object[] constants = enumType.getEnumConstants();
                    if (null != constants && constants.length > 0) {
                        String simpleName = enumType.getSimpleName();
                        Class<?> existing = eligibleEnums.get(simpleName);
                        if (null != existing && existing.getName().equals(enumType.getName())) {
                            warn("Ambiguous enum name found between %s and %s", existing.getName(), enumType.getName());
                        } else {
                            eligibleEnums.put(simpleName, enumType);
                        }
                    }
                }
            }
        });
        eligibleEnumSize = eligibleEnums.size();
    }

    private Map<Keyword, Map<String, String>> ensureWithoutProperties(Locale locale) {
        Map<Keyword, Map<String, String>> map = withoutProperties.get(locale);
        if (null == map) {
            map = new HashMap<>();
            withoutProperties.put(locale, map);
        }
        if (map.size() < eligibleEnumSize) {
            populateWithoutProperties(map, locale);
        }
        return map;
    }

    private Map<Keyword, Map<String, Map<String, Object>>> ensureWithProperties(Locale locale) {
        Map<Keyword, Map<String, Map<String, Object>>> map = withProperties.get(locale);
        if (null == map) {
            map = new HashMap<>();
            withProperties.put(locale, map);
        }
        if (map.size() < eligibleEnumSize) {
            populateWithProperties(map, locale);
        }
        return map;
    }

    private void populateWithoutProperties(Map<Keyword, Map<String, String>> withoutProperties, Locale locale) {
        for (Map.Entry<String, Class<? extends Enum>> entry: eligibleEnums.entrySet()) {
            String key = entry.getKey();
            if (!withoutProperties.containsKey(key)) {
                Map<String, String> map = $.cast(I18n.i18n(locale, entry.getValue()));
                withoutProperties.put(Keyword.of(key), map);
            }
        }
    }

    private void populateWithProperties(Map<Keyword, Map<String, Map<String, Object>>> withProperties, Locale locale) {
        for (Map.Entry<String, Class<? extends Enum>> entry: eligibleEnums.entrySet()) {
            String key = entry.getKey();
            if (!withProperties.containsKey(key)) {
                Map<String, Map<String, Object>> map = $.cast(I18n.i18n(locale, entry.getValue(), true));
                withProperties.put(Keyword.of(key), map);
            }
        }
    }
}
