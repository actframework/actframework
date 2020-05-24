package act.test.util;

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
import act.app.App;
import act.app.DaoLocator;
import act.app.RuntimeDirs;
import act.conf.AppConfig;
import act.test.Scenario;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.Keyword;
import org.osgl.util.S;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ScenarioManager extends YamlLoader {

    private static DaoLocator NULL_DAO = new NullDaoLocator();

    private Map<Keyword, Scenario> store = new LinkedHashMap<>();
    private Map<Keyword, List<Scenario>> partitionSetups = new HashMap<>();

    private String urlContext;
    private String issueUrlTemplate;
    private String issueUrlIcon = "external-link";
    private RequestTemplateManager requestTemplateManager;

    public ScenarioManager() {
        super("act.test");
        setFixtureFolder("/", "/test");
        configure();
    }

    public ScenarioManager(String modelPackage, String... modelPackages) {
        super();
        addModelPackages("act.test");
        addModelPackages(modelPackage, modelPackages);
        setFixtureFolder("/", "/test");
        configure();
    }

    public Scenario get(String name) {
        return store.get(Keyword.of(name));
    }

    public Scenario get(Keyword testId) {
        return store.get(testId);
    }

    public List<Scenario> getPartitionSetups(String partition) {
        List<Scenario> list = partitionSetups.get(Keyword.of(partition));
        return null != list ? list : C.<Scenario>list();
    }

    public Map<String, Scenario> load() {
        loadDefault();
        searchScenarioFolder();
        Map<String, Scenario> scenarioMap = new LinkedHashMap<>();
        for (Map.Entry<Keyword, Scenario> entry : store.entrySet()) {
            scenarioMap.put(entry.getKey().hyphenated(), entry.getValue());
        }
        for (Scenario scenario : scenarioMap.values()) {
            scenario.resolveDependencies();
        }
        for (Scenario scenario : scenarioMap.values()) {
            scenario.resolveSetupDependencies();
        }
        ScenarioComparator comparator = new ScenarioComparator(false);
        for (List<Scenario> list : partitionSetups.values()) {
            Collections.sort(list, comparator);
        }
        return scenarioMap;
    }

    private void configure() {
        App app = Act.app();
        if (null == app) {
            return;
        }
        AppConfig<?> config = app.config();
        urlContext = config.get("test.urlContext");
        issueUrlTemplate = config.get("test.issueUrlTemplate");
        if (S.notBlank(issueUrlTemplate)) {
            issueUrlIcon = inferIssueUrlIcon(issueUrlTemplate);
        }
        requestTemplateManager = new RequestTemplateManager();
        requestTemplateManager.load();
    }

    private String inferIssueUrlIcon(String issueUrl) {
        String issueUrlIcon = "external-link";
        if (issueUrl.contains("github")) {
            issueUrlIcon = "github-issue";
        } else if (issueUrl.contains("jira") || issueUrl.contains("/browse/")) {
            issueUrlIcon = "jira-issue";
        } else if (issueUrl.contains("gitlab")) {
            issueUrlIcon = "gitlab-issue";
        } else if (issueUrl.contains("gitee") || issueUrl.contains("oschina")) {
            issueUrlIcon = "gitee-issue";
        }
        return issueUrlIcon;
    }

    private void loadDefault() {
        File file = getFile("scenarios.yml");
        if (null != file) {
            parseOne(IO.readContentAsString(file), file.getName());
        }
    }

    private void searchScenarioFolder() {
        File folder = getFile("scenarios");
        if (null != folder) {
            if (folder.exists()) {
                loadFromScenarioDir(folder);
            }
        }
    }

    private void loadFromScenarioDir(File scenariosDir) {
        File[] ymlFiles = scenariosDir.listFiles();
        if (null == ymlFiles) {
            return;
        }
        for (File file : ymlFiles) {
            if (file.isDirectory()) {
                loadFromScenarioDir(file);
            } else {
                String fileName = file.getName();
                if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                    String content = IO.read(file).toString();
                    if (S.blank(content)) {
                        warn("Empty yaml file found: " + file.getPath());
                        continue;
                    }
                    try {
                        parseOne(content, file.getAbsolutePath());
                    } catch (RuntimeException e) {
                        error(e, "Error parsing scenario file: %s", file.getName());
                        throw e;
                    }
                }
            }
        }
    }

    private void parseOne(String content, String fileName) {
        Map<String, Object> map;
        try {
            map = parse(content, NULL_DAO);
        } catch (Exception e) {
            if (fileName.contains("/")) {
                fileName = ".../" + S.afterLast(fileName, "/");
            }
            throw new UnexpectedException(e, "Error parsing yaml file: %s", fileName);
        }
        Map<String, Scenario> loaded = $.cast(map);
        boolean hasDefaultUrlContext = S.notBlank(urlContext);
        for (Map.Entry<String, Scenario> entry : loaded.entrySet()) {
            String key = entry.getKey();
            Scenario scenario = entry.getValue();
            scenario.scenarioManager = this;
            scenario.requestTemplateManager = this.requestTemplateManager;
            scenario.name = key;
            boolean inferIssueKey = !scenario.noIssue && S.blank(scenario.issueKey) && S.notBlank(issueUrlTemplate);
            if (inferIssueKey) {
                if (key.contains(" ")) {
                    scenario.issueKey = S.beforeFirst(key, " ");
                    if (scenario.issueKey.startsWith("!") || scenario.issueKey.endsWith("!")) {
                        scenario.issueKey = null;
                        scenario.noIssue = true;
                        if (S.blank(scenario.refId)) {
                            scenario.refId = S.afterFirst(key, " ");
                        }
                        if (S.blank(scenario.description)) {
                            scenario.description = S.afterFirst(key, " ");
                        }
                    }
                } else {
                    scenario.issueKey = key;
                }
            }
            String issueKey = scenario.issueKey;
            boolean noIssue = scenario.noIssue || scenario.notIssue
                    || Keyword.eq("noIssue", issueKey)
                    || Keyword.eq("noIssueKey", issueKey)
                    || Keyword.eq("notAnIssue", issueKey)
                    || Keyword.eq("notIssue", issueKey);
            scenario.noIssue = noIssue;
            if (noIssue) {
                scenario.issueKey = null;
            }
            int issueKeyLen = noIssue ? 0 : S.string(scenario.issueKey).length();
            boolean namePrefixedWithIssueKey = 0 < issueKeyLen &&
                    (key.startsWith(scenario.issueKey + " ") || key.startsWith(scenario.issueKey + "-") || key.startsWith(scenario.issueKey + ":"));
            if (S.blank(scenario.refId)) {
                scenario.refId = namePrefixedWithIssueKey ? key.substring(issueKeyLen + 1) : key;
            }
            if (S.blank(scenario.description)) {
                scenario.description = namePrefixedWithIssueKey ? key.substring(issueKeyLen + 1) : key;
            }
            scenario.source = fileName;
            if (hasDefaultUrlContext) {
                if (S.isBlank(scenario.urlContext)) {
                    scenario.urlContext = this.urlContext;
                } else if (!scenario.urlContext.startsWith("/")) {
                    scenario.urlContext = S.pathConcat(this.urlContext, '/', scenario.urlContext);
                }
            }
            this.store.put(Keyword.of(key), scenario);
            this.store.put(Keyword.of(scenario.name), scenario);
            if (scenario.setup) {
                List<Scenario> list = partitionSetups.get(Keyword.of(scenario.partition));
                if (null == list) {
                    list = new ArrayList<>();
                    partitionSetups.put(Keyword.of(scenario.partition), list);
                }
                list.add(scenario);
                // DON"T DO THIS NOW - dependent scenario might not be loaded yet Collections.sort(list, new ScenarioComparator(this, false));
            }
            if (S.notBlank(scenario.refId)) {
                this.store.put(Keyword.of(scenario.refId), scenario);
            }
            if (S.blank(scenario.issueUrl) && S.notBlank(issueUrlTemplate)) {
                scenario.issueUrl = S.notBlank(scenario.issueKey) ? S.fmt(issueUrlTemplate, scenario.issueKey) : null;
                scenario.issueUrlIcon = S.notBlank(scenario.issueKey) ? issueUrlIcon : null;
            } else if (S.notBlank(scenario.issueUrl)) {
                scenario.issueUrlIcon = inferIssueUrlIcon(scenario.issueUrl);
            }
        }
    }

}
