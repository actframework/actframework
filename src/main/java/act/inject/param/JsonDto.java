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

import java.util.*;

/**
 * The base class of system generated data transfer object to support converting JSON string into
 * handler method parameters (or plus controller class fields)
 */
public abstract class JsonDto {

    public static final String CTX_ATTR_KEY = "__json_dto__";

    private Map<String, Object> beans = new HashMap<String, Object>();

    /**
     * Called by {@link ParamValueLoader} to get the bean
     * @param name the name of field or param
     * @return the bean
     */
    public Object get(String name) {
        return beans.get(name);
    }

    /**
     * Called by generated `setXxx(T bean)` method
     * @param name the name of the param or field
     * @param bean the bean instance
     */
    protected void set(String name, Object bean) {
        beans.put(name, bean);
    }

    /**
     * Get all bean objects deserialized into this DTO.
     * @return all bean objects
     */
    public Collection beans() {
        return beans.values();
    }
}
