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

import static act.test.TestStatus.PENDING;
import static act.test.util.ErrorMessage.*;

import act.Act;
import act.metric.Metric;
import act.metric.MetricInfo;
import act.metric.Timer;
import act.test.util.*;
import act.util.ProgressGauge;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.util.*;

public class Scenario implements ScenarioPart {

    private static final Logger LOGGER = LogManager.get(Scenario.class);

    public static final String PARTITION_DEFAULT = "_default_";

    private static final ThreadLocal<Scenario> current = new ThreadLocal<>();

    public String name;
    public String issueKey;
    public boolean noIssue;
    public boolean notIssue;
    // whether this scenario is a
    // setup fixture for a partition
    public boolean setup;
    // GH1091 - a user friendly name for dependency reference
    public String refId;
    public String description;
    public String issueUrl;
    public String issueUrlIcon;
    public String ignore;
    public List<String> fixtures = new ArrayList<>();
    public Object generateTestData;
    public List<String> depends = new ArrayList<>();
    public Set<Scenario> allDepends = new HashSet<>();
    public List<Interaction> interactions = new ArrayList<>();
    public Map<String, Object> constants = new HashMap<>();
    public TestStatus status = PENDING;
    public boolean clearFixtures = true;
    public String urlContext;
    public String partition = PARTITION_DEFAULT;
    public String source;
    private transient Metric metric = Act.metricPlugin().metric(MetricInfo.ACT_TEST_SCENARIO);

    public ScenarioManager scenarioManager;
    public RequestTemplateManager requestTemplateManager;

    public String errorMessage;
    public Throwable cause;

    public Scenario() {
    }

    @Override
    public String toString() {
        S.Buffer buf = S.buffer("[").a(status).a("]").a(title());
        if (partition != "DEFAULT") {
            buf.append('@').append(partition);
        }
        return buf.toString();
    }

    public String title() {
        boolean hasIssueKey = S.notBlank(issueKey);
        boolean nonDefaultPartition = S.neq(PARTITION_DEFAULT, partition);
        S.Buffer buf = S.buffer();
        if (hasIssueKey || nonDefaultPartition) {
            buf.append("[");
            if (hasIssueKey) {
                buf.append(issueKey);
            }
            if (nonDefaultPartition) {
                buf.append("«").append(partition).append("»");
            }
            buf.append("]").append(" ");
        }
        buf.append(S.blank(description) ? name : description);
        return buf.toString();
    }

    /**
     * For {@link #title()} JSON export.
     *
     * @return the {@link #title()} of the scenario.
     */
    public String getTitle() {
        return title();
    }

    public String getIgnoreReason() {
        return S.eq("true", ignore, S.IGNORECASE) ? "ignored" : ignore;
    }

    public String causeStackTrace() {
        return null == cause ? null: E.stackTrace(cause);
    }

    public String getStackTrace() {
        return causeStackTrace();
    }

    public TestStatus statusOf(Interaction interaction) {
        return interaction.status;
    }

    public String errorMessageOf(Interaction interaction) {
        return interaction.errorMessage;
    }

    public void resolveDependencies() {
        if (!allDepends.isEmpty()) {
            // already resolved
            return;
        }
        for (String name : depends) {
            Scenario depend = scenarioManager.get(name);
            E.unexpectedIf(null == depend, "cannot find dependent scenario by name: " + name);
            if (this == depend) {
                LOGGER.warn("Scenario cannot depend on it self: %s", name);
                continue;
            }
            E.unexpectedIf(this == depend, "Circular dependency found");
            allDepends.add(depend);
            depend.resolveDependencies();
            E.unexpectedIf(depend.allDepends.contains(this), "Circular dependency found from %s on %s", this.name, name);
            allDepends.addAll(depend.allDepends);
        }
    }

