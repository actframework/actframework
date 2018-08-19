package act.e2e;

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
import act.apidoc.Endpoint;
import act.app.App;
import act.app.DbServiceManager;
import act.db.Dao;
import act.db.DbService;
import act.e2e.func.Func;
import act.e2e.macro.Macro;
import act.e2e.req_modifier.RequestModifier;
import act.e2e.util.*;
import act.e2e.verifier.Verifier;
import act.event.EventBus;
import act.inject.DefaultValue;
import act.job.OnAppStart;
import act.sys.Env;
import act.util.LogSupport;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.annotation.DeleteAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.*;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

@Env.RequireMode(Act.Mode.DEV)
public class E2E extends LogSupport {


    static final Logger LOGGER = LogManager.get(E2E.class);

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
            URL url = Act.getResource("e2e/constants.properties");
            if (null != url) {
                Properties p = IO.loadProperties(url);
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

    /**
     * Load fixture data for testing.
     *
     * Requested by e2e executor and executed on test host to
     * setup testing data
     */
    @PostAction("e2e/fixtures")
    public void loadFixtures(List<String> fixtures) {
        for (String fixture : fixtures) {
            yamlLoader.loadFixture(fixture, dbServiceManager);
        }
    }

    /**
     * Generate testing data for specified model types
     * @param modelType
     *      the model type
     * @param number
     *      the number of records to be generated
     */
    @PostAction("e2e/generateTestData")
    public void generateSampleData(String modelType, @DefaultValue("100") Integer number) {
        E.illegalArgumentIf(number < 1);
        Class<?> modelClass = null;
        if (modelType.contains(".")) {
            modelClass = Act.appClassForName(modelType);
        } else {
            String modelPackages = Act.appConfig().get("e2e.model-packages");
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
     * Requested by e2e executor and executed on test host to
     * setup testing data
     */
    @DeleteAction("e2e/fixtures")
    public void clearFixtures() {
        List<Dao> toBeDeleted = new ArrayList<>();
        for (DbService svc : dbServiceManager.registeredServices()) {
            for (Class entityClass : svc.entityClasses()) {
                try {
                    toBeDeleted.add(dbServiceManager.dao(entityClass));
                } catch (IllegalArgumentException e) {
                    if (e.getMessage().contains("Cannot find out Dao for model type")) {
                        // ignore - must be caused by MappedSuperClass
                        logger.debug(e, "error getting dao for %s", entityClass);
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
        int count = 1000;
        while (!toBeDeleted.isEmpty() && count-- > 0) {
            List<Dao> list = new ArrayList<>(toBeDeleted);
            for (Dao dao : list) {
                try {
                    TxScope.enter();
                    dao.drop();
                    try {
                        TxScope.commit();
                    } catch (Exception e) {
                        continue;
                    }
                    toBeDeleted.remove(dao);
                } catch (Exception e) {
                    // ignore and try next dao
                } finally {
                    TxScope.clear();
                }
            }
        }
    }

    // wait 1 seconds to allow app setup the network
    @OnAppStart(delayInSeconds = 1)
    public void run(App app) {
        boolean run = $.bool(app.config().get("e2e.run")) || "e2e".equalsIgnoreCase(Act.profile());
        if (run) {
            run(app, true);
        }
    }

    public List<Scenario> run(App app, boolean shutdownApp) {
        E.illegalStateIf(inProgress());
        info("Start running E2E test scenarios\n");
        int exitCode = 0;
        EventBus eventBus = app.eventBus();
        STARTED.set(true);
        try {
            eventBus.trigger(E2EStart.INSTANCE);
            app.captchaManager().disable();
            registerTypeConverters();
            RequestTemplateManager requestTemplateManager = new RequestTemplateManager();
            requestTemplateManager.load();
            final ScenarioManager scenarioManager = new ScenarioManager();
            Map<String, Scenario> scenarios = scenarioManager.load();
            if (scenarios.isEmpty()) {
                LOGGER.warn("No scenario defined.");
            } else {
                C.List<Scenario> list = C.list(scenarios.values()).sorted(new ScenarioComparator(scenarioManager));
                for (Scenario scenario : list) {
                    try {
                        scenario.start(scenarioManager, requestTemplateManager);
                    } catch (Exception e) {
                        String message = e.getMessage();
                        scenario.errorMessage = S.blank(message) ? e.getClass().getName() : message;
                        scenario.cause = e.getCause();
                        scenario.status = E2EStatus.FAIL;
                    }
                }
            }
            List<Scenario> list = new ArrayList<>();
            for (Scenario scenario : scenarios.values()) {
                addToList(scenario, list, scenarioManager);
            }
            if (shutdownApp) {
                for (Scenario scenario : list) {
                    if (!scenario.status.pass()) {
                        exitCode = -1;
                    }
                    output(scenario);
                }
            }
            return list;
        } catch (Exception e) {
            exitCode = -1;
            throw e;
        } finally {
            STARTED.set(false);
            if (shutdownApp) {
                app.shutdown(exitCode);
            } else {
                app.captchaManager().enable();
            }
            eventBus.trigger(E2EStop.INSTANCE);
        }
    }

    private void output(Scenario scenario) {
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
        for (Interaction interaction : scenario.interactions) {
            String msg = S.concat("[", interaction.status, "]", interaction.description);
            info(msg);
        }
    }

    private void printFooter() {
        println();
    }

    private Object generateSampleData_(Class<?> modelType) {
        BeanSpec spec = BeanSpec.of(modelType, Act.injector());
        return Endpoint.generateSampleData(spec, C.<String, Class>Map(), new HashSet<Type>(), new ArrayList<String>(), null);
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
        if (!E2E.inProgress()) {
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
