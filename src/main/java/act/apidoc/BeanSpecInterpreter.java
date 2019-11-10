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

import act.db.DbBind;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.joda.time.ReadableInstant;
import org.osgl.inject.BeanSpec;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

class BeanSpecInterpreter {
    String interpret(BeanSpec beanSpec) {
        if (beanSpec.hasAnnotation(DbBind.class)) {
            return "String";
        }
        Class<?> rawType = beanSpec.rawType();
        if (Collection.class.isAssignableFrom(rawType)) {
            List<Type> types = beanSpec.typeParams();
            if (types.size() > 0) {
                return interpret(BeanSpec.of(types.get(0), null, beanSpec.injector())) + "[]";
            }
            return "any[]";
        }
        if (Map.class.isAssignableFrom(rawType)) {
            List<Type> types = beanSpec.typeParams();
            if (types.size() > 1) {
                Type key = types.get(0);
                Type val = types.get(1);
                String keyStr = interpret(BeanSpec.of(key, null, beanSpec.injector()));
                String valStr = interpret(BeanSpec.of(val, null, beanSpec.injector()));
                if ("String".equals(keyStr) && "Object".equals(valStr)) {
                    return "js object";
                }
                return "map of (" + keyStr + ", " + valStr + ")";
            }
        }
        if (JSONObject.class.isAssignableFrom(rawType)) {
            return "JSON object";
        }
        if (JSONArray.class.isAssignableFrom(rawType)) {
            return "JSON array";
        }
        if (Number.class.isAssignableFrom(rawType)) {
            return "number";
        }
        if (Date.class.isAssignableFrom(rawType)) {
            return "datetime";
        }
        if (ReadableInstant.class.isAssignableFrom(rawType)) {
            return "datetime";
        }
        if (rawType.getName().contains("ObjectId")) {
            return "id (String)";
        }
        return rawType.getSimpleName();
    }
}
