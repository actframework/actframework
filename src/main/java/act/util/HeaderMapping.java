package act.util;

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

import org.osgl.util.*;

import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Specify header mapping when loading a list of data
 * into a field with {@link act.inject.util.LoadResource}.
 *
 * Note if the target field type is not a List of data, this
 * annotation will be ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HeaderMapping {

    /**
     * Specify header mapping.
     *
     * Example: `"# as id,姓 as lastName,名 as firstName"`
     */
    String value();

    class Parser {
        public static Map<String, String> parse(String value) {
            if (S.isBlank(value)) {
                return C.Map();
            }
            S.List list = S.fastSplit(value, ",");
            Map<String, String> map = new HashMap<>();
            for (String part : list) {
                part = part.trim();
                int pos = part.toLowerCase().indexOf(" as ");
                E.illegalArgumentIf(pos < 1, "Illegal mapping specification: " + part);
                String src = part.substring(0, pos);
                String tgt = part.substring(pos + 4);
                map.put(src, tgt);
            }
            return map;
        }
    }

}
