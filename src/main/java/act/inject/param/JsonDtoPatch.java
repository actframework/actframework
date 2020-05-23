package act.inject.param;

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

import act.Act;
import act.app.App;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.ValueLoader;
import org.osgl.inject.annotation.LoadValue;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import javax.persistence.Transient;

/**
 * Patch JSON DTO bean object in case there are
 * value loader annotation on certain field, and the
 * value of the field on the bean is null, then it
 * shall inject the value loader result into the field
 * on the bean.
 *
 * See https://github.com/actframework/actframework/issues/1016
 */
public class JsonDtoPatch {
    private List<JsonDtoPatch> fieldsPatches = new ArrayList<>();
    private String name;
    private ValueLoader loader;
    private JsonDtoPatch(String name, BeanSpec spec, Set<BeanSpec> circularReferenceDetector) {
        this.name = S.requireNotBlank(name);
        this.loader = valueLoaderOf(spec);
        if (null == loader) {
            for (BeanSpec fieldSpec : spec.nonStaticFields()) {
                if (circularReferenceDetector.contains(fieldSpec)) {
                    continue;
                }
                circularReferenceDetector.add(fieldSpec);
                if (fieldSpec.isTransient()
                        || fieldSpec.hasAnnotation(Transient.class)
                        || fieldSpec.hasAnnotation(NoBind.class)
                ) {
                    continue;
                }
                Class fieldType = fieldSpec.rawType();
                if (App.class == fieldType || Class.class == fieldType || Object.class == fieldType
                        || Field.class == fieldType || ResourceBundle.class == fieldType
                        || Collection.class.isAssignableFrom(fieldType)
                        || Map.class.isAssignableFrom(fieldType)
                ) {
                    continue;
                }
                String fieldName = fieldSpec.name();
                JsonDtoPatch child = new JsonDtoPatch(fieldName, fieldSpec, circularReferenceDetector);
                if (!child.isEmpty()) {
                    fieldsPatches.add(child);
                }
                circularReferenceDetector.remove(fieldSpec);
            }
        }
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name();
    }

    public void applyChildren(Object host) {
        for (JsonDtoPatch child : fieldsPatches) {
            child.apply(host);
        }
    }

    public void apply(Object host) {
        if (null != loader) {
            Object o = loader.get();
            $.setProperty(host, o, name);
        } else {
            Object o = $.getProperty(host, name);
            applyChildren(o);
        }
    }

    private boolean isEmpty() {
        return null == loader && fieldsPatches.isEmpty();
    }

    private ValueLoader valueLoaderOf(BeanSpec spec) {
        LoadValue loadValue = spec.getAnnotation(LoadValue.class);
        if (null == loadValue) {
            Annotation[] aa = spec.taggedAnnotations(LoadValue.class);
            if (aa.length > 0) {
                Annotation a = aa[0];
                loadValue = a.annotationType().getAnnotation(LoadValue.class);
            }
        }
        return null == loadValue ? null : Act.getInstance(loadValue.value());
    }

    public static JsonDtoPatch of(BeanSpec spec) {
        Set<BeanSpec> circularReferenceDetector = new HashSet<>();
        JsonDtoPatch patch = new JsonDtoPatch(spec.name(), spec, circularReferenceDetector);
        return patch.isEmpty() ? null : patch;
    }
}
