package act.controller.meta;

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

import org.osgl.$;
import org.osgl.util.C;

import java.util.HashMap;
import java.util.Map;

public class ParamAnnoInfo extends ParamAnnoInfoTraitBase {
    private String bindName = "";
    private Map<Class, Object> defValMap = new HashMap<>();

    public ParamAnnoInfo(int index) {
        super(index);
    }

    @Override
    public void attachTo(HandlerParamMetaInfo param) {
        param.paramAnno(this);
    }

    public ParamAnnoInfo bindName(String name) {
        this.bindName = name;
        return this;
    }
    public String bindName() {
        return bindName;
    }
    public ParamAnnoInfo defVal(Class<?> type, Object val) {
        defValMap.put(type, val);
        return this;
    }
    public <T> T defVal(Class<?> type) {
        if (primitiveTypes.containsKey(type)) {
            type = primitiveTypes.get(type);
        }
        Object v = defValMap.get(type);
        if (null == v) return null;
        return $.cast(v);
    }

    private static Map<Class, Class> primitiveTypes = C.Map(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            short.class, Short.class,
            char.class, Character.class,
            int.class, Integer.class,
            float.class, Float.class,
            long.class, Long.class,
            double.class, Double.class
    );

}
