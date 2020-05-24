package act.inject.param;

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
import act.data.Sensitive;
import act.util.ActContext;
import act.validation.Password;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.inject.InjectException;
import org.osgl.util.S;

import java.lang.reflect.Field;

/**
 * Load instance loaded by {@link ParamValueLoader} into {@link java.lang.reflect.Field}
 */
class FieldLoader {
    private final Field field;
    private final Class<?> fieldType;
    private final boolean isString;
    private final ParamValueLoader loader;
    private final ParamValueLoader stringValueLoader;
    private final boolean isSensitive;
    private final boolean isPassword;
    private Lang.TypeConverter<Object, Object> converter;

    FieldLoader(Field field, ParamValueLoader loader, ParamValueLoader stringValueLoader, Lang.TypeConverter<Object, Object> converter) {
        Class<?> type = field.getType();
        boolean isString = String.class == type;
        boolean isCharArray = char[].class == type;
        this.isString = isString;
        this.isSensitive = isString && null != field.getAnnotation(Sensitive.class);
        this.isPassword = (isString || isCharArray) && null != field.getAnnotation(Password.class);
        this.field = field;
        this.fieldType = type;
        this.loader = $.requireNotNull(loader);
        this.stringValueLoader = stringValueLoader;
        this.converter = converter;
    }

    FieldLoader(Field field, ParamValueLoader loader) {
        this(field, loader, null, null);
    }

    public void applyTo($.Func0<Object> beanSource, ActContext context) {
        Object fieldValue = loader.load(null, context, true);
        if (null == fieldValue && null != converter) {
            // try converter
            Object o = stringValueLoader.load(null, context, true);
            if (null != o) {
                fieldValue = converter.convert(o);
            }
        }
        // #429 ensure POJO instance get initialized
        if (null == fieldValue) {
            // counter effect to #429 - We don't want to leave an empty reference for JPA model entities
            //beanSource.apply();
            // #689 initialize field if it is an array or a container
            if (fieldType.isArray()) {
                return;
            }
            return;
        }
        try {
            if (isSensitive) {
                fieldValue = Act.crypto().encrypt((String)fieldValue);
            } else if (isPassword) {
                if (isString) {
                    fieldValue = Act.crypto().passwordHash((String) fieldValue);
                } else {
                    char[] ca = $.convert(fieldValue).to(char[].class);
                    fieldValue = Act.crypto().passwordHash(ca);
                }
            }
            Object bean = beanSource.apply();
            field.set(bean, fieldValue);
        } catch (Exception e) {
            throw new InjectException(e);
        }
    }

    @Override
    public String toString() {
        return S.concat("field loader|", loader);
    }
}
