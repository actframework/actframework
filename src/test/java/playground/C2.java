package playground;

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

import act.app.ActionContext;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.result.Result;

import javax.inject.Named;

public class C2 extends CBase {

    @Before(priority =  5, except = "doAnn")
    public void before() {
    }

    private boolean cond1() {
        return true;
    }

    public void foo(String id, String email, ActionContext ctx) {
        if (cond1()) {
            ctx.param("id", id);
            ctx.param("email", email);
            render(id, email);
        } else if (id.hashCode() < 10) {
            ctx.param("id", id);
            render(id);
        } else {
            throw render(email);
        }
    }

    public Result bar() {
        return ok();
    }

    public void foo(@Named("abc") String x, @Named("xyz") int i) {

    }
}
