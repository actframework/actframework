package act.data;

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

import act.annotations.Label;
import act.app.App;
import act.app.AppServiceBase;
import act.util.ActContext;
import act.util.PropertySpec;
import org.joda.time.*;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.util.C;
import org.osgl.util.Generics;
import org.osgl.util.S;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Keep the property information of Data class
 */
public class DataPropertyRepository extends AppServiceBase<DataPropertyRepository> {

    /**
     * all classes that can NOT be decomposed in terms of get properties
     */
    private Set<Class> terminators;

    /**
     * name of terminator classes.
     */
    private Set<String> extendedTerminators;

    /**
     * Map a list of property path to class name
     */
    private Map<String, List<S.Pair>> repo = new HashMap<>();

    private OutputFieldsCache outputFieldsCache = new OutputFieldsCache();

    public DataPropertyRepository(App app) {
        super(app, true);
        _init();
    }

    @Override
    protected void releaseResources() {
        extendedTerminators.clear();
        terminators.clear();
        repo.clear();
    }

    /**
     * Returns the complete property list of a class
     * @param c the class
     * @return the property list of the class
     */
    public synchronized List<S.Pair> propertyListOf(Class<?> c) {
        String cn = c.getName();
        List<S.Pair> ls = repo.get(cn);
        if (ls != null) {
            return ls;
        }
        Set<Class<?>> circularReferenceDetector = new HashSet<>();
        ls = propertyListOf(c, circularReferenceDetector, null);
        repo.put(c.getName(), ls);
        return ls;
    }

    public List<S.Pair> outputFields(PropertySpec.MetaInfo spec, Class<?> componentClass, Object firstElement, ActContext context) {
        return outputFieldsCache.getOutputFields(spec, componentClass, firstElement, context);
    }

    private List<S.Pair> propertyListOf(Class<?> c, Set<Class<?>> circularReferenceDetector, Map<String, Class> typeImplLookup) {
        if (null == typeImplLookup) {
            typeImplLookup = Generics.buildTypeParamImplLookup(c);
        } else {
            typeImplLookup.putAll(Generics.buildTypeParamImplLookup(c));
        }
        if (circularReferenceDetector.contains(c)) {
            return C.list();
        }
        circularReferenceDetector.add(c);
        try {
            return buildPropertyList(c, circularReferenceDetector, typeImplLookup);
        } finally {
            circularReferenceDetector.remove(c);
        }
    }

    private static final Comparator<S.Pair> pairComp = new Comparator<S.Pair>() {
        @Override
        public int compare(S.Pair o1, S.Pair o2) {
            return o1._1.compareTo(o2._1);
        }
    };

    private List<S.Pair> buildPropertyList(Class c, Set<Class<?>> circularReferenceDetector, Map<String, Class> typeImplLookup) {
        Method[] ma = c.getMethods();
        String context = "";
        List<S.Pair> retLst = new ArrayList<>();
        for (Method m: ma) {
            buildPropertyPath(context, m, c, retLst, circularReferenceDetector, typeImplLookup);
        }
        List<Field> fa = $.fieldsOf(c);
        for (Field f: fa) {
            int mod = f.getModifiers();
            if (Modifier.isTransient(mod) || !Modifier.isPublic(mod)) {
                continue;
            }
            buildPropertyPath(context, f, retLst, circularReferenceDetector, typeImplLookup);
        }
        Collections.sort(retLst, pairComp);
        return retLst;
    }

    private void buildPropertyPath(String context, Method m, Class<?> c, List<S.Pair> repo, Set<Class<?>> circularReferenceDetector, Map<String, Class> typeImplLookup) {
        if (Modifier.isStatic(m.getModifiers())) {
            return;
        }
        if (m.getParameterTypes().length > 0) {
            return;
        }
        String name = m.getName();
        if ("getClass".equals(name)) {
            return;
        }
        String propName = "";
        if (name.startsWith("get")) {
            propName = getPropName(name);
        } else if (name.startsWith("is")) {
            propName = isPropName(name);
        }
        if (S.isEmpty(propName)) {
            return;
        }
        Class returnType = Generics.getReturnType(m, c);
        Label label = m.getAnnotation(Label.class);
        String labelString = null == label ? null : label.value();
        buildPropertyPath(returnType, m.getGenericReturnType(), labelString, context, propName, repo, circularReferenceDetector, typeImplLookup);
    }


