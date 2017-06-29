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

import act.app.App;
import act.app.AppServiceBase;
import act.util.ActContext;
import act.util.PropertySpec;
import org.joda.time.*;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.rythmengine.utils.S;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Keep the property information of Data class
 */
public class DataPropertyRepository extends AppServiceBase<DataPropertyRepository> {

    private static final Logger LOGGER = LogManager.get(DataPropertyRepository.class);

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
    private Map<String, List<String>> repo = C.newMap();

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
    public synchronized List<String> propertyListOf(Class<?> c) {
        String cn = c.getName();
        List<String> ls = repo.get(cn);
        if (ls != null) {
            return ls;
        }
        ls = buildPropertyList(c);
        repo.put(cn, ls);
        return ls;
    }

    public List<String> outputFields(PropertySpec.MetaInfo spec, Class<?> componentClass, ActContext context) {
        return outputFieldsCache.getOutputFields(spec, componentClass, context);
    }

    private List<String> buildPropertyList(Class c) {
        Method[] ma = c.getMethods();
        String context = "";
        List<String> retLst = C.newList();
        for (Method m: ma) {
            buildPropertyPath(context, m, retLst);
        }
        return retLst;
    }

    private void buildPropertyPath(String context, Method m, List<String> repo) {
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
        Class c = m.getReturnType();
        if (Class.class.equals(c)) {
            return;
        }
        if (c.isArray()) {
            Class componentType = c.getComponentType();
            List<String> retTypeProperties = propertyListOf(componentType);
            context = context + propName + ".";
            for (String s: retTypeProperties) {
                repo.add(context + s);
            }
            return;
        }
        if ($.isSimpleType(c)) {
            repo.add(context + propName);
            return;
        }
        if (Iterable.class.isAssignableFrom(c)) {
            Type t = m.getGenericReturnType();
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                Type[] ta = pt.getActualTypeArguments();
                for (Type t0: ta) {
                    Class<?> c0 = (Class) t0;
                    List<String> retTypeProperties = propertyListOf(c0);
                    context = context + propName + ".";
                    for (String s: retTypeProperties) {
                        repo.add(context + s);
                    }
                }
            }
            return;
        }
        if (terminators.contains(c) || extendedTerminators.contains(c.getName())) {
            repo.add(context + propName);
            return;
        }
        List<String> retTypeProperties = propertyListOf(c);
        context = context + propName + ".";
        for (String s : retTypeProperties) {
            repo.add(context + s);
        }
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
