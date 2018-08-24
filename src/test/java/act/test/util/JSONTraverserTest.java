package act.test.util;

/*-
 * #%L
 * ACT E2E Plugin
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

import static act.test.util.JSONTraverser.traverse;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.junit.Test;
import osgl.ut.TestBase;

public class JSONTraverserTest extends TestBase {

    public JSONObject simpleObj;
    public JSONArray simpleArray;

    public JSONObject fooBar;
    public JSONArray complexArray;

    @Before
    public void prepare() {
        simpleObj = new JSONObject();
        simpleObj.put("foo", "bar");
        simpleObj.put("bar", 10);

        simpleArray = new JSONArray();
        simpleArray.add("foo");
        simpleArray.add(10);

        fooBar = new JSONObject();
        JSONArray bar = new JSONArray();
        bar.add(0, simpleObj);
        fooBar.put("bar", bar);
        fooBar.put("foo", simpleArray);
        complexArray = new JSONArray();
        complexArray.add(0, fooBar);
    }

    @Test
    public void testSimpleObj() {
        eq("bar", traverse(simpleObj, "foo"));
        eq(10, traverse(simpleObj, "bar"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSimpleObjFail() {
        traverse(simpleObj, "foo.bar");
    }

    @Test
    public void testSimpleArray() {
        eq("foo", traverse(simpleArray, "0"));
        eq(10, traverse(simpleArray, "1"));
    }

    @Test
    public void testComplexCase() {
        eq("foo", traverse(complexArray, "0.foo[0]"));
        eq("bar", traverse(complexArray, "[0][bar][0][foo]"));
        eq(10, traverse(complexArray, "0.bar.0.bar"));
    }
}
