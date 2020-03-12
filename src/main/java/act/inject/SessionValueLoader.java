package act.inject;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2020 ActFramework
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

import act.apidoc.ApiManager;
import act.app.ActionContext;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.ValueLoader;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Map;

public class SessionValueLoader implements ValueLoader<Object> {

    private String sessionKey;
    private Class<?> targetType;

    @Override
    public void init(Map<String, Object> map, BeanSpec beanSpec) {
        this.sessionKey = (String) map.get("value");
        if (S.blank(this.sessionKey)) {
            this.sessionKey = beanSpec.name();
        }
        E.unexpectedIf(S.blank(this.sessionKey), "session key not found");
        this.targetType = beanSpec.rawType();
    }

    @Override
    public Object get() {
        if (ApiManager.inProgress()) {
            return null;
        }
        ActionContext ctx = ActionContext.current();
        E.illegalStateIf(null == ctx, "Not in request handling context");
        return $.convert(ctx.session(sessionKey)).to(targetType);
    }
}