    public void resolveSetupDependencies() {
        if (setup) {
            return;
        }
        for (Scenario setupScenario : scenarioManager.getPartitionSetups(partition)) {
            if (!setupScenario.allDepends.contains(this)) {
                allDepends.add(setupScenario);
                allDepends.addAll(setupScenario.allDepends);
            }
        }
    }

    @Override
    public void validate(TestSession session) throws UnexpectedException {
        errorIf(S.blank(name), "Scenario name not defined");
        for (Interaction interaction : interactions) {
            interaction.validate(session);
        }
        processConstants(session);
    }

    public void reset() {
        status = PENDING;
        for (Interaction interaction : interactions) {
            interaction.reset();
        }
    }

    private void processConstants(TestSession session) {
        Map<String, Object> copy = new HashMap<>(constants);
        for (Map.Entry<String, Object> entry : copy.entrySet()) {
            Object value = entry.getValue();
            String sVal = S.string(value);
            if (sVal.startsWith("${")) {
                String expr = S.strip(sVal).of("${", "}");
                value = session.eval(expr);
                E.unexpectedIf(null == value, "Error evaluating constant: %s", expr);
            } else if (sVal.contains("${")) {
                value = session.processStringSubstitution(sVal);
            }
            String key = entry.getKey();
            constants.remove(key);
            key = S.underscore(key);
            constants.put(key, value);
            session.constants.put(key, value); // otherwise line 194 `value = session.eval(expr)` might fail
        }
    }

    private boolean generateTestData(TestSession session) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("generate test data for " + name);
        }
        if (null == generateTestData) {
            return true;
        }
        Timer timer = metric.startTimer("generate-test-data");
        try {
            boolean ok;
            if (generateTestData instanceof Map) {
                Map<String, Integer> map = $.cast(generateTestData);
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    RequestSpec req = RequestSpec.generateTestData(entry.getKey(), entry.getValue());
                    ok = session.verify(req, "generate test data for " + entry.getKey());
                    if (!ok) {
                        return false;
                    }
                }
            } else if (generateTestData instanceof List) {
                List<String> list = $.cast(generateTestData);
                for (String modelType : list) {
                    RequestSpec req = RequestSpec.generateTestData(modelType, 100);
                    ok = session.verify(req, "generate test data for " + modelType);
                    if (!ok) {
                        return false;
                    }
                }
            }
            return true;
        } finally {
            timer.stop();
        }
    }

    boolean run(TestSession session, ProgressGauge gauge) {
        boolean traceEnabled = LOGGER.isTraceEnabled();
        if (traceEnabled) {
            LOGGER.trace("run " + name);
        }
        if (status.finished()) {
            if (traceEnabled) {
                LOGGER.trace("already finished: " + name);
            }
            return status.pass();
        }
        Timer timer = metric.startTimer("run");
        try {
            return generateTestData(session) && runInteractions(session, gauge);
        } finally {
            timer.stop();
        }
    }

    private boolean runInteractions(TestSession session, ProgressGauge gauge) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("run interactions for " + name);
        }
        gauge.incrMaxHintBy(interactions.size());
        for (Interaction interaction : interactions) {
            try {
                gauge.setPayload(Test.PG_PAYLOAD_INTERACTION, interaction.description);
                boolean pass = runInteraction(session, interaction);
                if (!pass) {
                    //errorMessage = S.fmt("interaction[%s] failure", interaction.description);
                    return false;
                }
            } finally {
                gauge.step();
            }
        }
        return true;
    }

    private boolean runInteraction(TestSession session, Interaction interaction) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("run interaction for %s: %s", name, interaction.description);
        }
        boolean okay = interaction.run();
        if (!okay) {
            return false;
        }

        for (Map.Entry<String, String> entry : interaction.cache.entrySet()) {
            String ref = entry.getValue();
            Object value = ref.contains("${") ? session.processStringSubstitution(ref) : session.getLastVal(ref);
            if (null != value) {
                session.cache(entry.getKey(), value);
            }
        }
        return true;
    }

}
