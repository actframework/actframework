package act.inject.util;

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

import act.Act;
import com.alibaba.fastjson.TypeReference;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.Genie;
import org.osgl.inject.Injector;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Map;

public class ConfigResourceLoader extends ResourceLoader {

    private boolean ignoreResourceNotFound;

    public ConfigResourceLoader() {
    }

    public ConfigResourceLoader(boolean ignoreResourceNotFound) {
        this.ignoreResourceNotFound = ignoreResourceNotFound;
    }

    @Override
    protected void initialized() {
        String path = (String) options.get("value");
        E.unexpectedIf(S.blank(path), "resource path not specified");
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.startsWith("config/")) {
            path = path.substring(7);
        }
        resource = ResourceLoader._load(profileConfig(path), spec, hint, true);
        if (null == resource) {
            resource = ResourceLoader._load(commonConfig(path), spec, hint, true);
        }
        if (null == resource) {
            resource = ResourceLoader._load(confConfig(path), spec, hint, true);
        }
        if (null == resource) {
            resource = ResourceLoader._load(path, spec, hint, true);
        }
        if (null == resource && !ignoreResourceNotFound) {
            LOGGER.warn("config resource not found: %s", path);
        }
    }

    private void init(Map options, BeanSpec spec, Object hint) {
        super.init(options, spec);
        this.hint = hint;
    }

    private String profileConfig(String path) {
        return S.concat("conf/", Act.profile(), "/", path);
    }

    private String commonConfig(String path) {
        return S.concat("conf/common/", path);
    }

    private String confConfig(String path) {
        return S.concat("conf/", path);
    }

    private static Injector injector = Genie.create();

    /**
     * A static method to load resource favor the profile configuration.
     *
     * If any exception encountered during resource load, this method returns `null`
     *
     * @param path the relative path to the resource
     * @param type the return value type
     * @param <T> generic type of return value
     * @return loaded resource or `null` if exception encountered.
     */
    public static <T> T load(String path, Class<T> type) {
        return load(path, type, null, false);
    }

    public static <T> T load(String path, Class<T> type, Object hint) {
        return load(path, type, hint, false);
    }

    public static <T> T load(String path, Class<T> type, boolean ignoreResourceNotFound) {
        return load(path, type, null, ignoreResourceNotFound);
    }

    public static <T> T load(String path, Class<T> type, Object hint, boolean ignoreResourceNotFound) {
        return __load(path, BeanSpec.of(type, injector), hint, ignoreResourceNotFound);
    }

    /**
     * A static method to load resource favor the profile configuration.
     *
     * If any exception encountered during resource load, this method returns `null`
     *
     * @param path the relative path to the resource
     * @param typeReference the return value type
     * @param <T> generic type of return value
     * @return loaded resource or `null` if exception encountered.
     */
    public static <T> T load(String path, TypeReference<T> typeReference) {
        return load(path, typeReference, null, false);
    }

    public static <T> T load(String path, TypeReference<T> typeReference, boolean ignoreResourceNotFound) {
        return load(path, typeReference, null, ignoreResourceNotFound);
    }

    public static <T> T load(String path, TypeReference<T> typeReference, Object hint) {
        return load(path, typeReference, hint, false);
    }

    public static <T> T load(String path, TypeReference<T> typeReference, Object hint, boolean ignoreResourceNotFound) {
        BeanSpec spec = BeanSpec.of(typeReference.getType(), injector);
        return __load(path, spec, hint, ignoreResourceNotFound);
    }

    /**
     * A static method to load resource favor the profile configuration.
     *
     * If any exception encountered during resource load, this method returns `null`
     *
     * @param path the relative path to the resource
     * @param spec bean spec specifies return value type
     * @param <T> generic type of return value
     * @return loaded resource or `null` if exception encountered.
     */
    public static <T> T load(String path, BeanSpec spec) {
        return load(path, spec, null, false);
    }

    public static <T> T load(String path, BeanSpec spec, boolean ignoreResourceNotFound) {
        return load(path, spec, null, ignoreResourceNotFound);
    }

    public static <T> T load(String path, BeanSpec spec, Object hint) {
        return load(path, spec, hint, false);
    }

    public static <T> T load(String path, BeanSpec spec, Object hint, boolean ignoreResourceNotFound) {
        return __load(path, spec, hint, ignoreResourceNotFound);
    }

    private static <T> T __load(String path, BeanSpec spec, Object hint, boolean ignoreResourceNotFound) {
        Map<String, Object> option = C.Map("value", path);
        ConfigResourceLoader loader = new ConfigResourceLoader(ignoreResourceNotFound);
        loader.init(option, spec, hint);
        return $.cast(loader.resource);
    }

}
