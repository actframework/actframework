package act.cli;

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
import org.osgl.http.H;

/**
 * The CliOverHttpSession
 */
public class CliOverHttpSession extends CliSession {
    private H.Session session;

    public CliOverHttpSession(ActionContext context) {
        super(context);
        session = context.session();
    }

    @Override
    public CliSession attribute(String key, Object val) {
        session.cache(key, val, app.config().sessionTtl());
        return this;
    }

    @Override
    public <T> T attribute(String key) {
        return session.cached(key);
    }

    @Override
    public CliSession removeAttribute(String key) {
        session.evict(key);
        return this;
    }
}
