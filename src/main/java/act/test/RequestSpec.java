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

import static org.osgl.http.H.Header.Names.CONTENT_TYPE;

import act.test.req_modifier.RequestModifier;
import act.test.util.RequestTemplateManager;
import com.alibaba.fastjson.JSON;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.util.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RequestSpec implements InteractionPart {

    public static final RequestSpec RS_CLEAR_FIXTURE = clearFixture();

    public String parent;
    public H.Method method;
    public int port;
    public String url;
    // shortcut for (GET, url) pair
    public String get;
    // shortcut for (POST, url) pair
    public String post;
    // shortcut for (PUT, url) pair
    public String put;
    // shortcut for (DELETE, url) pair
    public String delete;

    // specify the email for inbox verification
    public String email;

    public String accept;
    public Boolean ajax;
    public List<RequestModifier> modifiers = new ArrayList<>();
    public Map<String, Object> params = new LinkedHashMap<>();
    public Map<String, Object> headers = new LinkedHashMap<>();
    public Map<String, Object> parts = new LinkedHashMap<>();
    public Object json;
    public Object xml;

    private boolean resolved;

    public RequestSpec() {}

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    public void resolveParent(RequestTemplateManager manager) {
        if (resolved) {
            return;
        }
        if (null == parent) {
            parent = "global";
        }
        RequestSpec parentSpec = manager.getTemplate(parent);
        if (null != parentSpec && this != parentSpec) {
            parentSpec.resolveParent(manager);
            extendsParent(parentSpec);
        } else if (!"global".equals(parent)) {
            throw new UnexpectedException("parent request template not found: " + parent);
        }
        resolved = true;
    }

    @Override
    public void validate(Interaction interaction) throws UnexpectedException {
        if (S.notBlank(email)) {
            return;
        }
        if (null != get) {
            method = H.Method.GET;
            url = get;
        } else if (null != post) {
            method = H.Method.POST;
            url = post;
        } else if (null != put) {
            method = H.Method.PUT;
            url = put;
        } else if (null != delete) {
            method = H.Method.DELETE;
            url = delete;
        }
        E.unexpectedIf(null == method, "method not specified in request spec of interaction[%s]", interaction);
        if (null == url || ".".equals(url)) {
            url = "";
        }
    }

    public void markAsResolved() {
        resolved = true;
    }

    public void unsetResolvedMark() {
        resolved = false;
    }

    private void extendsParent(RequestSpec parent) {
        if (null == accept) {
            accept = parent.accept;
        }
        if (null == ajax) {
            ajax = parent.ajax;
        }
        if (null == url) {
            url = parent.url;
        }
        if (null == method) {
            method = parent.method;
        }
        for (Map.Entry<String, Object> entry : parent.params.entrySet()) {
            String key = entry.getKey();
            if (!params.containsKey(key)) {
                params.put(key, entry.getValue());
            }
        }
        for (Map.Entry<String, Object> entry : parent.headers.entrySet()) {
            String key = entry.getKey();
            if (!headers.containsKey(key)) {
                headers.put(key, entry.getValue());
            }
        }
    }

    private static RequestSpec clearFixture() {
        RequestSpec rs = new RequestSpec();
        rs.method = H.Method.DELETE;
        rs.url = "/~/test/fixtures";
        return rs;
    }

    public static RequestSpec loadFixtures(List<String> fixtures) {
        RequestSpec rs = new RequestSpec();
        rs.method = H.Method.POST;
        rs.headers.put(CONTENT_TYPE, H.Format.JSON.contentType());
        rs.url = "/~/test/fixtures";
        rs.json = JSON.toJSONString(C.Map("fixtures", fixtures));
        return rs;
    }

    public static RequestSpec generateTestData(String modelType, int number) {
        RequestSpec rs = new RequestSpec();
        rs.method = H.Method.POST;
        rs.headers.put(CONTENT_TYPE, H.Format.JSON.contentType());
        rs.url = "/~/test/generateTestData";
        rs.json = JSON.toJSONString(C.Map("modelType", modelType, "number", number));
        return rs;
    }
}
