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

import act.Act;
import act.metric.MetricPlugin;
import act.test.*;
import act.test.macro.Macro;
import act.test.req_modifier.RequestModifier;
import act.test.verifier.*;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.mockito.Mockito;
import org.osgl.$;
import org.osgl.http.H;

import java.util.Map;

public class ScenarioManagerTest extends TestTestBase {

    private ScenarioManager loader = new ScenarioManager("act.test.model");

    @BeforeClass
    public static void prepare() {
        new Contains().register();
        new Eq().register();
        new Exists().register();
        new Gt().register();
        Test.registerTypeConverters();
        RequestModifier.registerModifiers();
        Macro.registerActions();
        MetricPlugin metricPlugin = Mockito.mock(MetricPlugin.class);
        $.setField("metricPlugin", Act.class, metricPlugin);
    }

    @org.junit.Test
    @Ignore // ResponseSpec now extends from AdaptiveBeanBase which require ActFramework to run up
    public void test() {
        Map<String, Scenario> map = loader.load();
        no(map.isEmpty());
        Scenario createTask = map.get("create-task");
        verifyCreateTask(createTask);
        String s = createTask.depends.get(0);
        Scenario signIn = loader.get(s);
        notNull(signIn);
        s = signIn.depends.get(0);
        Scenario signUp = loader.get(s);
        verifySignUp(signUp);
    }

    private void verifySignUp(Scenario signUp) {
        notNull(signUp);
        eq(1, signUp.fixtures.size());
        String fixture = signUp.fixtures.get(0);
        eq("init-data.yml", fixture);
        eq(1, signUp.interactions.size());
        Interaction interaction = signUp.interactions.get(0);
        notNull(interaction);
        RequestSpec req = interaction.request;
        eq(2, req.modifiers.size());
        RequestModifier json = req.modifiers.get(0);
        eq("accept-json", json.toString());
        RequestModifier ip = req.modifiers.get(1);
        eq("remote-address: 127.0.0.2", ip.toString());
        eq(H.Method.POST, req.method);
        eq("/sign_up", req.url);
        eq(3, req.params.size());
        Map<String, Object> params = req.params;
        eq("test@123.com", params.get("email"));
        eq("abc", params.get("password"));
        eq(1, params.get("value"));
        ResponseSpec resp = interaction.response;
        eq(H.Status.CREATED, resp.status);
    }

    private void verifyCreateTask(Scenario createTask) {
        notNull(createTask);
    }

}
