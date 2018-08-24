package act.test.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2018 ActFramework
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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

public class JSONTraverser {

    private JSONObject obj;
    private JSONArray array;

    public JSONTraverser(Object o) {
        if (o instanceof JSONObject) {
            obj = (JSONObject) o;
        } else if (o instanceof JSONArray) {
            array = (JSONArray) o;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public Object traverse(String path) {
        String[] sa = path.split("[\\.\\[\\]]+");
        if (sa.length == 1) {
            if (obj != null) {
                return obj.get(path);
            } else {
                return array.get(Integer.parseInt(path));
            }
        }
        List<String> list = C.newListOf(sa);
        String first = list.remove(0);
        if ("".equalsIgnoreCase(first)) {
            first = list.remove(0);
        }
        Object o = traverse(first);
        JSONTraverser traverser = new JSONTraverser(o);
        String rest = S.join(list).by(".").get();
        return traverser.traverse(rest);
    }


    public static Object traverse(Object obj, String path) {
        return new JSONTraverser(obj).traverse(path);
    }

}
