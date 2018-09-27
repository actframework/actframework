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
import act.app.App;
import act.cli.CliContext;
import act.cli.CliSession;
import act.inject.param.ScopeCacheSupport;
import org.osgl.http.H;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.ScopeCache;

public class SessionScope extends ScopeCacheSupport.Base implements ScopeCache.SessionScope, ScopeCacheSupport {

    public static final act.inject.genie.SessionScope INSTANCE = new act.inject.genie.SessionScope();
    private final int TTL;

    public SessionScope() {
        TTL = App.instance().config().sessionTtl();
    }

    @Override
    public <T> T get(BeanSpec target) {
        return get(target.toString());
    }

    @Override
    public <T> T get(String key) {
        ActionContext actionContext = ActionContext.current();
        if (null != actionContext) {
            H.Session session = actionContext.session();
            T t = session.cached(key);
            if (null != t) {
                session.cache(key, t, TTL);
            }
            return t;
        }
        CliContext cliContext = CliContext.current();
        if (null != cliContext) {
            CliSession cliSession = cliContext.session();
            return cliSession.attribute(key);
        }
        return null;
    }

    @Override
    public <T> void put(BeanSpec target, T t) {
        put(target.toString(), t);
    }

    @Override
    public <T> void put(String key, T t) {
        ActionContext actionContext = ActionContext.current();
        if (null != actionContext) {
            actionContext.session().cache(key, t, TTL);
        }
        CliContext cliContext = CliContext.current();
        if (null != cliContext) {
            CliSession cliSession = cliContext.session();
            cliSession.attribute(key, t);
        }
    }

}
