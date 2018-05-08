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

import act.Act;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.osgl.$;
import org.osgl.util.ValueObject;

import java.util.Map;

public abstract class MapCodec<T extends Map<String, Object>> implements ValueObject.Codec<T> {

    private Class<T> targetClass;

    public MapCodec(Class<T> targetClass) {
        this.targetClass = $.requireNotNull(targetClass);
    }

    @Override
    public Class<T> targetClass() {
        return targetClass;
    }

    @Override
    public T parse(String s) {
        JSONObject json = JSON.parseObject(s);
        T map = Act.app().getInstance(targetClass);
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            map.put(entry.getKey(), ValueObject.of(entry.getValue()));
        }
        return map;
    }

    @Override
    public String toString(T o) {
        return JSON.toJSONString(o);
    }

    @Override
    public String toJSONString(T o) {
        return toString(o);
    }

    public static class JsonObjectCodec extends MapCodec<JSONObject> {
        public JsonObjectCodec() {
            super(JSONObject.class);
        }
    }

    public static class GenericMapCodec extends MapCodec<Map<String, Object>> {
        public GenericMapCodec() {
            super(target());
        }

        private static Class<Map<String, Object>> target() {
            return $.cast(Map.class);
        }
    }

}
