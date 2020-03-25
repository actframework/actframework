package act.test;

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

import act.util.SingletonBase;
import org.osgl.inject.annotation.MapKey;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class TestEngineManager extends SingletonBase {

    @Inject
    @MapKey("name")
    private Map<String, TestEngine> engineLookup;

    public TestEngine getEngine(String name) {
        TestEngine engine = engineLookup.get(name);
        return null == engine ? engineLookup.get(DefaultTestEngine.NAME) : engine;
    }

    public void setupEngines() {
        for (TestEngine engine : engineLookup.values()) {
            engine.setup();
        }
    }

}
