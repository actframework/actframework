package act.test.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2018 ActFramework
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

import act.Act;
import act.app.App;
import act.app.DaoLocator;
import act.conf.AppConfig;
import act.db.Dao;
import act.test.Test;
import act.util.LogSupport;
import com.alibaba.fastjson.*;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.OsglConfig;
import org.osgl.exception.UnexpectedException;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;
import org.yaml.snakeyaml.Yaml;

import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlLoader extends LogSupport {

    static Pattern keyPattern = Pattern.compile("([^(]+)\\(([^)]+)\\)");

    private List<String> modelPackages = new ArrayList<>();

    private String fixtureFolder = "/test/fixtures/";

    private ClassLoader classLoader = appClassLoader();

    public YamlLoader() {
        resetModelPackages();
        loadConfig();
    }

    /**
     * Add model packages to the loader.
     *
     * The model package can be used to concat with the type string when it
     * does not contains `.`.
     *
     * For example, given the following yaml file:
     *
     * ```yaml
     * User(tom)
     * name: Tom
     * ```
     *
     * The model type `User` does not contains `.`, so the loader assume it
     * need a model package. Assume it has the following model packages set:
     *
     * * `com.xprj.util`
     * * `com.xprj.model`
     *
     * Then it will try to use the above packages to search for the model. Firstly it
     * will try `com.xprj.util.User`, if it cannot load class, then it will try `com.xprj.model.User`.
     *
     * If both `come.xprj.util` and `com.xprj.model` has `User` class defined, then it will
     * use the first one. To specify the User in the second package, use full notation in your
     * yaml file:
     *
     * ```yaml
     * com.xproj.model.User(tom)
     * name: Tom
     * ```
     *
     * @param modelPackage
     * @param modelPackages
     */
    protected YamlLoader(String modelPackage, String... modelPackages) {
        resetModelPackages();
        loadConfig();
        addModelPackages(modelPackage, modelPackages);
    }

    private void addModelPackage(String packageSpec) {
        for (String pkg : S.fastSplit(packageSpec, ",")) {
            pkg = pkg.trim();
            if (S.empty(pkg)) {
                continue;
            }
            this.modelPackages.add(S.ensure(pkg).endWith("."));
        }
    }


    public Map<String, Object> loadFixture(String fixtureName, DaoLocator daoLocator) {
        boolean isJson = fixtureName.endsWith(".json");
        boolean isYaml = !isJson && (fixtureName.endsWith(".yaml") || fixtureName.endsWith(".yml"));
        E.unsupportedIfNot(isJson || isYaml, "fixture resource file type not supported: " + fixtureName);
        String content = getResourceAsString(fixtureName);
        if (null == content) {
            return C.Map();
        }
        return isJson ? parseJson(content, daoLocator) : parse(content, daoLocator);
    }

    private Map<String, Object> parseJson(String content, DaoLocator daoLocator) {
        if (S.blank(content)) {
            return C.Map();
        }
        if (content.startsWith("[")) {
            // MongoDB exported data list variation
            JSONArray array = JSON.parseArray(content);
            int len = array.size();
            Map<String, Object> retVal = new LinkedHashMap<>();
            for (int i = 0; i < len; ++i) {
                JSONObject obj = array.getJSONObject(i);
                String className = obj.getString("className");
                E.unsupportedIf(S.isBlank(className), "Unsupported JSON resource, className required");
                String key = obj.getString("key");
                if (null == key) {
                    key = obj.getString("name");
                }
                if (null == key) {
                    key = obj.getString("id");
                }
                if (null == key) {
                    key = S.mediumRandom();
                }
                Class<?> modelType = loadModelType(className);
                Dao dao = null == daoLocator ? null : daoLocator.dao(modelType);
                Object entity = OsglConfig.INSTANCE_FACTORY.apply(modelType);
                $.map(obj).to(entity);
                if (null != dao) {
                    TxScope.enter();
                    try {
                        dao.save(entity);
                        TxScope.commit();
                    } catch (Exception e) {
                        TxScope.rollback(e);
                    } finally {
                        TxScope.clear();
                    }
                }
                retVal.put(key, entity);
            }
            return retVal;
        } else {
            JSONObject obj = JSON.parseObject(content);
            return resolve((Map) obj, daoLocator);
        }
    }

    /**
     * Read the data YAML file and returns List of model objects mapped to their class names
     *
     * @param yaml
     *         the yaml content
     * @return the loaded data mapped to name
     */
    public Map<String, Object> parse(String yaml, DaoLocator daoLocator) {
        if (S.blank(yaml)) {
            return C.Map();
        }
        Object o = new Yaml().load(yaml);
        if (null == o) {
            return C.Map();
        }
        Map<Object, Map<?, ?>> objects = $.cast(o);
        return resolve(objects, daoLocator);
    }

    private Map<String, Object> resolve(Map<Object, Map<?, ?>> objects, DaoLocator daoLocator) {
        resolveConstants(objects);
        Map<String, Map<String, Object>> mapCache = C.newMap();
        Map<String, Object> entityCache = new LinkedHashMap<>();
        Map<String, Class> classCache = C.newMap();
        Map<String, AtomicInteger> nameCounters = C.newMap();
        for (Object key : objects.keySet()) {
            String keyStr = key.toString().trim();
            if (!keyStr.contains("(")) {
                String type = keyStr.contains(".") ? S.cut(keyStr).afterLast(".") : keyStr;
                type = S.camelCase(type);
                AtomicInteger counter = nameCounters.get(type);
                if (null == counter) {
                    counter = new AtomicInteger();
                    nameCounters.put(type, counter);
                }
                keyStr = keyStr + "(" + type + " - " + counter.getAndIncrement() + ")";
            }
            Matcher matcher = keyPattern.matcher(keyStr.trim());
            if (matcher.matches()) {
                String type = matcher.group(1);
                String id = matcher.group(2);

                Class<?> modelType = classCache.get(type);
                if (null == modelType) {
                    modelType = loadModelType(type);
                    classCache.put(type, modelType);
                }

                if (null != id && mapCache.containsKey(id)) {
                    throw E.unexpected("Duplicate id '" + id + "' for type " + type);
                }

                Map entityValues = objects.get(key);
                Dao dao = null == daoLocator ? null : daoLocator.dao(modelType);
                resolveDependencies(entityValues, mapCache, entityCache, dao);
                mapCache.put(id, entityValues);
                Object entity = OsglConfig.INSTANCE_FACTORY.apply(modelType);
                $.map(entityValues)
                        .withConverter(new Lang.TypeConverter<String, Class>() {
                            @Override
                            public Class convert(String s) {
                                return loadModelType(s);
                            }
                        })
                        .to(entity);
                if (null != dao) {
                    TxScope.enter();
                    try {
                        dao.save(entity);
                        TxScope.commit();
                    } catch (Exception e) {
                        TxScope.rollback(e);
                    } finally {
                        TxScope.clear();
                    }
                }
                if (null != id) {
                    entityCache.put(id, entity);
                }
            }
        }
        return entityCache;
    }

    private void resolveConstants(Map<?, ?> entityValue) {
        for (Map.Entry entry : entityValue.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (s.startsWith("${") && s.endsWith("}")) {
                    s = s.substring(2);
                    s = s.substring(0, s.length() - 1);
                    String constant = Test.constant(s);
                    if (null != constant) {
                        entry.setValue(constant);
                    }
                }
            } else if (v instanceof Map) {
                resolveConstants((Map) v);
            }
        }
    }

    protected String getResourceAsString(String name) {
        URL url = Act.getResource(patchResourceName(name));
        return null == url ? null : IO.read(url).toString();
    }

    private String patchResourceName(String name) {
        return S.ensure(name).startWith(fixtureFolder);
    }

    private Class<?> loadModelType(String type) {
        if (type.contains(".") || $.isPrimitiveType(type)) {
            return $.classForName(type, classLoader);
        }
        for (String pkg : modelPackages) {
            String patched = S.concat(pkg, type);
            try {
                return $.classForName(patched, classLoader);
            } catch (Exception e) {
                // ignore
            }
        }
        throw new UnexpectedException("Cannot load type: %s", type);
    }

    protected void setFixtureFolder(String fixtureFolder) {
        if (S.notBlank(fixtureFolder)) {
            this.fixtureFolder = S.ensure(S.ensure(fixtureFolder.trim()).startWith("/")).endWith("/");
        }
    }

    protected void resetModelPackages() {
        this.modelPackages.add("java.util.");
        this.modelPackages.add("java.lang.");
    }

    protected void addModelPackages(String modelPackage, String... modelPackages) {
        this.addModelPackage(modelPackage);
        for (String s : modelPackages) {
            this.addModelPackage(s);
        }
    }

    private ClassLoader appClassLoader() {
        App app = Act.app();
        if (null == app) {
            return Thread.currentThread().getContextClassLoader();
        }
        ClassLoader appClassLoader = app.classLoader();
        return null == appClassLoader ? Thread.currentThread().getContextClassLoader() : appClassLoader;
    }

    private void resolveDependencies(Map<String, Object> objects, Map<String, Map<String, Object>> mapCache, Map<String, Object> entityCache, Dao dao) {
        for (String k : objects.keySet()) {
            Object v = objects.get(k);
            if (v instanceof Map) {
                resolveDependencies((Map) v, mapCache, entityCache, dao);
            } else if (v instanceof String) {
                String s = (String) v;
                if (s.startsWith("$")) {
                    String id = s.substring(1);
                    Map<String, Object> embedded = mapCache.get(id);
                    objects.put(k, null != embedded ? embedded : s);
                } else if (s.startsWith("ref:")) {
                    String id = s.substring(4);
                    Object reference = entityCache.get(id);
                    if (null == reference) {
                        throw E.unexpected("Cannot find reference object by ID: %s", id);
                    } else if (null == dao) {
                        throw E.unexpected("Cannot resolve reference when Dao is missing");
                    }
                    Object theId = dao.getId(reference);
                    objects.put(k, theId);
                } else if (s.startsWith("password:")) {
                    String password = s.substring(9);
                    objects.put(k, Act.crypto().passwordHash(password));
                } else if (k.equals("password")) {
                    objects.put(k, Act.crypto().passwordHash(s));
                }
            } else if (v instanceof List) {
                List array = (List) v;
                int len = array.size();
                for (int i = 0; i < len; i++) {
                    Object e = array.get(i);
                    if (e instanceof JSONObject) {
                        resolveDependencies((JSONObject) e, mapCache, entityCache, dao);
                    } else if (e instanceof String) {
                        String s = (String) e;
                        if (s.startsWith("[") && s.endsWith("]")) {
                            String id = s.substring(1, s.length() - 1);
                            Map<String, Object> embedded = mapCache.get(id);
                            if (null == embedded) {
                                throw E.unexpected("Cannot find embedded object by ID: %s", id);
                            }
                            array.set(i, embedded);
                        } else if (s.startsWith("embed:")) {
                            String id = s.substring(6);
                            Object embedded = entityCache.get(id);
                            if (null != embedded) {
                                array.set(i, embedded);
                            }
                        } else if (s.startsWith("ref:")) {
                            String id = s.substring(4);
                            Object reference = entityCache.get(id);
                            if (null == reference) {
                                throw E.unexpected("Cannot find reference object by ID: %s", id);
                            } else if (null == dao) {
                                throw E.unexpected("Cannot resolve reference when Dao is missing");
                            }
                            Object theId = dao.getId(reference);
                            array.set(i, theId);
                        }
                    }
                }
            }
        }
    }

    private void loadConfig() {
        App app = Act.app();
        if (null == app) {
            return;
        }
        AppConfig<?> config = app.config();
        if (null == config) {
            return;
        }
        String modelPackages = config.get("test.model-packages");
        if (S.notBlank(modelPackages)) {
            addModelPackage(modelPackages);
        }
        String fixtureFolder = config.get("test.fixture-folder");
        setFixtureFolder(fixtureFolder);
    }

}
