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
import act.apidoc.Endpoint;
import act.app.App;
import act.app.DbServiceManager;
import act.app.event.SysEventId;
import act.db.Dao;
import act.db.DbService;
import act.event.EventBus;
import act.inject.DefaultValue;
import act.inject.param.NoBind;
import act.job.Job;
import act.job.JobManager;
import act.job.OnSysEvent;
import act.metric.MeasureTime;
import act.metric.MetricInfo;
import act.sys.Env;
import act.test.func.Func;
import act.test.macro.Macro;
import act.test.req_modifier.RequestModifier;
import act.test.util.*;
import act.test.verifier.Verifier;
import act.util.*;
import act.util.ProgressGauge;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.fusesource.jansi.Ansi;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.annotation.DeleteAction;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.*;

import javax.inject.Inject;
import javax.persistence.MappedSuperclass;
import java.io.File;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Env.RequireMode(Act.Mode.DEV)
@Stateless
public class Test extends LogSupport {

    public static final String PG_PAYLOAD_SCENARIO = "scenario";
    public static final String PG_PAYLOAD_INTERACTION = "interaction";
    public static final String PG_PAYLOAD_FAILED = "failed";

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    /**
     * keep the ID of the email been sent last time when E2E is in progress
     */
    private static String lastEmailId;

    public static class ConstantPool {

        private static Map<String, String> pool = new HashMap<>();

        static {
            load();
        }

        private static void load() {
            Properties p = null;
            File file = Act.app().testResource("constants.properties");
            if (file.exists()) {
                p = IO.loadProperties(file);
            } else {
                URL url = Act.getResource("test/constants.properties");
                if (null != url) {
                    p = IO.loadProperties(url);
                }
            }
            if (null != p) {
                for (Map.Entry entry : p.entrySet()) {
                    String key = S.string(entry.getKey());
                    key = S.underscore(key);
                    pool.put(key, S.string(entry.getValue()));
                }
            }
        }

        public static String get(String name) {
            return pool.get(S.underscore(name));
        }
    }

    @Inject
    private DbServiceManager dbServiceManager;

    @Inject
    private YamlLoader yamlLoader;

    @Inject
    private JobManager jobManager;

    @NoBind
    private List<Scenario> result;

    @NoBind
    private Throwable error;

    @NoBind
    public ProgressGauge gauge;

    /**
     * Load fixture data for testing.
     *
     * Requested by test executor and executed on test host to
     * setup testing data
     */
    @PostAction({"e2e/fixtures", "test/fixtures"})
    @MeasureTime(MetricInfo.ACT_TEST_HELPER + ":load-fixtures")
    public void loadFixtures(List<String> fixtures) {
        for (String fixture : fixtures) {
            Job fixtureLoader = jobManager.jobById(fixture, false);
            if (null != fixtureLoader) {
                fixtureLoader.run();
            } else {
                yamlLoader.loadFixture(fixture, dbServiceManager);
            }
        }
    }

    /**
     * Generate testing data for specified model types
     * @param modelType
     *      the model type
     * @param number
     *      the number of records to be generated
     */
    @PostAction({"e2e/generateTestData", "test/generateTestData"})
    @MeasureTime(MetricInfo.ACT_TEST_HELPER + ":generate-sample-data")
    public void generateSampleData(String modelType, @DefaultValue("100") Integer number) {
        E.illegalArgumentIf(number < 1);
        Class<?> modelClass = null;
        if (modelType.contains(".")) {
            modelClass = Act.appClassForName(modelType);
        } else {
            String modelPackages = Act.appConfig().get("test.model-packages");
            E.illegalArgumentIf(S.blank(modelPackages), "Unknown model type: " + modelType);
            for (String pkg : S.fastSplit(modelPackages, ",")) {
                String type = S.concat(pkg, ".", modelType);
                try {
                    modelClass = Act.appClassForName(type);
                } catch (Exception e) {
                    // ignore
                }
            }
            E.illegalArgumentIf(null == modelClass, "Unknown model type: " + modelType);
        }
        Dao dao = dbServiceManager.dao(modelClass);
        E.illegalStateIf(null == dao);
        List list = new ArrayList();
        for (int i = 0; i < number; ++i) {
            list.add(generateSampleData_(modelClass));
        }
        dao.save(list);
    }