    private void buildPropertyPath(String context, Field field, List<S.Pair> repo, Set<Class<?>> circularReferenceDetector, Map<String, Class> typeImplLookup) {
        Label label = field.getAnnotation(Label.class);
        String labelString = null == label ? null : label.value();
        buildPropertyPath(field.getType(), field.getGenericType(), labelString, context, field.getName(), repo, circularReferenceDetector, typeImplLookup);
    }

    private void buildPropertyPath(Class<?> c, Type genericType, String label, String context, String propName, List<S.Pair> repo, Set<Class<?>> circularReferenceDetector, Map<String, Class> typeImplLookup) {
        if (Class.class.equals(c)) {
            return;
        }
        if (c.isArray()) {
            Class componentType = c.getComponentType();
            List<S.Pair> retTypeProperties = propertyListOf(componentType, circularReferenceDetector, typeImplLookup);
            context = context + propName + ".";
            for (S.Pair pair: retTypeProperties) {
                pair = S.pair(context + pair._1, pair._2);
                if (!repo.contains(pair)) {
                    repo.add(pair);
                }
            }
            return;
        }
        if ($.isSimpleType(c)) {
            String s0 = context + propName;
            S.Pair pair = S.pair(s0, label);
            if (!repo.contains(pair)) {
                repo.add(pair);
            }
            return;
        }
        if (Iterable.class.isAssignableFrom(c)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type[] ta = pt.getActualTypeArguments();
                for (Type t0: ta) {
                    Class<?> c0;
                    if (t0 instanceof Class) {
                        c0 = (Class) t0;
                    } else if (t0 instanceof TypeVariable) {
                        String typeVarName = ((TypeVariable) t0).getName();
                        c0 = typeImplLookup.get(typeVarName);
                    } else {
                        throw new UnexpectedException("Unknown type: " + t0);
                    }
                    List<S.Pair> retTypeProperties = propertyListOf(c0, circularReferenceDetector, typeImplLookup);
                    context = S.buffer(context).append(propName).append(".").toString();
                    for (S.Pair pair: retTypeProperties) {
                        pair = S.pair(context + pair._1, pair._2);
                        if (!repo.contains(pair)) {
                            repo.add(pair);
                        }
                    }
                }
            }
            return;
        }
        if (terminators.contains(c) || extendedTerminators.contains(c.getName())) {
            String s0 = context + propName;
            S.Pair pair = S.pair(s0, label);
            if (!repo.contains(pair)) {
                repo.add(pair);
            }
            return;
        }
        List<S.Pair> retTypeProperties = propertyListOf(c, circularReferenceDetector, typeImplLookup);
        context = context + propName + ".";
        for (S.Pair pair : retTypeProperties) {
            pair = S.pair(context + pair._1, pair._2);
            if (!repo.contains(pair)) {
                repo.add(pair);
            }
        }
    }

    public static List<String> getFields(List<S.Pair> fieldsAndLabels) {
        List<String> l = new ArrayList<>(fieldsAndLabels.size());
        for (S.Pair pair : fieldsAndLabels) {
            l.add(pair._1);
        }
        return l;
    }

    private static String getPropName(String name) {
        return S.lowerFirst(name.substring(3));
    }

    private static String isPropName(String name) {
        return S.lowerFirst(name.substring(2));
    }

    private void _init() {
        Set<Class> s = C.newSet();
        s.add(BigDecimal.class);
        s.add(BigInteger.class);

        s.add(Date.class);
        s.add(java.sql.Date.class);
        s.add(Calendar.class);
        s.add(DateTime.class);
        s.add(Instant.class);
        s.add(LocalDate.class);
        s.add(LocalDateTime.class);
        s.add(LocalTime.class);

        s.add(Object.class); // use toString() to ouptut

        terminators = s;

        Set<String> s0 = C.newSet();
        for (Class c: s) {
            s0.add(c.getName());
        }

        s0.add("java.time.Instant");
        s0.add("java.time.LocalTime");
        s0.add("java.time.LocalDate");
        s0.add("java.time.LocalDateTime");
        s0.add("org.bson.types.ObjectId");

        extendedTerminators = s0;
    }

}
