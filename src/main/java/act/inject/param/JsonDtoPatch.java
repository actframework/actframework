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
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.ValueLoader;
import org.osgl.inject.annotation.LoadValue;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.*;

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
    private List<JsonDtoPatch> fieldsPatch = new ArrayList<>();
    private String name;
    private JsonDtoPatch parent;
    private ValueLoader loader;
    private JsonDtoPatch(String name, BeanSpec spec, JsonDtoPatch parent) {
        this.name = S.requireNotBlank(name);
        this.parent = parent;
        this.loader = valueLoaderOf(spec);
        if (null == loader) {
            for (Map.Entry<String, BeanSpec> entry : spec.fields().entrySet()) {
                String fieldName = entry.getKey();
                BeanSpec fieldSpec = entry.getValue();

            }
        }
    }

    public void apply(Object host) {
        Object v = $.getProperty(host, name);
        if (null != v) {
            return;
        }
        if (null != loader) {
        }
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
}
