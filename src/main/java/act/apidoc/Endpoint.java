package act.apidoc;

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

import static act.apidoc.SampleDataCategory.EMAIL;
import static act.apidoc.SimpleEndpointIdProvider.className;
import static act.apidoc.SimpleEndpointIdProvider.id;

import act.Act;
import act.app.data.StringValueResolverManager;
import act.conf.AppConfig;
import act.data.DataPropertyRepository;
import act.data.Sensitive;
import act.handler.RequestHandler;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import act.inject.DefaultValue;
import act.inject.DependencyInjector;
import act.inject.param.NoBind;
import act.inject.param.ParamValueLoaderService;
import act.util.*;
import act.validation.NotBlank;
import com.alibaba.fastjson.JSON;
import org.apache.bval.constraints.NotEmpty;
import org.joda.time.*;
import org.osgl.$;
import org.osgl.OsglConfig;
import org.osgl.http.H;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.Injector;
import org.osgl.logging.Logger;
import org.osgl.mvc.annotation.*;
import org.osgl.mvc.result.Result;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import javax.validation.constraints.NotNull;

/**
 * An `Endpoint` represents an API that provides specific service
 */
public class Endpoint implements Comparable<Endpoint>, EndpointIdProvider {

    private static final Logger LOGGER = ApiManager.LOGGER;

    private static BeanSpecInterpreter beanSpecInterpreter = new BeanSpecInterpreter();

    public static class ParamInfo {
        public String bindName;
        public transient BeanSpec beanSpec;
        public String type;
        public String description;
        public String defaultValue;
        public boolean required;
        public List<String> options;

        private ParamInfo(String bindName, BeanSpec beanSpec, String description) {
            this.bindName = bindName;
            this.beanSpec = beanSpec;
            this.description = description;
            this.defaultValue = checkDefaultValue(beanSpec);
            this.required = checkRequired(beanSpec);
            this.options = checkOptions(beanSpec);
        }

        public String getName() {
            return bindName;
        }

        public String getType() {
            return null == beanSpec ? type : beanSpecInterpreter.interpret(beanSpec);
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description.replaceAll("\\n\\s+","\n");
        }

        public boolean isRequired() {
            return required;
        }

        public List<String> getOptions() {
            return options;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        private String checkDefaultValue(BeanSpec spec) {
            DefaultValue def = spec.getAnnotation(DefaultValue.class);
            if (null != def) {
                return def.value();
            }
            Class<?> type = spec.rawType();
            if (type.isPrimitive()) {
                Object o = Act.app().resolverManager().resolve("", type);
                return null != o ? o.toString() : null;
            }
            return null;
        }

        private boolean checkRequired(BeanSpec spec) {
            return (spec.hasAnnotation(NotNull.class)
                    || spec.hasAnnotation(NotBlank.class)
                    || spec.hasAnnotation(NotEmpty.class));
        }

        private List<String> checkOptions(BeanSpec spec) {
            Class<?> type = spec.rawType();
            if (type.isEnum()) {
                return C.listOf(type.getEnumConstants()).map($.F.asString());
            }
            return null;
        }
    }

    /**
     * The scheme defines the protocol used to access the endpoint
     *
     * At the moment we support HTTP only
     */
    public enum Scheme {
        HTTP
    }

    /**
     * unique identify an endpoint in an application.
     */
    public String id;

    public transient EndpointIdProvider parent;

    /**
     * The scheme used to access the endpoint
     */
    public Scheme scheme = Scheme.HTTP;

    public int port;

    /**
     * The HTTP method
     */
    public H.Method httpMethod;

    /**
     * The URL path
     */
    public String path;

    /**
     * The handler.
     *
     * In most case should be `pkg.Class.method`
     */
    public String handler;

    FastJsonPropertyPreFilter fastJsonPropertyPreFilter;

    /**
     * The description
     */
    public String description;

    /**
     * The return info description
     */
    public String returnDescription;

    public String module;

    private transient Class<?> returnType;

    private Map<String, Class> typeLookups;

    public String returnSample;

