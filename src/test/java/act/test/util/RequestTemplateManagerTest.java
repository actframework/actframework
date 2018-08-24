package act.test.util;

/*-
 * #%L
 * ACT E2E Plugin
 * %%
 * Copyright (C) 2018 ActFramework
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

import act.test.RequestSpec;
import org.junit.Test;

public class RequestTemplateManagerTest extends TestTestBase {

    @Test
    public void test() {
        RequestTemplateManager manager = new RequestTemplateManager();
        manager.load();
        RequestSpec spec = manager.getTemplate("global");
        notNull(spec);
        eq("last|", spec.headers.get("Authorization"));
        isNull(spec.ajax);
        isNull(spec.json);
        spec = manager.getTemplate("ajax");
        notNull(spec);
        eq("last|", spec.headers.get("Authorization"));
        yes(spec.ajax);
        eq("json", spec.accept);
    }

}
