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
import org.osgl.inject.BeanSpec;
import org.osgl.inject.ValueLoader;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.lang.reflect.Type;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

public class ResourceLoader<T> extends ValueLoader.Base<T> {

    private Object resource;

    @Override
    protected void initialized() {
        String path = (String) options.get("value");
        E.unexpectedIf(S.blank(path), "resource path not specified");
        boolean trimLeadingSlash = !(Boolean) options.get("skipTrimLeadingSlash");
        while (trimLeadingSlash && path.startsWith("/")) {
            path = path.substring(1);
        }
        E.unexpectedIf(S.blank(path), "resource path not specified");
        load(path, spec);
    }

    @Override
    public T get() {
        return (T) resource;
    }

    private void load(String resourcePath, BeanSpec spec) {
        URL url = loadResource(resourcePath);
        E.unexpectedIf(null == url, "Resource not found: " + resourcePath);
        Class<?> rawType = spec.rawType();
        if (String.class == rawType) {
            resource = IO.readContentAsString(url);
        } else if (byte[].class == rawType) {
            resource = readContent(url);
        } else if (List.class.isAssignableFrom(rawType)) {
            List<Type> typeParams = spec.typeParams();
            if (!typeParams.isEmpty()) {
                if (String.class == typeParams.get(0)) {
                    resource = IO.readLines(url);
                    return;
                }
            }
        } else if (ByteBuffer.class == rawType) {
            byte[] ba = readContent(url);
            ByteBuffer buffer = ByteBuffer.allocateDirect(ba.length);
            buffer.put(ba);
            buffer.flip();
            resource = buffer;
        }
        if (null == resource) {
            E.unsupport("Unsupported target type: %s", spec);
        }
    }

    private byte[] readContent(URL url) {
        return IO.readContent(IO.is(url));
    }

    private URL loadResource(String path) {
        App app = Act.app();
        if (null == app || null == app.classLoader()) {
            return ResourceLoader.class.getClassLoader().getResource(path);
        } else {
            return app.classLoader().getResource(path);
        }
    }
}
