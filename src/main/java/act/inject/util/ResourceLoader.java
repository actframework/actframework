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
import act.app.App;
import com.alibaba.fastjson.TypeReference;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.Genie;
import org.osgl.inject.Injector;
import org.osgl.inject.ValueLoader;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

public class ResourceLoader<T> extends ValueLoader.Base<T> {

    protected static final Logger LOGGER = LogManager.get(ResourceLoader.class);

    protected Object resource;

    @Override
    protected void initialized() {
        String path = (String) options.get("value");
        E.unexpectedIf(S.blank(path), "resource path not specified");
        boolean trimLeadingSlash = !(Boolean) options.get("skipTrimLeadingSlash");
        while (trimLeadingSlash && path.startsWith("/")) {
            path = path.substring(1);
        }
        E.unexpectedIf(S.blank(path), "resource path not specified");
        resource = load(path, spec);
    }

    @Override
    public T get() {
        return (T) resource;
    }

    private static Injector injector = Genie.create();

    /**
     * A static method to load resource content
     *
     * If any exception encountered during resource load, this method returns `null`
     *
     * @param path the relative path to the resource
     * @param type the return value type
     * @param <T> generic type of return value
     * @return loaded resource or `null` if exception encountered.
     */
    public static <T> T load(String path, Class<T> type) {
        return load(path, type, false);
    }

    public static <T> T load(String path, Class<T> type, boolean ignoreResourceNotFound) {
        return load(path, BeanSpec.of(type, injector), ignoreResourceNotFound);
    }

    /**
     * A static method to load resource content
     *
     * If any exception encountered during resource load, this method returns `null`
     *
     * @param path the relative path to the resource
     * @param typeReference the return value type
     * @param <T> generic type of return value
     * @return loaded resource or `null` if exception encountered.
     */
    public static <T> T load(String path, TypeReference<T> typeReference) {
        return load(path, typeReference, false);
    }

    public static <T> T load(String path, TypeReference<T> typeReference, boolean ignoreResourceNotFound) {
        BeanSpec spec = BeanSpec.of(typeReference.getType(), injector);
        return load(path, spec, ignoreResourceNotFound);
    }


    /**
     * Load resource content from given path into variable with
     * type specified by `spec`.
     *
     * @param resourcePath the resource path
     * @param spec {@link BeanSpec} specifies the return value type
     * @return the resource content in a specified type or `null` if resource not found
     * @throws UnexpectedException if return value type not supported
     */
    public static <T> T load(String resourcePath, BeanSpec spec) {
        return load(resourcePath, spec, false);
    }

    public static <T> T load(String resourcePath, BeanSpec spec, boolean ignoreResourceNotFound) {
        return $.cast(_load(resourcePath, spec, ignoreResourceNotFound));
    }

    protected static Object _load(String resourcePath, BeanSpec spec, boolean ignoreResourceNotFound) {
        URL url = loadResource(resourcePath);
        if (null == url) {
            if (!ignoreResourceNotFound) {
                LOGGER.warn("resource not found: " + resourcePath);
            }
            return null;
        }
        Class<?> rawType = spec.rawType();
        if (URL.class == rawType) {
            return url;
        } else if (String.class == rawType) {
            return IO.readContentAsString(url);
        } else if (byte[].class == rawType) {
            return readContent(url);
        } else if (List.class.equals(rawType)) {
            List<Type> typeParams = spec.typeParams();
            if (!typeParams.isEmpty()) {
                if (String.class == typeParams.get(0)) {
                    return IO.readLines(url);
                }
            }
        } else if (Collection.class.isAssignableFrom(rawType)) {
            List<Type> typeParams = spec.typeParams();
            if (!typeParams.isEmpty()) {
                if (String.class == typeParams.get(0)) {
                    Collection<String> col = Act.getInstance((Class<Collection<String>>)rawType);
                    col.addAll(IO.readLines(url));
                    return col;
                }
            }
        } else if (ByteBuffer.class == rawType) {
            byte[] ba = readContent(url);
            ByteBuffer buffer = ByteBuffer.allocateDirect(ba.length);
            buffer.put(ba);
            buffer.flip();
            return buffer;
        } else if (InputStream.class == rawType) {
            return IO.is(url);
        } else if (Reader.class == rawType) {
            return new InputStreamReader(IO.is(url));
        } else if (ISObject.class.isAssignableFrom(rawType)) {
            return SObject.of(readContent(url));
        }
        throw new UnexpectedException("return type not supported: " + spec);
    }

    private static byte[] readContent(URL url) {
        return IO.readContent(IO.is(url));
    }

    private static URL loadResource(String path) {
        App app = Act.app();
        if (null == app || null == app.classLoader()) {
            return ResourceLoader.class.getClassLoader().getResource(path);
        } else {
            return app.classLoader().getResource(path);
        }
    }
}