    /**
     * Param list.
     *
     * Only available when handler is driven by
     * {@link act.handler.builtin.controller.impl.ReflectedHandlerInvoker}
     */
    public List<ParamInfo> params = new ArrayList<>();

    public String sampleJsonPost;
    public String sampleQuery;
    public Class<?> controllerClass;
    public transient SampleDataProviderManager sampleDataProviderManager;

    private Endpoint() {}

    Endpoint(int port, H.Method httpMethod, String path, RequestHandler handler) {
        AppConfig conf = Act.appConfig();
        this.httpMethod = $.requireNotNull(httpMethod);
        String urlContext = conf.urlContext();
        this.path = null == urlContext || path.startsWith("/~/") ? $.requireNotNull(path) : S.concat(urlContext, $.requireNotNull(path));
        this.handler = handler.toString();
        this.port = port;
        this.sampleDataProviderManager = Act.app().sampleDataProviderManager();
        explore(handler);
    }

    @Override
    public int compareTo(Endpoint o) {
        int n = path.compareTo(o.path);
        if (0 != n) {
            return n;
        }
        return httpMethod.ordinal() - o.httpMethod.ordinal();
    }

    @Override
    public String toString() {
        return id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getParentId() {
        return null == parent ? null : parent.getId();
    }

    /**
     * Returns extends id. This is the concatenation of
     * {@link #httpMethod} and {@link #id}. This will
     * be used by the frontend UI.
     *
     * @return the extended id
     */
    public String getXid() {
        return S.concat(httpMethod, id.replace('.', '_'));
    }

    public Scheme getScheme() {
        return scheme;
    }

    public int getPort() {
        return port;
    }

    public H.Method getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public String getHandler() {
        return handler;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = processTypeImplSubstitution(description);
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public List<ParamInfo> getParams() {
        return params;
    }

    public Class<?> returnType() {
        return returnType;
    }

    public String getReturnSample() {
        return returnSample;
    }

    public String getReturnType() {
        if (void.class == returnType || Void.class == returnType) {
            return null;
        }
        return className(returnType);
    }

    public String getSampleJsonPost() {
        return sampleJsonPost;
    }

    public String getSampleQuery() {
        return sampleQuery;
    }

    public Class<?> controllerClass() {
        return controllerClass;
    }

    public String processTypeImplSubstitution(String s) {
        int n = s.indexOf("${");
        if (n < 0) {
            return s;
        }
        int a = 0;
        int z = n;
        S.Buffer buf = S.buffer();
        while (true) {
            buf.append(s.substring(a, z));
            n = s.indexOf("}", z);
            a = n;
            E.illegalArgumentIf(n < -1, "Invalid string: " + s);
            String key = s.substring(z + 2, a);
            Class<?> impl = typeLookups.get(key);
            if (null != impl) {
                buf.append(Keyword.of(className(impl)).readable().toLowerCase());
            } else {
                buf.append("${").append(key).append("}");
            }
            n = s.indexOf("${", a);
            if (n < 0) {
                buf.append(s.substring(a + 1));
                return buf.toString();
            }
            z = n;
        }
    }

    private void explore(RequestHandler handler) {
        RequestHandlerProxy proxy = $.cast(handler);
        ReflectedHandlerInvoker invoker = $.cast(proxy.actionHandler().invoker());
        Class<?> controllerClass = invoker.controllerClass();
        typeLookups = Generics.buildTypeParamImplLookup(controllerClass);
        Method method = invoker.method();
        returnType = Generics.getReturnType(method, controllerClass);
        PropertySpec pspec = method.getAnnotation(PropertySpec.class);
        if (null != pspec) {
            PropertySpec.MetaInfo propSpec = new PropertySpec.MetaInfo();
            for (String v : pspec.value()) {
                propSpec.onValue(v);
            }
            for (String v : pspec.http()) {
                propSpec.onValue(v);
            }
            List<String> outputs = propSpec.outputFieldsForHttp();
            Set<String> excluded = propSpec.excludeFieldsForHttp();
            if (!(outputs.isEmpty() && excluded.isEmpty())) {
                fastJsonPropertyPreFilter = new FastJsonPropertyPreFilter(returnType, outputs, excluded, Act.app().service(DataPropertyRepository.class));
            }
            // just ignore cli value here
        }
        Module classModule = controllerClass.getAnnotation(Module.class);
        String classModuleText = null == classModule ? inferModule(controllerClass) : classModule.value();
        this.id = id(controllerClass, method);
        if (controllerClass != method.getDeclaringClass()) {
            parent = new SimpleEndpointIdProvider(method.getDeclaringClass(), method);
        }
        Map<String, Class> typeParamLookup = C.Map();
        if (controllerClass.getGenericSuperclass() instanceof ParameterizedType) {
            typeParamLookup = Generics.buildTypeParamImplLookup(controllerClass);
        }
        Description descAnno = method.getAnnotation(Description.class);
        this.description = null == descAnno ? id(controllerClass, method) : descAnno.value();
        Module methodModule = method.getAnnotation(Module.class);
        this.module = null == methodModule ? classModuleText : methodModule.value();
        boolean payloadMethod = H.Method.POST == httpMethod || H.Method.PUT == httpMethod || H.Method.PATCH == httpMethod;
        boolean body = payloadMethod && null != invoker.singleJsonFieldName();
        exploreParamInfo(method, typeParamLookup, body);
        if (!Modifier.isStatic(method.getModifiers())) {
            exploreParamInfo(controllerClass, typeParamLookup, body);
        }
        this.controllerClass = controllerClass;
        try {
            this.returnSample = void.class == returnType ? null : generateSampleJson(BeanSpec.of(returnType, null, Act.injector()), typeParamLookup, true);
        } catch (Exception e) {
            LOGGER.warn(e, "Error creating returnSample of endpoint for request handler [%s] for [%s %s]", handler, httpMethod, path);
        }
    }

    private String inferModule(Class<?> controllerClass) {
        Class<?> enclosingClass = controllerClass.getEnclosingClass();
        if (null != enclosingClass) {
            String enclosingModule = inferModule(enclosingClass);
            return S.concat(enclosingModule, ".", controllerClass.getSimpleName());
        }
        return controllerClass.getSimpleName();
    }

    private void exploreParamInfo(Method method, Map<String, Class> typeParamLookup, boolean body) {
        Type[] paramTypes = method.getGenericParameterTypes();
        int paramCount = paramTypes.length;
        if (0 == paramCount) {
            return;
        }
        DependencyInjector injector = Act.injector();
        Method declaredMethod = overridenRequestHandlerMethod(method);
        if (null == declaredMethod) {
            return;
        }
        Annotation[][] allAnnos = declaredMethod.getParameterAnnotations();
        Map<String, Object> sampleData = new HashMap<>();
        StringValueResolverManager resolver = Act.app().resolverManager();
        List<String> sampleQuery = new ArrayList<>();
        for (int i = 0; i < paramCount; ++i) {
            Type type = paramTypes[i];
            Annotation[] annos = allAnnos[i];
            ParamInfo info = paramInfo(type, typeParamLookup, annos, injector, null, body);
            if (null != info) {
                params.add(info);
                if (path.contains("{" + info.getName() + "}")) {
                    // no sample data for URL path variable
                    continue;
                }
                Object sample;
                if (null != info.defaultValue) {
                    sample = resolver.resolve(info.defaultValue, info.beanSpec.rawType());
                } else {
                    sample = generateSampleData(info.beanSpec, typeParamLookup, new HashSet<Type>(), new ArrayList<String>(), false);
                }
                if (H.Method.GET == this.httpMethod) {
                    String query = generateSampleQuery(info.beanSpec.withoutName(), typeParamLookup, info.bindName, new HashSet<Type>(), C.<String>newList());
                    if (S.notBlank(query)) {
                        sampleQuery.add(query);
                    }
                } else {
                    sampleData.put(info.bindName, sample);
                }
            }
        }
        if (!sampleData.isEmpty()) {
            Object payload = sampleData;
            if (sampleData.size() == 1) {
                payload = sampleData.values().iterator().next();
            }
            sampleJsonPost = null == payload ? null : JSON.toJSONString(payload, true);
        }
        if (!sampleQuery.isEmpty()) {
            this.sampleQuery = S.join("&", sampleQuery);
        }
    }

    // we don't need fields declared in `@NoBind` or `@Stateless` classes
    private static final $.Predicate<Field> FIELD_PREDICATE = new $.Predicate<Field>() {
        @Override
        public boolean test(Field field) {
            return !ParamValueLoaderService.shouldWaive(field);
        }
    };

    private void exploreParamInfo(Class<?> controller, Map<String, Class> typeParamLookup, boolean body) {
        DependencyInjector injector = Act.injector();
        List<Field> fields = $.fieldsOf(controller, FIELD_PREDICATE);
        for (Field field : fields) {
            Type type = field.getGenericType();
            Annotation[] annos = field.getAnnotations();
            ParamInfo info = paramInfo(type, typeParamLookup, annos, injector, field.getName(), body);
            if (null != info) {
                params.add(info);
            }
        }
    }

    private ParamInfo paramInfo(Type type, Map<String, Class> typeParamLookup, Annotation[] annos, DependencyInjector injector, String name, boolean body) {
        if (isLoginUser(annos)) {
            return null;
        }
        BeanSpec spec = BeanSpec.of(type, annos, name, injector, typeParamLookup);
        if (ParamValueLoaderService.providedButNotDbBind(spec, injector)) {
            return null;
        }
        if (ParamValueLoaderService.hasDbBind(spec.allAnnotations())) {
            if (org.osgl.util.S.blank(name)) {
                name = spec.name();
            }
            return new ParamInfo(name, BeanSpec.of(String.class, injector, typeParamLookup), name + " id");
        }
        String description = "";
        Description descAnno = spec.getAnnotation(Description.class);
        if (null != descAnno) {
            description = descAnno.value();
        }
        return new ParamInfo(body ? spec.name() + " (body)" : spec.name(), spec, description);
    }

    private boolean isLoginUser(Annotation[] annos) {
        for (Annotation a : annos) {
            if ("LoginUser".equals(a.annotationType().getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    private String generateSampleJson(BeanSpec spec, Map<String, Class> typeParamLookup, boolean isReturn) {
        Class<?> type = spec.rawType();
        if (Result.class.isAssignableFrom(type) || void.class == type) {
            return null;
        }
        Object sample = generateSampleData(spec, typeParamLookup, new HashSet<Type>(), new ArrayList<String>(), isReturn);
        if (null == sample) {
            return null;
        }
        if (sample instanceof Map && ((Map) sample).isEmpty()) {
            return null;
        }
        if ($.isSimpleType(type)) {
            sample = C.Map("result", sample);
        }
        return JSON.toJSONString(sample, true);
    }

    private String generateSampleQuery(BeanSpec spec, Map<String, Class> typeParamLookup, String bindName, Set<Type> typeChain, List<String> nameChain) {
        Class<?> type = spec.rawType();
        String specName = spec.name();
        if (S.notBlank(specName)) {
            nameChain.add(specName);
        }
        if ($.isSimpleType(type)) {
            Object o = generateSampleData(spec, typeParamLookup, typeChain, nameChain, false);
            if (null == o) {
                return "";
            }
            return bindName + "=" + o;
        }
        if (type.isArray()) {
            // TODO handle datetime component type
            Class<?> elementType = type.getComponentType();
            BeanSpec elementSpec = BeanSpec.of(elementType, Act.injector(), typeParamLookup);
            if ($.isSimpleType(elementType)) {
                Object o = generateSampleData(elementSpec, typeParamLookup, typeChain, nameChain, false);
                if (null == o) {
                    return "";
                }
                return bindName + "=" + o
                        + "&" + bindName + "=" + generateSampleData(elementSpec, typeParamLookup, typeChain, nameChain, false);
            }
        } else if (Collection.class.isAssignableFrom(type)) {
            // TODO handle datetime component type
            List<Type> typeParams = spec.typeParams();
            Type elementType = typeParams.isEmpty() ? Object.class : typeParams.get(0);
            BeanSpec elementSpec = BeanSpec.of(elementType, null, Act.injector(), typeParamLookup);
            if ($.isSimpleType(elementSpec.rawType())) {
                Object o = generateSampleData(elementSpec, typeParamLookup, typeChain, nameChain, false);
                if (null == o) {
                    return "";
                }
                return bindName + "=" + o
                        + "&" + bindName + "=" + generateSampleData(elementSpec, typeParamLookup, typeChain, nameChain, false);
            }
        } else if (Map.class.isAssignableFrom(type)) {
            LOGGER.warn("Map not supported yet");
            return "";
        } else if (ReadableInstant.class.isAssignableFrom(type)) {
            return bindName + "=<datetime>";
        }
        if (null != stringValueResolver(type)) {
            SampleData.Category anno = spec.getAnnotation(SampleData.Category.class);
            SampleDataCategory category = null != anno ? anno.value() : null;
            return bindName + "=" + sampleDataProviderManager.getSampleData(category, bindName, String.class);
        }
        List<String> queryPairs = new ArrayList<>();
        List<Field> fields = $.fieldsOf(type);
        for (Field field : fields) {
            if (ParamValueLoaderService.shouldWaive(field)) {
                continue;
            }
            String fieldBindName = bindName + "." + field.getName();
            String pair = generateSampleQuery(BeanSpec.of(field, Act.injector(), typeParamLookup), typeParamLookup, fieldBindName, C.newSet(typeChain), C.newList(nameChain));
            if (S.notBlank(pair)) {
                queryPairs.add(pair);
            }
        }
        return S.join(queryPairs).by("&").get();
    }

    private static boolean isCollection(Type type) {
        if (type instanceof Class) {
            Class clazz = $.cast(type);
            if (Iterable.class.isAssignableFrom(clazz)) {
                return true;
            }
            return false;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = $.cast(type);
            return isCollection(ptype.getRawType());
        }
        return false;
    }

    private Object generateSampleData(BeanSpec spec, Map<String, Class> typeParamLookup, Set<Type> typeChain, List<String> nameChain, boolean isReturn) {
        return generateSampleData(spec, typeParamLookup, typeChain, nameChain, fastJsonPropertyPreFilter, isReturn);
    }

    public static Object generateSampleData(
            BeanSpec spec, Map<String, Class> typeParamLookup,
            Set<Type> typeChain, List<String> nameChain,
            FastJsonPropertyPreFilter fastJsonPropertyPreFilter,
            boolean isReturn
    ) {
        Type type = spec.type();
        if (void.class == type) {
            return null;
        }
        if (typeChain.contains(type) && !isCollection(type) && !$.isSimpleType(spec.rawType())) {
            return S.concat(spec.name(), ":", type); // circular reference detected
        }
        typeChain.add(type);
        String name = spec.name();
        if (S.notBlank(name)) {
            nameChain.add(name);
        }
        if (null != fastJsonPropertyPreFilter) {
            String path = S.join(nameChain).by(".").get();
            if (!fastJsonPropertyPreFilter.matches(path)) {
                if (spec.isArray() || Iterable.class.isAssignableFrom(spec.rawType())) {
                    return Act.getInstance(spec.rawType());
                } else {
                    return null;
                }
            }
        }
        SampleData.Category anno = spec.getAnnotation(SampleData.Category.class);
        SampleDataCategory category = null != anno ? anno.value() : null;
        Class<?> classType = spec.rawType();
        SampleDataProviderManager sampleDataProviderManager = Act.app().sampleDataProviderManager();
        Object o = sampleDataProviderManager.getSampleData(category, name, classType, false);
        if (null != o) {
            return o;
        }
        try {
            if (void.class == classType || Void.class == classType || Result.class.isAssignableFrom(classType)) {
                return null;
            }
            if (Object.class == classType) {
                return "<Any>";
            }
            try {
                if (classType.isEnum()) {
                    Object[] ea = classType.getEnumConstants();
                    int len = ea.length;
                    return 0 < len ? ea[N.randInt(len)] : null;
                } else if (Locale.class == classType) {
                    return (Act.appConfig().locale());
                } else if (String.class == classType || Keyword.class == classType) {
                    String mockValue = sampleDataProviderManager.getSampleData(category, name, String.class);
                    if (spec.hasAnnotation(Sensitive.class)) {
                        return Act.crypto().encrypt(mockValue);
                    }
                    return Keyword.class == classType ? Keyword.of(mockValue) : mockValue;
                } else if (classType.isArray()) {
                    Object sample = Array.newInstance(classType.getComponentType(), 2);
                    Array.set(sample, 0, generateSampleData(BeanSpec.of(classType.getComponentType(), Act.injector(), typeParamLookup), typeParamLookup, C.newSet(typeChain), C.newList(nameChain), fastJsonPropertyPreFilter, isReturn));
                    Array.set(sample, 1, generateSampleData(BeanSpec.of(classType.getComponentType(), Act.injector(), typeParamLookup), typeParamLookup, C.newSet(typeChain), C.newList(nameChain), fastJsonPropertyPreFilter, isReturn));
                    return sample;
                } else if ($.isSimpleType(classType)) {
                    if (Enum.class == classType) {
                        return "<Any Enum>";
                    }
                    if (!classType.isPrimitive()) {
                        classType = $.primitiveTypeOf(classType);
                    }
                    return StringValueResolver.predefined().get(classType).resolve(null);
                } else if (LocalDateTime.class.isAssignableFrom(classType)) {
                    return sampleDataProviderManager.getSampleData(category, name, LocalDateTime.class);
                } else if (DateTime.class.isAssignableFrom(classType)) {
                    return sampleDataProviderManager.getSampleData(category, name, DateTime.class);
                } else if (LocalDate.class.isAssignableFrom(classType)) {
                    return sampleDataProviderManager.getSampleData(category, name, LocalDate.class);
                } else if (LocalTime.class.isAssignableFrom(classType)) {
                    return LocalTime.now();
                } else if (Date.class.isAssignableFrom(classType)) {
                    return sampleDataProviderManager.getSampleData(category, name, Date.class);
                } else if (BigDecimal.class == classType) {
                    return BigDecimal.valueOf(1.1);
                } else if (BigInteger.class == classType) {
                    return BigInteger.valueOf(1);
                } else if (ISObject.class.isAssignableFrom(classType)) {
                    return SObject.of("blob data");
                } else if (Map.class.isAssignableFrom(classType)) {
                    Map map = $.cast(Act.getInstance(classType));
                    List<Type> typeParams = spec.typeParams();
                    if (typeParams.isEmpty()) {
                        typeParams = Generics.tryGetTypeParamImplementations(classType, Map.class);
                    }
                    if (typeParams.size() < 2) {
                        return null;
                    } else {
                        Type keyType = typeParams.get(0);
                        Type valType = typeParams.get(1);
                        if (Object.class == valType) {
                            return null;
                        }
                        Object key1 = "foo";
                        Object key2 = "bar";
                        if (keyType != String.class) {
                            key1 = generateSampleData(BeanSpec.of(keyType, null, Act.injector(), typeParamLookup), typeParamLookup, C.newSet(typeChain), C.newList(nameChain), fastJsonPropertyPreFilter, isReturn);
                            key2 = generateSampleData(BeanSpec.of(keyType, null, Act.injector(), typeParamLookup), typeParamLookup, C.newSet(typeChain), C.newList(nameChain), fastJsonPropertyPreFilter, isReturn);
                        }
                        Object val1 = generateSampleData(BeanSpec.of(valType, null, Act.injector(), typeParamLookup), typeParamLookup, C.newSet(typeChain), C.newList(nameChain), fastJsonPropertyPreFilter, isReturn);
                        Object val2 = generateSampleData(BeanSpec.of(valType, null, Act.injector(), typeParamLookup), typeParamLookup, C.newSet(typeChain), C.newList(nameChain), fastJsonPropertyPreFilter, isReturn);
                        map.put(key1, val1);
                        map.put(key2, val2);
                    }
                    return map;
                } else if (Iterable.class.isAssignableFrom(classType)) {
                    Collection col = $.cast(Act.getInstance(classType));
                    List<Type> typeParams = spec.typeParams();
                    if (typeParams.isEmpty()) {
                        typeParams = Generics.tryGetTypeParamImplementations(classType, Map.class);
                    }
                    if (typeParams.isEmpty()) {
                        return null;
                    } else {
                        Type componentType = typeParams.get(0);
                        col.add(generateSampleData(BeanSpec.of(componentType, null, Act.injector(), typeParamLookup), typeParamLookup, C.newSet(typeChain), C.newList(nameChain), fastJsonPropertyPreFilter, isReturn));
                        col.add(generateSampleData(BeanSpec.of(componentType, null, Act.injector(), typeParamLookup), typeParamLookup, C.newSet(typeChain), C.newList(nameChain), fastJsonPropertyPreFilter, isReturn));
                    }
                    return col;
                }

                Object obj = sampleDataProviderManager.getSampleData(category, name, classType);
                if (null != obj) {
                    return obj;
                }

                Injector injector = Act.injector();
                try {
                    obj = Act.getInstance(classType);
                } catch (Exception e) {
                    Method emailGetter = null;
                    Map<String, Object> map = new HashMap<>();
                    Method[] ma = classType.getMethods();
                    for (Method m : ma) {
                        if (!Modifier.isStatic(m.getModifiers()) && m.getName().startsWith("get") && m.getReturnType() != void.class) {
                            if (shouldWaive(m, classType)) {
                                continue;
                            }
                            Class<?> propertyClass = m.getReturnType();
                            Object val = null;
                            try {
                                String propertyName = m.getName().substring(3);
                                if ("name".equalsIgnoreCase(propertyName)) {
                                    propertyName = m.getDeclaringClass().getSimpleName();
                                }
                                Annotation[] annotations = m.getDeclaredAnnotations();
                                Type propertyType = m.getGenericReturnType();
                                if (propertyType instanceof TypeVariable) {
                                    propertyType = propertyClass;
                                }
                                BeanSpec propertySpec = BeanSpec.of(propertyType, annotations, propertyName, injector, m.getModifiers(), typeParamLookup);
                                if (null == emailGetter && isEmail(propertySpec)) {
                                    emailGetter = m;
                                } else {
                                    val = generateSampleData(propertySpec, typeParamLookup, C.newSet(typeChain), C.newList(nameChain), fastJsonPropertyPreFilter, isReturn);
                                    if (null != val) {
                                        Class<?> valType = val.getClass();
                                        if (!propertyClass.isAssignableFrom(valType)) {
                                            val = $.convert(val).to(propertyClass);
                                        }
                                        if (null != val) {
                                            map.put(propertyName, val);
                                        }
                                    }
                                }
                            } catch (Exception e1) {

                            }
                        }
                    }
                    if (null != emailGetter) {
                        String mockEmail = sampleDataProviderManager.getSampleData(SampleDataCategory.EMAIL, name, String.class);
                        map.put(emailGetter.getName().substring(3), mockEmail);
                    }
                    return map;
                }
                List<Field> fields = $.fieldsOf(classType);
                Field emailField = null;
                for (Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    if (!isReturn) {
                        if (ParamValueLoaderService.shouldWaive(field)) {
                            continue;
                        }
                    } else {
                        // for return type we shouldn't waive NoBind fields
                        int modifiers = field.getModifiers();
                        if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) {
                            continue;
                        }
                        String fieldName = field.getName();
                        Class<?> entityType = field.getDeclaringClass();
                        boolean shouldWaive = Object.class.equals(entityType)
                                || Class.class.equals(entityType)
                                || OsglConfig.globalMappingFilter_shouldIgnore(fieldName);
                        if (shouldWaive) {
                            continue;
                        }
                    }
                    Class<?> fieldClass = field.getType();
                    Object val = null;
                    try {
                        field.setAccessible(true);
                        String fieldName = field.getName();
                        if ("name".equalsIgnoreCase(fieldName)) {
                            fieldName = field.getDeclaringClass().getSimpleName();
                        }
                        Annotation[] annotations = field.getDeclaredAnnotations();
                        Type fieldType = field.getGenericType();
                        if (fieldType instanceof TypeVariable) {
                            fieldType = fieldClass;
                        }
                        BeanSpec fieldSpec = BeanSpec.of(fieldType, annotations, fieldName, injector, field.getModifiers(), typeParamLookup);
                        if (null == emailField && isEmail(fieldSpec)) {
                            emailField = field;
                        } else {
                            val = generateSampleData(fieldSpec, typeParamLookup, C.newSet(typeChain), C.newList(nameChain), fastJsonPropertyPreFilter, isReturn);
                            if (null == val) {
                                continue;
                            }
                            Class<?> valType = val.getClass();
                            if (!fieldClass.isAssignableFrom(valType)) {
                                val = $.convert(val).to(fieldClass);
                            }
                            if (null != val) {
                                if (valType == String.class && fieldSpec.hasAnnotation(Sensitive.class)) {
                                    val = Act.app().crypto().encrypt((String) val);
                                }
                                field.set(obj, val);
                            }
                        }
                    } catch (Exception e2) {
                        LOGGER.warn("Error setting value[%s] to field[%s.%s]", val, classType.getSimpleName(), field.getName());
                    }
                }
                if (null != emailField) {
                    String mockEmail = sampleDataProviderManager.getSampleData(SampleDataCategory.EMAIL, name, String.class);
                    $.setFieldValue(obj, emailField, mockEmail);
                }
                return obj;
            } catch (Exception e) {
                LOGGER.warn("error generating sample data for type: %s", classType);
                return null;
            }
        } finally {
            //typeChain.remove(classType);
        }
    }

    private static boolean isEmail(BeanSpec spec) {
        SampleData.Category anno = spec.getAnnotation(SampleData.Category.class);
        SampleDataCategory category = null != anno ? anno.value() : null;
        if (null != category && category != EMAIL) {
            return false;
        }
        category = SampleDataCategory.of(spec.name());
        return category == EMAIL;
    }

    private static <T> StringValueResolver stringValueResolver(Class<? extends T> type) {
        return Act.app().resolverManager().resolver(type);
    }

    // see ParamValueLoaderService.shouldWaive(Field)
    private static boolean shouldWaive(Method getter, Class<?> implementClass) {
        int modifiers = getter.getModifiers();
        if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) {
            return true;
        }
        String fieldName = getter.getName().substring(3);
        Class<?> entityType = Generics.getReturnType(getter, implementClass);
        return ParamValueLoaderService.noBind(entityType)
                || getter.isAnnotationPresent(NoBind.class)
                || getter.isAnnotationPresent(Stateless.class)
                || getter.isAnnotationPresent(Global.class)
                || ParamValueLoaderService.isInBlackList(fieldName)
                || Object.class.equals(entityType)
                || Class.class.equals(entityType)
                || OsglConfig.globalMappingFilter_shouldIgnore(fieldName);
    }

    private static Method overridenRequestHandlerMethod(Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            return null;
        }
        if (isRequestHandler(method)) {
            return method;
        }
        Method overridenMethod = overridenMethod(method);
        return null == overridenMethod ? null : overridenRequestHandlerMethod(overridenMethod);
    }

    private static Method overridenMethod(Method method) {
        Class<?> host = method.getDeclaringClass();
        Class<?> superHost = host.getSuperclass();
        if (null == superHost || Object.class == superHost) {
            return null;
        }
        Method[] ma = superHost.getMethods();
        for (Method m : ma) {
            if (m.getName().equals(method.getName()) && $.eq2(method.getParameterTypes(), method.getParameterTypes())) {
                return m;
            }
        }
        return null;
    }

    private static boolean isRequestHandler(Method method) {
        return method.isAnnotationPresent(Action.class)
                || method.isAnnotationPresent(GetAction.class)
                || method.isAnnotationPresent(PutAction.class)
                || method.isAnnotationPresent(PostAction.class)
                || method.isAnnotationPresent(PatchAction.class)
                || method.isAnnotationPresent(DeleteAction.class);
    }


}
