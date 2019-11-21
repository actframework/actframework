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
import act.test.RequestSpec;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.Keyword;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestTemplateManager extends YamlLoader {

    private static DaoLocator NULL_DAO = new NullDaoLocator();

    private Map<Keyword, RequestSpec> store = new HashMap<>();

    public RequestTemplateManager() {
        super("act.test");
        setFixtureFolder("/", "/test/");
    }

    public void load() {
        String content = getResourceAsString("requests.yml");
        if (null != content) {
            try {
                doParse(content);
            } catch (RuntimeException e) {
                throw E.unexpected(e, "Error loading requests.yml");
            }
        }
        addBuildInTemplates();
    }

    public RequestSpec getTemplate(String id) {
        return store.get(Keyword.of(id));
    }

    private void doParse(String content) {
        Map<String, Object> map = parse(content, NULL_DAO);
        List<RequestSpec> toBeResolved = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            RequestSpec spec = (RequestSpec) entry.getValue();
            store.put(Keyword.of(key), spec);
            if (!"global".equalsIgnoreCase(key)) {
                toBeResolved.add(spec);
            } else {
                spec.markAsResolved();
            }
        }
        for (RequestSpec spec : toBeResolved) {
            spec.resolveParent(this);
        }
    }

    private void addBuildInTemplates() {
        addAjax();
        addGlobal();
    }

    private void addGlobal() {
        Keyword key = Keyword.of("global");
        if (store.containsKey(key)) {
            return;
        }
        RequestSpec spec = new RequestSpec();
        App app = Act.app();
        if (null != app) {
            String sessionHeader = app.config().sessionHeader();
            if (S.notBlank(sessionHeader)) {
                spec.headers.put(sessionHeader, "last:");
            }
            if ($.bool(app.config().get("act.test.json"))) {
                spec.accept = "json";
            }
        }
        store.put(key, spec);

        // we need to redo parent resolving so that
        // the new global templates be applied.
        for (RequestSpec rs : store.values()) {
            rs.unsetResolvedMark();
        }

        // global template must be mark as resolved
        spec.markAsResolved();

        for (RequestSpec rs : store.values()) {
            rs.resolveParent(this);
        }
    }

    private void addAjax() {
        Keyword key = Keyword.of("ajax");
        if (store.containsKey(key)) {
            return;
        }
        RequestSpec spec = new RequestSpec();
        spec.ajax = true;
        spec.json = true;
        store.put(key, spec);
    }
}
