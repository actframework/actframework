package act.inject.param;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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
import act.controller.ParamNames;
import act.util.ActContext;
import org.osgl.util.E;
import org.osgl.util.S;

public class PartialPathLoader extends ParamValueLoader.NonCacheable {

    private String bindName;

    public PartialPathLoader(String bindName) {
        this.bindName = bindName;
    }

    @Override
    public String toString() {
        return S.concat("partial path loader[", bindName(), "]");
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        E.illegalStateIfNot(context instanceof ActionContext, "Not in an HTTP request context");
        ActionContext actionContext = (ActionContext) context;
        return actionContext.__pathParamVal();
    }

    @Override
    public String bindName() {
        return bindName;
    }

}
