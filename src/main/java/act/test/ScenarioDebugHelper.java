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

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.handler.RequestHandlerBase;
import act.metric.Metric;
import act.metric.MetricPlugin;
import act.metric.Timer;
import act.sys.Env;
import act.util.Async;
import act.util.ProgressGauge;
import act.util.PropertySpec;
import com.alibaba.fastjson.*;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.mvc.result.Result;
import org.osgl.util.Keyword;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import static act.controller.Controller.Util.render;
import static act.metric.MetricInfo.ACT_TEST_INTERACTION;
import static act.metric.MetricInfo.ACT_TEST_SCENARIO;

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
        private Metric metric = Act.metricPlugin().metric(ACT_TEST_SCENARIO);

        public LoadFixtures(Test test) {
            this.test = test;
        }

        @Override
        public void handle(ActionContext context) {
            Timer timer = metric.startTimer("load-fixtures:handler");
            try {
                JSONObject json = JSON.parseObject(context.body());
                JSONArray fixtures = json.getJSONArray("fixtures");
                test.loadFixtures((List) fixtures);
                H.Response resp = context.resp();
                resp.status(H.Status.OK);
                resp.commit();
            } finally {
                timer.stop();
            }
        }

        @Override
        public void prepareAuthentication(ActionContext context) {

        }
    }

    public static class ClearFixtures extends RequestHandlerBase {
        private Test test;
        private Metric metric = Act.metricPlugin().metric(ACT_TEST_SCENARIO);

        public ClearFixtures(Test test) {
            this.test = test;
        }

        @Override
        public void handle(ActionContext context) {
            Timer timer = metric.startTimer("clear-fixtures:handler");
            try {
                test.clearFixtures();
                H.Response resp = context.resp();
                resp.status(H.Status.NO_CONTENT);
                resp.commit();
            } finally {
                timer.stop();
            }
        }

        @Override
        public void prepareAuthentication(ActionContext context) {

        }
    }


    @Inject
    private Test test;

    @GetAction({"e2e", "test", "tests"})
    public Result testForm(String partition, ActionContext context) {
        context.templatePath("/~test_async.html");
        return render(partition);
    }

    @PostAction({"e2e", "test", "tests"})
    @PropertySpec("name, ignore, source, status, issueUrl, title, errorMessage, interactions.status, interactions.description, interactions.stackTrace, interactions.errorMessage")
    @Async
    public List<Scenario> run(App app, String partition, ActionContext context, ProgressGauge gauge) {
        List<Scenario> results = test.run(app, null, partition, false, gauge);
        boolean failure = false;
        for (Scenario scenario : results) {
            if ($.not(scenario.ignore) && !scenario.status.pass()) {
                failure = true;
                break;
            }
        }
        context.renderArg("failure", failure);
        gauge.markAsDone();
        return results;
    }

    @GetAction({"e2e/{testId}", "test/{testId}", "tests/{testId}"})
    public List<Scenario> runTest(App app, Keyword testId, ActionContext context, ProgressGauge gauge) {
        if (context.accept().isSameTypeWith(H.Format.HTML)) {
            context.templatePath("/~test.html");
        }
        List<Scenario> results = test.run(app, testId, null, false, gauge);
        boolean failure = false;
        for (Scenario scenario : results) {
            if ($.not(scenario.ignore) && !scenario.status.pass()) {
                failure = true;
                break;
            }
        }
        context.renderArg("failure", failure);
        gauge.markAsDone();
        return results;
    }
}
