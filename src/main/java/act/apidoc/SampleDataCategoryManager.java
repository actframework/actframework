package act.apidoc;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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
import org.osgl.inject.annotation.MapKey;
import org.osgl.util.E;
import org.osgl.util.Keyword;
import org.osgl.util.S;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class SampleDataCategoryManager {

    @Inject
    @MapKey("name")
    private Map<Keyword, ISampleDataCategory> registry;

    public void register(ISampleDataCategory category) {
        String name = category.name();
        E.illegalArgumentIf(registry.containsKey(name), "Sample data category[%s] already registered", name);
        registry.put(Keyword.of(name), category);
    }

    public ISampleDataCategory getCategory(String name) {
        return registry.get(Keyword.of(name));
    }

    public static ISampleDataCategory get(String name) {
        if (S.blank(name)) {
            return null;
        }
        return Act.getInstance(SampleDataCategoryManager.class).getCategory(name);
    }

}
