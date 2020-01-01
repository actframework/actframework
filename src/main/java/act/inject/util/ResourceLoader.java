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
import act.app.DevModeClassLoader;
import act.app.data.StringValueResolverManager;
import act.util.$$;
import act.util.HeaderMapping;
import act.util.Jars;
import com.alibaba.fastjson.*;
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
import org.osgl.util.*;
import org.osgl.util.TypeReference;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ResourceLoader<T> extends ValueLoader.Base<T> {

    protected static final Logger LOGGER = LogManager.get(ResourceLoader.class);

    private static abstract class ReadStageBaseAcceptor<SOURCE, STAGE extends IO.ReadStageBase> {

        abstract STAGE accept(SOURCE src);
    }

    private static class UrlReadStageAcceptor extends ReadStageBaseAcceptor<URL, IO.UrlReadStage> {
        @Override
        IO.UrlReadStage accept(URL src) {
            return new IO.UrlReadStage(src);
        }
    }

    private static class StringReadStageAcceptor extends ReadStageBaseAcceptor<String, IO.CharSequenceReadStage> {
        @Override
        IO.CharSequenceReadStage accept(String src) {
            return new IO.CharSequenceReadStage(src);
        }
    }

    protected Object resource;

    protected Object hint;

    @Override
    protected void initialized() {
        String path = (String) options.get("value");
        E.unexpectedIf(S.blank(path), "resource path not specified");
        boolean trimLeadingSlash = !$.bool(options.get("skipTrimLeadingSlash"));
        while (trimLeadingSlash && path.startsWith("/")) {
            path = path.substring(1);
        }
        E.unexpectedIf(S.blank(path), "resource path not specified");
        HeaderMapping anno = spec.getAnnotation(HeaderMapping.class);
        if (null != anno) {
            this.hint = HeaderMapping.Parser.parse(anno.value());
        }
        resource = load(path, spec, hint);
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
     * @param <T>  generic type of return value
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
     * @param path          the relative path to the resource
     * @param typeReference the return value type
     * @param <T>           generic type of return value
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
     * @param spec         {@link BeanSpec} specifies the return value type
     * @return the resource content in a specified type or `null` if resource not found
     * @throws UnexpectedException if return value type not supported
     */
    public static <T> T load(String resourcePath, BeanSpec spec) {
        return load(resourcePath, spec, false);
    }

    public static <T> T load(String resourcePath, BeanSpec spec, Object hint) {
        return $.cast(_load(resourcePath, spec, hint, false));
    }

    public static <T> T load(String resourcePath, BeanSpec spec, boolean ignoreResourceNotFound) {
        return $.cast(_load(resourcePath, spec, null, ignoreResourceNotFound));
    }

    protected static Object _load(String resourcePath, BeanSpec spec, Object hint, boolean ignoreResourceNotFound) {
        URL url = loadResource(resourcePath);
        if (null == url) {
            if (!ignoreResourceNotFound) {
                LOGGER.warn("resource not found: " + resourcePath);
            }
            return null;
        }
        if (Act.isDev()) {
            DevModeClassLoader classLoader = $.cast(Act.app().classLoader());
            classLoader.registerResourceFileDetector(resourcePath);
        }
        return _load(url, spec, hint);
    }

    private static Object _load(URL url, BeanSpec spec, Object hint) {
        $.Var<JarEntry> entryBag = $.var();
        $.Var<JarFile> jarFileBag = $.var();
        $.Var<File> fileBag = $.var();
        if (isDir(url, entryBag, jarFileBag, fileBag)) {
            E.unsupportedIfNot(Map.class.isAssignableFrom(spec.rawType()), "Does not support loading directory into " + spec);
            JarEntry entry = entryBag.get();
            Map map = $.cast(Act.getInstance(spec.rawType()));
            List<Type> mapTypes = spec.typeParams();
            Type valType = mapTypes.size() > 1 ? mapTypes.get(1) : String.class;
            BeanSpec subSpec = BeanSpec.of(valType, spec.injector());
            boolean isKeyword = false;
            if (mapTypes.size() > 0) {
                Type keyType = mapTypes.get(0);
                if (keyType instanceof Class) {
                    isKeyword = Keyword.class == keyType;
                    E.unsupportedIfNot(isKeyword || String.class == keyType, "Map spec not supported: " + spec);
                } else {
                    throw E.unsupport("Map spec not supported: " + spec);
                }
            }
            if (null != entry) {
                String dirName = entry.getName();
                int dirNameLen = dirName.length();
                JarFile file = jarFileBag.get();
                String parentSpec = file.toString();
                Enumeration<JarEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry0 = entries.nextElement();
                    String entryName = entry0.getName();
                    if (dirName.equals(entryName)) {
                        continue;
                    }
                    if (!entryName.startsWith(dirName)) {
                        continue;
                    }
                    if (entry0.isDirectory()) {
                        // we don't support loading recursively
                        continue;
                    }
                    String fileName = entryName.substring(dirNameLen);
                    if (fileName.contains("/")) {
                        // we don't support sub dir resources
                        continue;
                    }
                    String subUrlSpec = S.concat(parentSpec, "/", fileName);
                    try {
                        URL subUrl = new URL(subUrlSpec);
                        String s = S.cut(fileName).beforeLast(".");
                        map.put(isKeyword ? Keyword.of(s) : s, _load(subUrl, subSpec, hint));
                    } catch (MalformedURLException e) {
                        throw E.unexpected(e);
                    }
                }
            } else {
                File dir = fileBag.get();
                File[] files = dir.listFiles();
                for (File file : files) {
                    if (file.isDirectory()) {
                        // we don't support loading recursively
                        continue;
                    }
                    String key = S.cut(file.getName()).beforeLast(".");
                    try {
                        URL fileUrl = file.toURI().toURL();
                        map.put(isKeyword ? Keyword.of(key) : key, _load(fileUrl, subSpec, hint));
                    } catch (MalformedURLException e) {
                        throw E.unexpected(e);
                    }
                }
            }
            return map;
        } // eof isDir

        Class<?> rawType = spec.rawType();
        if (URL.class == rawType) {
            return url;
        }

        IO.ReadStageBase readStage;
        if (Act.appConfig().resourceFiltering()) {
            if (isBinary(url, spec)) {
                readStage = new IO.UrlReadStage(url);
            } else {
                String content = IO.readContentAsString(url);
                content = $$.processStringSubstitution(content);
                readStage = new IO.CharSequenceReadStage(content);
                MimeType mimeType = MimeType.findByFileExtension(S.fileExtension(url.getFile()));
                if (null != mimeType) {
                    readStage.contentType(mimeType);
                }
            }
        } else {
            readStage = new IO.UrlReadStage(url);
        }

        try {
            return null == hint ? readStage.to(spec) : readStage.hint(hint).to(spec);
        } catch (Exception e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(e, "error read URL[%s] to [%s] via IO.read call", url, spec);
            }
            // ignore
        }

        if (rawType.isArray()) {
            if (byte[].class == rawType) {
                return readStage.toByteArray();
            }
            Class<?> componentType = rawType.getComponentType();
            if (componentType.isArray()) {
                Class<?> subComponentType = componentType.getComponentType();
                boolean isString = String.class == subComponentType;
                boolean isPrimitive = !isString && $.isPrimitiveType(subComponentType);
                boolean isWrapper = !isPrimitive && !isString && $.isWrapperType(subComponentType);
                if (isString || isPrimitive || isWrapper) {
                    List<String> lines = readStage.toLines();
                    int len = lines.size();
                    Object a2 = Array.newInstance(componentType, len);
                    for (int i = 0; i < len; ++i) {
                        String line = lines.get(i);
                        List<String> elements = S.fastSplit(line, ",");
                        int len2 = elements.size();
                        Object a = Array.newInstance(subComponentType, len2);
                        Array.set(a2, i, a);
                        for (int j = 0; j < len2; ++j) {
                            Object e = $.convert(elements.get(j)).to(subComponentType);
                            if (isPrimitive) {
                                if (int.class == subComponentType) {
                                    Array.setInt(a, j, (Integer) e);
                                } else if (double.class == subComponentType) {
                                    Array.setDouble(a, j, (Double) e);
                                } else if (long.class == subComponentType) {
                                    Array.setLong(a, j, (Long) e);
                                } else if (float.class == subComponentType) {
                                    Array.setFloat(a, j, (Float) e);
                                } else if (boolean.class == subComponentType) {
                                    Array.setBoolean(a, j, (Boolean) e);
                                } else if (short.class == subComponentType) {
                                    Array.setShort(a, j, (Short) e);
                                } else if (byte.class == subComponentType) {
                                    Array.setByte(a, j, (Byte) e);
                                } else if (char.class == subComponentType) {
                                    Array.setChar(a, j, (Character) e);
                                } else {
                                    throw E.unsupport("Sub component type not supported: " + subComponentType.getName());
                                }
                            } else {
                                Array.set(a, j, e);
                            }
                        }
                    }
                    return a2;
                } else {
                    throw E.unsupport("Sub component type not supported: " + subComponentType.getName());
                }
            } else {
                List<String> lines = readStage.toLines();
                if (String.class == componentType) {
                    return lines.toArray(new String[lines.size()]);
                }
                Object array = Array.newInstance(componentType, lines.size());
                return $.map(lines).to(array);
            }
        }
        String resourcePath = url.getPath();
        boolean isJson = resourcePath.endsWith(".json");
        if (isJson) {
            String content = readStage.toString();
            content = content.trim();
            Object o = content.startsWith("[") ? JSON.parseArray(content) : JSON.parseObject(content);
            return $.map(o).targetGenericType(spec.type()).to(rawType);
        }
        boolean isYaml = (resourcePath.endsWith(".yml") || resourcePath.endsWith(".yaml"));
        if (isYaml) {
            Object o = new Yaml().load(readStage.toString());
            if (List.class.isAssignableFrom(rawType)) {
                List<Type> typeParams = spec.typeParams();
                Class<?> listElementType = (Class<?>) typeParams.get(0);
                if (o instanceof Map) {
                    Map src = (Map)o;
                    List sink = new ArrayList(src.size());
                    for (Object val : src.values()) {
                        sink.add($.map(val).to(listElementType));
                    }
                    return sink;
                } else if (o instanceof List) {
                    List src = (List) o;
                    List sink = new ArrayList(src.size());
                    for (Object val : src) {
                        sink.add($.map(val).to(listElementType));
                    }
                    return sink;
                } else {
                    return $.map(o).targetGenericType(spec.type()).to(rawType);
                }
            } else {
                return $.map(o).targetGenericType(spec.type()).to(rawType);
            }
        }
        boolean isXml = resourcePath.endsWith(".xml");
        if (isXml) {
            Object o = XML.read(readStage.toReader());
            if ($.isCollectionType(rawType)) {
                JSONArray array = $.convert(o).to(JSONArray.class);
                return $.map(array).targetGenericType(spec.type()).to(rawType);
            } else {
                JSONObject json = $.convert(o).to(JSONObject.class);
                return $.map(json).targetGenericType(spec.type()).to(rawType);
            }
        }
        if (String.class == rawType) {
            return readStage.toString();
        } else if (List.class.equals(rawType)) {
            List<Type> typeParams = spec.typeParams();
            List<String> lines = C.newList(readStage.toLines());
            if (!typeParams.isEmpty()) {
                if (String.class == typeParams.get(0)) {
                    return lines;
                }
                Type typeParam = typeParams.get(0);
                if (typeParam instanceof Class) {
                    List list = new ArrayList(lines.size());
                    for (String line : lines) {
                        list.add($.convert(line).to((Class) typeParam));
                    }
                    return list;
                }
                throw E.unsupport("List element type not supported: " + typeParam);
            }
        } else if (Map.class.isAssignableFrom(rawType)) {
            if (resourcePath.endsWith(".properties")) {
                Properties properties = IO.loadProperties(readStage.toInputStream());
                if (Properties.class == rawType || Properties.class.isAssignableFrom(rawType)) {
                    return properties;
                }
                return $.map(properties).targetGenericType(spec.type()).to(rawType);
            } else {
                // try my best
                C.List<String> lines = C.newList(readStage.toLines());
                if (lines.isEmpty()) {
                    return C.Map();
                }
                lines = lines.filter(S.F.startsWith("#").negate());
                ListIterator<String> itr = lines.listIterator();
                String firstLine = itr.next();
                while (itr.hasNext()) {
                    if (!firstLine.startsWith("#")) {
                        break;
                    }
                    firstLine = itr.next();
                }
                char sep;
                if (firstLine.contains("=")) {
                    sep = '=';
                } else if (firstLine.contains(":")) {
                    sep = ':';
                } else {
                    throw new UnexpectedException("Unable to load resource into Map: " + resourcePath);
                }
                Map map = (Map)Act.getInstance(spec.rawType());
                List<Type> mapTypeParams = spec.typeParams();
                Class<?> keyType = String.class;
                Class<?> valType = String.class;
                if (null != mapTypeParams && mapTypeParams.size() == 2) {
                    keyType = (Class) mapTypeParams.get(0);
                    valType = (Class) mapTypeParams.get(1);
                }
                for (String line : lines) {
                    S.Pair pair = S.binarySplit(line, sep);
                    String key = pair.left();
                    String val = pair.right();
                    if (val.contains("#")) {
                        val = S.cut(val).beforeFirst("#").trim();
                    }
                    map.put($.convert(key).to(keyType), $.convert(val).to(valType));
                }
                return map;
            }
        } else if (Collection.class.isAssignableFrom(rawType)) {
            List<Type> typeParams = spec.typeParams();
            if (!typeParams.isEmpty()) {
                Collection col = (Collection) Act.getInstance(rawType);
                if (String.class == typeParams.get(0)) {
                    col.addAll(readStage.toLines());
                    return col;
                } else {
                    StringValueResolverManager resolverManager = Act.app().resolverManager();
                    try {
                        Class componentType = spec.componentSpec().rawType();
                        List<String> stringList = readStage.toLines();
                        for (String line : stringList) {
                            col.add(resolverManager.resolve(line, componentType));
                        }
                    } catch (RuntimeException e) {
                        throw new UnexpectedException("return type not supported: " + spec);
                    }
                }
            }
        } else if (ByteBuffer.class == rawType) {
            byte[] ba = readStage.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocateDirect(ba.length);
            buffer.put(ba);
            buffer.flip();
            return buffer;
        } else if (Path.class.isAssignableFrom(rawType)) {
            try {
                return Paths.get(url.toURI());
            } catch (URISyntaxException exception) {
                throw E.unexpected(exception);
            }
        } else if (File.class.isAssignableFrom(rawType)) {
            return new File(url.getFile());
        } else if (ISObject.class.isAssignableFrom(rawType)) {
            return SObject.of(readStage.toInputStream());
        } else if (InputStream.class == rawType) {
            return readStage.toInputStream();
        } else if (Reader.class == rawType) {
            return readStage.toReader();
        }
        String content = readStage.toString();
        try {
            return Act.app().resolverManager().resolve(content, rawType);
        } catch (RuntimeException e) {
            throw new UnexpectedException("return type not supported: " + spec);
        }
    }

    private static boolean isDir(URL url, $.Var<JarEntry> entryBag, $.Var<JarFile> jarFileBag, $.Var<File> fileBag) {
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            File file = new File(url.getFile());
            fileBag.set(file);
            return file.isDirectory();
        } else if ("jar".equals(protocol)) {
            JarEntry entry = Jars.jarEntry(url, jarFileBag);
            entryBag.set(entry);
            return entry.isDirectory();
        } else {
            throw E.unsupport("URL protocol not supported: " + url);
        }
    }

    private static byte[] readContent(URL url) {
        return IO.readContent(IO.inputStream(url));
    }

    private static URL loadResource(String path) {
        App app = Act.app();
        if (null == app || null == app.classLoader()) {
            return ResourceLoader.class.getClassLoader().getResource(path);
        } else {
            return app.classLoader().getResource(path);
        }
    }

    private static boolean isBinary(URL url, BeanSpec spec) {
        if (isBinary(spec)) {
            return true;
        }
        return SObject.of(url).isBinary();
    }

    public static boolean isBinary(BeanSpec spec) {
        Class<?> type = spec.rawType();
        if (byte[].class == type) {
            return true;
        }
        if (URL.class == type) {
            return true;
        }
        if (InputStream.class == type) {
            return true;
        }
        if (ISObject.class.isAssignableFrom(type)) {
            return true;
        }
        return false;
    }
}
