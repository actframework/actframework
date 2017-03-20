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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.ValueObject;

import java.util.List;

public class ListCodec implements ValueObject.Codec<List<Object>> {

    @Override
    public Class<List<Object>> targetClass() {
        return $.cast(List.class);
    }

    @Override
    public List<Object> parse(String s) {
        JSONArray array = JSON.parseArray(s);
        List<Object> list = C.newSizedList(array.size());
        for (Object o : array) {
            list.add(ValueObject.of(o));
        }
        return list;
    }

    @Override
    public String toString(List<Object> o) {
        return JSON.toJSONString(o);
    }

    @Override
    public String toJSONString(List<Object> o) {
        return toString(o);
    }
}