    /**
     * Clear fixture data.
     *
     * Requested by test executor and executed on test host to
     * setup testing data
     */
    @DeleteAction({"e2e/fixtures", "test/fixtures"})
    @MeasureTime(MetricInfo.ACT_TEST_HELPER + ":clear-fixtures")
    public void clearFixtures() {
        List<Dao> toBeDeleted = new ArrayList<>();
        for (DbService svc : dbServiceManager.registeredServices()) {
            for (Class entityClass : svc.entityClasses()) {
                if (Modifier.isAbstract(entityClass.getModifiers())) {
                    continue;
                }
                if (entityClass.isAnnotationPresent(NotFixture.class) || entityClass.isAnnotationPresent(MappedSuperclass.class)) {
                    continue;
                }
                try {
                    Dao dao = dbServiceManager.dao(entityClass);
                    if (dao.getClass().isAnnotationPresent(NotFixture.class)) {
                        continue;
                    }
                    toBeDeleted.add(dao);
                } catch (IllegalArgumentException e) {
                    if (e.getMessage().contains("Cannot find out Dao for model type")) {
                        // ignore - must be caused by MappedSuperClass
                        debug(e, "error getting dao for %s", entityClass);
                        continue;
                    }
                }
            }
        }
        /*
         * The following logic is to deal with the case where two
         * models have relationship, and the one that is not the owner
         * has been called to delete first, in which case it will
         * fail because reference exists in other table(s). so
         * we want to ignore that case and keep removing other tables.
         * Hopefully when the owner model get removed eventually and
         * back to the previous model, it will be good to go.
         */
        int count = toBeDeleted.size();
        while (!toBeDeleted.isEmpty() && count-- > 0) {
            List<Dao> list = new ArrayList<>(toBeDeleted);
            for (Dao dao : list) {
                try {
                    TxScope.enter();
                    dao.drop();
                    try {
                        TxScope.commit();
                    } catch (Exception e) {
                        warn(e, "error drop dao");
                    }
                    toBeDeleted.remove(dao);
                } catch (Exception e) {
                    trace(e, "error drop dao");
                } finally {
                    TxScope.clear();
                }
            }
        }
    }

    // ensure automated test start after three events:
    // 1. ACT_START
    // 2. POST_START
    // 3. DB_SVC_LOADED
    @OnSysEvent(SysEventId.ACT_START)
    public void run(final App app) {
        Object o = app.config().get("test.delay");
        final $.Var<Long> delay = $.var(0l);
        if (null != o) {
            delay.set($.convert(o).to(Long.class));
        }
        boolean run = shallRunAutomatedTest(app);
        if (run) {
            app.jobManager().post(SysEventId.POST_STARTED, new Runnable() {
                @Override
                public void run() {
                    final ProgressGauge gauge = new SimpleProgressGauge();
                    if (delay.get() > 0l) {
                        app.jobManager().delay(new Runnable() {
                            @Override
                            public void run() {
                                Test.this.run(app, null, null, true, gauge);
                                gauge.markAsDone();
                            }
                        }, delay.get(), TimeUnit.SECONDS);
                    } else {
                        app.jobManager().now(new Runnable() {
                            @Override
                            public void run() {
                                Test.this.run(app, null, null, true, gauge);
                                gauge.markAsDone();
                            }
                        });
                    }
                }
            });
        }
    }

    public static boolean shallRunAutomatedTest(App app) {
        return $.bool(app.config().get("test.run")) || $.bool(app.config().get("e2e.run")) || "test".equalsIgnoreCase(Act.profile()) || "e2e".equalsIgnoreCase(Act.profile());
    }

