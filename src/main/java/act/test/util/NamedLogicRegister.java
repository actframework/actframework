package act.test.util;

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

import act.Act;
import act.util.SubClassFinder;
import org.osgl.util.E;
import org.osgl.util.Keyword;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;

@Singleton
public class NamedLogicRegister {

    private Map<Class<? extends NamedLogic>, Map<Keyword, NamedLogic>> registry = new HashMap<>();

    @SubClassFinder
    public void doRegister(NamedLogic logic) {
        logic.register();
    }

    void register(Keyword keyword, NamedLogic logic, boolean force) {
        Class<? extends NamedLogic> type = logic.type();
        Map<Keyword, NamedLogic> lookup = registry.get(type);
        if (null == lookup) {
            lookup = new HashMap<>();
            registry.put(type, lookup);
        }
        NamedLogic existing = lookup.put(keyword, logic);
        E.unexpectedIf(!force && null != existing && logic != existing, "Keyword already used: " + keyword.hyphenated());
    }

    static <T extends NamedLogic> T get(Class<? extends NamedLogic> logicType, String name) {
        NamedLogicRegister register = Act.getInstance(NamedLogicRegister.class);
        Map<Keyword, NamedLogic> lookup = register.registry.get(logicType);
        if (null == lookup) {
            return null;
        }
        return (T) lookup.get(Keyword.of(name));
    }

}
