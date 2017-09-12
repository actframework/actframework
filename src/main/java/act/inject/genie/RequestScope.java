package act.inject.genie;

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
import act.cli.CliContext;
import act.inject.param.ScopeCacheSupport;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.ScopeCache;

public class RequestScope extends ScopeCacheSupport.Base implements ScopeCache.RequestScope, ScopeCacheSupport {

    public static final act.inject.genie.RequestScope INSTANCE = new act.inject.genie.RequestScope();

    @Override
    public <T> T get(BeanSpec target) {
        return get(target.toString());
    }

    @Override
    public <T> T get(String key) {
        ActionContext actionContext = ActionContext.current();
        if (null != actionContext) {
            return actionContext.attribute(key);
        }
        CliContext cliContext = CliContext.current();
        if (null != cliContext) {
            return cliContext.attribute(key);
        }
        return null;
    }

    @Override
    public <T> void put(BeanSpec target, T t) {
        if (null == t) {
            return;
        }
        put(target.toString(), t);
    }

    public <T> void put(String key, T t) {
        if (null == t) {
            return;
        }
        ActionContext actionContext = ActionContext.current();
        if (null != actionContext) {
            actionContext.attribute(key, t);
        }
        CliContext cliContext = CliContext.current();
        if (null != cliContext) {
            cliContext.attribute(key, t);
        }
    }
}
