package act.e2e.util;

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
import act.app.App;
import act.app.DaoLocator;
import act.app.RuntimeDirs;
import act.e2e.Scenario;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.Keyword;
import org.osgl.util.S;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ScenarioManager extends YamlLoader {

    private static DaoLocator NULL_DAO = new NullDaoLocator();

    private Map<Keyword, Scenario> store = new LinkedHashMap<>();

    public ScenarioManager() {
        super("act.e2e");
        setFixtureFolder("/e2e/");
        configure();
    }

    public ScenarioManager(String modelPackage, String... modelPackages) {
        super();
        addModelPackages("act.e2e");
        addModelPackages(modelPackage, modelPackages);
        setFixtureFolder("/e2e");
        configure();
    }

    public Scenario get(String name) {
        return store.get(Keyword.of(name));
    }

    public Map<String, Scenario> load() {
        loadDefault();
        searchScenarioFolder();
        Map<String, Scenario> scenarioMap = new LinkedHashMap<>();
        for (Map.Entry<Keyword, Scenario> entry : store.entrySet()) {
            scenarioMap.put(entry.getKey().hyphenated(), entry.getValue());
        }
        return scenarioMap;
    }

    private void configure() {
        App app = Act.app();
        if (null == app) {
            return;
        }
    }

    private void loadDefault() {
        String content = getResourceAsString("scenarios.yml");
        if (null == content) {
            return;
        }
        parseOne(content);
    }

    private void searchScenarioFolder() {
        App app = Act.app();
        if (null != app) {
            searchWhenInAppContext(app);
        } else {
            URL url = ScenarioManager.class.getResource("/e2e/scenarios");
            if (null != url) {
                File file = new File(url.getFile());
                if (file.exists()) {
                    loadFromScenarioDir(file);
                }
            }
        }
    }

    private void searchWhenInAppContext(App app) {
        File resource = RuntimeDirs.resource(app);
        if (resource.exists()) {
            loadFromDir(resource);
        } else {
            String appJarFile = System.getProperty(Act.PROP_APP_JAR_FILE);
            if (null != appJarFile) {
                File jarFile = new File(appJarFile);
                loadFromJar(jarFile);
            }
        }
    }

    private void loadFromDir(File resourceDir) {
        if (!resourceDir.exists()) {
            return;
        }
        File scenariosDir = new File(resourceDir, "e2e/scenarios");
        if (!scenariosDir.exists()) {
            return;
        }
        loadFromScenarioDir(scenariosDir);
    }

    private void loadFromScenarioDir(File scenariosDir) {
        File[] ymlFiles = scenariosDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });
        if (null == ymlFiles) {
            return;
        }
        for (File file : ymlFiles) {
            String content = IO.read(file).toString();
            if (S.blank(content)) {
                warn("Empty yaml file found: " + file.getPath());
                continue;
            }
            try {
                parseOne(content);
            } catch (RuntimeException e) {
                error(e, "Error parsing scenario file: %s", file.getName());
                throw e;
            }
        }
    }

    private void loadFromJar(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            for (JarEntry entry : C.enumerable(jar.entries())) {
                String name = entry.getName();
                if (isScenarioFile(name)) {
                    InputStream is = jar.getInputStream(entry);
                    String content = IO.readContentAsString(is);
                    parseOne(content);
                }
            }
        } catch (IOException e) {
            warn(e, "Error loading scenario from jar file");
        }
    }

    private boolean isScenarioFile(String name) {
        return name.startsWith("e2e/scenarios/") && name.endsWith(".yml");
    }

    private void parseOne(String content) {
        Map<String, Object> map = parse(content, NULL_DAO);
        Map<String, Scenario> loaded = $.cast(map);
        for (Map.Entry<String, Scenario> entry : loaded.entrySet()) {
            String key = entry.getKey();
            Scenario scenario = entry.getValue();
            scenario.name = key;
            this.store.put(Keyword.of(key), scenario);
        }
    }

}
