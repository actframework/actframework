package testapp.controller;

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

import act.app.ActAppException;
import act.app.ActionContext;
import org.osgl.mvc.annotation.*;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;
import testapp.util.Trackable;

import static org.osgl.http.H.Method.GET;
import static org.osgl.http.H.Method.PUT;

/**
 * A faked controller class with AppContext field
 */
@With({FilterA.class, FilterB.class, FilterC.class})
public class WithAppContext extends Trackable {
    private ActionContext ctx;

    @Catch(ActAppException.class)
    public void handle(ActAppException e, ActionContext ctx) {
        track("handle");
    }

    @Before
    public void setup() {
        track("setup");
    }

    @After
    public void after() {
        track("after");
    }

    @Finally
    public void teardown() {
        track("teardown");
    }

    @Action(value = "/no_ret_no_param", methods = {GET, PUT})
    public void noReturnNoParam() {
        track("noReturnNoParam");
    }

    @GetAction("/static_no_ret_no_param")
    public static String staticReturnStringNoParam() {
        return "foo";
    }

    public Result foo() {
        return Ok.get();
    }

    public static void bar() {}
}
