package act.test;

/*-
 * #%L
 * ACT Framework
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

import static act.controller.Controller.Util.renderTemplate;

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.handler.RequestHandlerBase;
import act.sys.Env;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Allows debug scenario file via:
 * 1. provide a command to launch test
 * 2. register test built-in services even when it is not in test profile
 */
@Singleton
@Env.RequireMode(Act.Mode.DEV)
public class ScenarioDebugHelper {

    public static class LoadFixtures extends RequestHandlerBase {

        private Test test;

        public LoadFixtures(Test test) {
            this.test = test;
        }

        @Override
        public void handle(ActionContext context) {
            JSONObject json = JSON.parseObject(context.body());
            JSONArray fixtures = json.getJSONArray("fixtures");
            test.loadFixtures((List) fixtures);
            H.Response resp = context.resp();
            resp.status(H.Status.OK);
            resp.commit();
        }

        @Override
        public void prepareAuthentication(ActionContext context) {

        }
    }

    public static class ClearFixtures extends RequestHandlerBase {
        private Test test;

        public ClearFixtures(Test test) {
            this.test = test;
        }

        @Override
        public void handle(ActionContext context) {
            test.clearFixtures();
            H.Response resp = context.resp();
            resp.status(H.Status.NO_CONTENT);
            resp.commit();
        }

        @Override
        public void prepareAuthentication(ActionContext context) {

        }
    }


    @Inject
    private Test test;

    @GetAction({"e2e", "test"})
    public void run(App app) {
        List<Scenario> scenarios = test.run(app, false);
        renderTemplate("/~test.html", scenarios);
    }

}