    @GetAction("test/result")
    @PropertySpec("error, scenario.partition, scenarios.name, scenarios.ignoreReason, scenarios.ignore, scenarios.source, scenarios.status, " +
            "scenarios.issueUrl, scenarios.issueUrlIcon, scenarios.title, scenarios.errorMessage, " +
            "scenarios.interactions.status, scenarios.interactions.description, " +
            "scenarios.interactions.stackTrace, scenarios.interactions.errorMessage")
    public Map<String, Object> result() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("scenarios", result);
        Map<String, String> err = new HashMap<>();
        if (null != this.error) {
            err.put("message", this.error.getMessage());
            err.put("stackTrace", E.stackTrace(this.error));
            ret.put("error", err);
        }
        return ret;
    }

    public List<Scenario> run(App app, Keyword testId, String partition, boolean shutdownApp, ProgressGauge gauge) {
        E.illegalStateIf(inProgress());
        info("Start running test scenarios");
        info("---------------------------------------------------------------");
        YamlLoader.resetWarned();
        int exitCode = 0;
        EventBus eventBus = app.eventBus();
        STARTED.set(true);
        this.error = null;
        this.result = C.list();
        this.gauge = gauge;
        try {
            eventBus.trigger(TestStart.INSTANCE);
            app.captchaManager().disable();
            registerTypeConverters();
            RequestTemplateManager requestTemplateManager = new RequestTemplateManager();
            requestTemplateManager.load();
            final ScenarioManager scenarioManager = new ScenarioManager();
            Map<String, Scenario> scenarios = scenarioManager.load();
            List<Scenario> list;
            if (scenarios.isEmpty()) {
                warn("No scenario defined.");
                list = C.list();
            } else {
                list = new ArrayList<>();
                ProgressBar pb = null;
                boolean pbStarted = false;
                if (null == testId && shutdownApp && Banner.supportAnsi()) {
                    String label = "Testing";
                    pb = new ProgressBar(label, gauge.maxHint(), 200, System.out, ProgressBarStyle.UNICODE_BLOCK);
                }
                List<Scenario> candidates = new ArrayList<>(scenarios.size());
                for (Scenario scenario : scenarios.values()) {
                    if (null != testId && $.ne(testId, Keyword.of(scenario.name))) {
                        continue;
                    }
                    if (S.notBlank(partition) && S.neq(partition, scenario.partition)) {
                        continue;
                    }
                    if (scenario.interactions.isEmpty()) {
                        continue;
                    }
                    if (!candidates.contains(scenario)) {
                        candidates.add(scenario);
                    }
                }
                // some scenarios might be run multiple times as they are dependencies.
                List<Scenario> toBeRemoved = new ArrayList<>(candidates.size());
                for (Scenario scenario : candidates) {
                    for (Scenario other : candidates) {
                        if (other == scenario) {
                            continue;
                        }
                        if (other.allDepends.contains(scenario)) {
                            toBeRemoved.add(scenario);
                        }
                    }
                }
                candidates.removeAll(toBeRemoved);
                Collections.sort(candidates, new ScenarioComparator(false));
                gauge.updateMaxHint(candidates.size() + 1);
                for (Scenario scenario : candidates) {
                    if (null != testId) {
                        scenario.ignore = null;
                    }
                    if ($.not(scenario.ignore)) {
                        if (S.neq(Act.profile(), "test")) {
                            info("running [%s]%s", scenario.partition, scenario.name);
                        }
                        try {
                            new TestSession(scenario, requestTemplateManager).run(gauge);
                        } catch (Exception e) {
                            gauge.setPayload(PG_PAYLOAD_FAILED, true);
                            String message = e.getMessage();
                            scenario.errorMessage = S.blank(message) ? e.getClass().getName() : message;
                            scenario.cause = e.getCause();
                            scenario.status = TestStatus.FAIL;
                        }
                    }
                    gauge.step();
                    addToList(scenario, list, scenarioManager);
                    for (Scenario dep : scenario.allDepends) {
                        addToList(dep, list, scenarioManager);
                    }
                    if (null != pb) {
                        if (!pbStarted) {
                            pb.start();
                            pbStarted = true;
                        }

                        pb.stepTo(gauge.currentSteps());
                        System.out.flush();
                    }
                }
                if (null != pb) {
                    pb.stepTo(pb.getMax());
                    pb.stop();
                    System.out.println();
                }
            }
            if (shutdownApp) {
                for (Scenario scenario : list) {
                    if ($.not(scenario.ignore) && !scenario.status.pass()) {
                        exitCode = -1;
                    }
                    output(scenario);
                }
            }
            Collections.sort(list, new ScenarioComparator(true));
            if (shutdownApp) {
                boolean ansi = Banner.supportAnsi();
                String msg = ansi ? Ansi.ansi().render("@|bold FAILED/IGNORED SCENARIOS:|@").toString() : "FAILED/IGNORED SCENARIOS:";
                info("================================================================================");
                info(msg);
                info("--------------------------------------------------------------------------------");
                for (Scenario scenario : list) {
                    if (scenario.status == TestStatus.FAIL) {
                        info("--------------------------------------------------------------------------------");
                        logError(ansi, "[failed] %s", scenario.title());
                        if (S.notBlank(scenario.errorMessage)) {
                            logError(ansi, scenario.errorMessage);
                        }
                        if (null != scenario.cause) {
                            logError(ansi, "cause: \n" + E.stackTrace(scenario.cause));
                        }
                    } else if ($.bool(scenario.ignore)) {
                        if ("true".equalsIgnoreCase(scenario.ignore)) {
                            logIgnore(ansi, "[ignored] %s", scenario.title());
                        } else {
                            logIgnore(ansi, "[ignored] %s\n\t - %s", scenario.title(), scenario.getIgnoreReason());
                        }
                    }
                }
                info("================================================================================");
                info("");
            }
            this.result = list;
            return list;
        } catch (Exception e) {
            this.error = e;
            exitCode = -1;
            throw e;
        } finally {
            this.gauge = null;
            STARTED.set(false);
            if (shutdownApp) {
                app.shutdown(exitCode);
            } else {
                app.captchaManager().enable();
            }
            eventBus.trigger(TestStop.INSTANCE);
        }
    }

    private void logError(boolean ansi, String msg, Object... args) {
        msg = S.fmt(msg, args);
        if (ansi) {
            msg = "@|red " + msg + "|@";
            msg = Ansi.ansi().render(msg).toString();
        }
        error(msg);
    }

    private void logIgnore(boolean ansi, String msg, Object... args) {
        msg = S.fmt(msg, args);
        if (ansi) {
            msg = "@|faint " + msg + "|@";
            msg = Ansi.ansi().render(msg).toString();
        }
        warn(msg);
    }

    private void output(Scenario scenario) {
        if ($.bool(scenario.ignore)) {
            return;
        }
        printBanner(scenario);
        printInteractions(scenario);
        printFooter();
    }

    private void printBanner(Scenario scenario) {
        printDoubleDashedLine();
        info(scenario.title());
        printDashedLine();
    }

    private void printInteractions(Scenario scenario) {
        boolean ansi = Banner.supportAnsi();
        for (Interaction interaction : scenario.interactions) {
            String msg = S.concat("[", interaction.status, "]", interaction.description);
            if (ansi) {
                String color = interaction.status == TestStatus.PASS ? "green" : "red";
                msg = "@|" + color + " " + msg + "|@";
                msg = Ansi.ansi().render(msg).toString();
            }
            info(msg);
        }
    }

    private void printFooter() {
        println();
    }

    private Object generateSampleData_(Class<?> modelType) {
        BeanSpec spec = BeanSpec.of(modelType, Act.injector());
        return Endpoint.generateSampleData(spec, C.<String, Class>Map(), new HashSet<Type>(), new ArrayList<String>(), null, false);
    }

    public static String constant(String name) {
        return ConstantPool.get(name);
    }

    public static void registerTypeConverters() {
        Verifier.registerTypeConverters();
        Macro.registerTypeConverters();
        RequestModifier.registerTypeConverters();
        Func.registerTypeConverters();
    }

    public static boolean inProgress() {
        return STARTED.get();
    }

    public static String generateEmailId() {
        if (!Test.inProgress()) {
            return null;
        }
        lastEmailId = S.random(8);
        return lastEmailId;
    }

    private static void addToList(Scenario scenario, List<Scenario> list, ScenarioManager manager) {
        for (String s : scenario.depends) {
            Scenario dep = manager.get(s);
            addToList(dep, list, manager);
        }
        if (!list.contains(scenario)) {
            list.add(scenario);
        }
    }

}
