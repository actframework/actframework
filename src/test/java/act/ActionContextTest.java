package act;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import act.app.ActionContext;
import act.route.Router;
import org.junit.Before;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.util.C;

import java.util.Arrays;
import java.util.Map;

public class ActionContextTest extends ActTestBase {

    protected ActionContext ctx;

    @Before
    public void prepare() throws Exception {
        setup();
        H.Request req = mock(H.Request.class);
        when(req.method()).thenReturn(H.Method.GET);
        when(req.paramNames()).thenReturn(C.list("foo", "bar"));
        when(req.paramVal("foo")).thenReturn("FOO");
        when(req.paramVal("bar")).thenReturn("BAR");
        when(req.paramVals("foo")).thenReturn(new String[]{"FOO", "foo"});
        when(req.paramVals("bar")).thenReturn(new String[]{"BAR", "bar"});

        ActResponse resp = mock(ActResponse.class);
        ctx = ActionContext.create(mockApp, req, resp);
        ctx.router(mock(Router.class));
    }

    @Test
    public void addParamToContext() {
        ctx.param("zoo", "ZOO");
        eq("ZOO", ctx.paramVal("zoo"));
    }

    @Test
    public void fetchReqParamVal() {
        eq("FOO", ctx.paramVal("foo"));
    }

    @Test
    public void fetchReqParamVals() {
        yes(Arrays.equals(new String[]{"FOO", "foo"}, ctx.paramVals("foo")));
    }

    @Test
    public void fetchAllParamMap() {
        Map<String, String[]> params = ctx.allParams();
        yes(params.containsKey("foo"));
        yes(C.listOf(params.get("foo")).contains("FOO"));
        yes(C.listOf(params.get("foo")).contains("foo"));
        yes(params.containsKey("bar"));
        no(params.containsKey("zoo"));
    }

    @Test
    public void fetchAllParamMapWithExtraParamAdded() {
        Map<String, String[]> params = ctx.allParams();
        yes(params.containsKey("foo"));
        yes(params.containsKey("bar"));
        no(params.containsKey("zoo"));
        ctx.param("zoo", "ZOO");
        params = ctx.allParams();
        yes(params.containsKey("foo"));
        yes(params.containsKey("bar"));
        yes(params.containsKey("zoo"));
        yes(C.listOf(params.get("zoo")).contains("ZOO"));
    }

    @Test
    public void extraParamOverrideReqParam() {
        eq("FOO", ctx.paramVal("foo"));
        eq(2, ctx.paramVals("foo").length);
        ctx.param("foo", "BAR");
        eq("BAR", ctx.paramVal("foo"));
        eq(1, ctx.paramVals("foo").length);
    }
}
