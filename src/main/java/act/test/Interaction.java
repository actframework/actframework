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
import act.metric.Metric;
import act.test.inbox.Inbox;
import act.test.macro.Macro;
import act.test.util.ErrorMessage;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.N;
import org.osgl.util.S;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static act.metric.MetricInfo.ACT_TEST_INTERACTION;
import static act.test.TestStatus.PENDING;
import static act.test.util.ErrorMessage.error;

public class Interaction implements ScenarioPart {
    public List<Macro> preActions = new ArrayList<>();
    public String description = "test interaction";
    public RequestSpec request;
    public ResponseSpec response;
    public List<Macro> postActions = new ArrayList<>();
    public Map<String, String> cache = new LinkedHashMap<>();
    // see https://github.com/actframework/actframework/issues/1119
    public Map<String, String> assign = new LinkedHashMap<>();
    public Map<String, String> store = new LinkedHashMap<>();
    public Map<String, String> save = new LinkedHashMap<>();
    public String errorMessage;
    public transient Throwable cause;
    public TestStatus status = PENDING;
    private transient Metric metric = Act.metricPlugin().metric(ACT_TEST_INTERACTION);

    @Override
    public void validate(TestSession session) throws UnexpectedException {
        E.unexpectedIf(null == request, "request spec not specified in interaction[%s]", this);
        //E.unexpectedIf(null == response, "response spec not specified");
        act.metric.Timer timer = metric.startTimer("validate");
        try {
            request.resolveParent(session.requestTemplateManager);
            request.validate(this);
            if (null != response) {
                response.validate(this);
            }
            reset();
        } finally {
            timer.stop();
        }
    }



    @Override
    public String toString() {
        return description;
    }

    public String getStackTrace() {
        return causeStackTrace();
    }

    public boolean run() {
        act.metric.Timer timer = metric.startTimer("run");
        if (!assign.isEmpty()) {
            cache.putAll(assign);
        }
        if (!store.isEmpty()) {
            cache.putAll(store);
        }
        if (!save.isEmpty()) {
            cache.putAll(save);
        }
        try {
            boolean pass = run(preActions) && verify() && run(postActions);
            status = TestStatus.of(pass);
            return pass;
        } finally {
            timer.stop();
        }
    }

    public String causeStackTrace() {
        return null == cause ? null: E.stackTrace(cause);
    }

    public void reset() {
        errorMessage = null;
        cause = null;
        status = PENDING;
    }

    private boolean verify() {
        Response resp = null;
        try {
            if (S.notBlank(request.email)) {
                doVerifyEmail(request.email);
            } else {
                resp = TestSession.current().sendRequest(request);
                doVerify(resp);
            }
            return true;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            if (null == errorMessage) {
                errorMessage = e.getClass().getName();
            }
            cause = causeOf(e);
            return false;
        } finally {
            IO.close(resp);
        }
    }

    private void doVerify(Response resp) throws Exception {
        act.metric.Timer timer = metric.startTimer("verify");
        try {
            verifyStatus(resp);
            verifyHeaders(resp);
            verifyBody(resp);
        } finally {
            timer.stop();
        }
    }

    private void doVerifyEmail(String email) throws Exception {
        email = TestSession.current().processStringSubstitution(email);
        Inbox inbox = Act.getInstance(Inbox.class);
        Inbox.Reader reader = inbox.getReader();
        String content = reader.readLatest(email);
        TestSession.current().verifyBody(content, response);
    }

    private boolean run(List<Macro> macros) {
        for (Macro macro : macros) {
            boolean okay = run(macro);
            if (!okay) {
                return false;
            }
        }
        return true;
    }

    private boolean run(Macro macro) {
        try {
            macro.run(TestSession.current());
            return true;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            if (null == errorMessage) {
                errorMessage = e.getClass().getName();
            }
            cause = causeOf(e);
            return false;
        }
    }

    private void verifyStatus(Response resp) {
        H.Status expected = expectedStatus();
        if (null == expected) {
            if (!resp.isSuccessful()) {
                int status = resp.code();
                try {
                    String body = resp.body().string();
                    String msg = body;
                    if (body.startsWith("{") && body.endsWith("}")) {
                        try {
                            JSONObject json = JSON.parseObject(body);
                            if (json.containsKey("message")) {
                                msg = json.getString("message");
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    if (msg.contains("<html>")) {
                        Document doc = Jsoup.parse(msg, S.concat("http://localhost:", TestSession.current().port(), "/"));
                        Elements elements = doc.select(".error-message");
                        if (elements.hasText()) {
                            msg = elements.text();
                        } else {
                            elements = doc.select("h1");
                            if (elements.hasText()) {
                                msg = doc.text();
                            } else {
                                msg = doc.title();
                            }
                        }
                    }
                    error("Status verification failure. Expected: successful, Found: %s, Error message: %s", status, msg);
                } catch (IOException e) {
                    error("Status verification failure. Expected: successful, Found: %s", status);
                }
            }
        } else {
            if (expected.code() != resp.code()) {
                error("Status verification failure. Expected: %s, Found: %s", expected.code(), resp.code());
            }
        }
    }

    private void verifyHeaders(Response resp) {
        if (null == response) {
            return;
        }
        for (Map.Entry<String, Object> entry : response.headers.entrySet()) {
            String headerName = entry.getKey();
            String headerVal = resp.header(headerName);
            try {
                TestSession.current().verifyValue(headerName, headerVal, entry.getValue());
            } catch (Exception e) {
                error(e, S.concat("Failed verifying header[", headerName, "]: ", e.getMessage()));
            }
        }
        TestSession.current().lastHeaders.set(resp.headers());
    }

    private void verifyBody(Response rs) throws Exception {
        if (null != response && S.notBlank(response.checksum)) {
            TestSession.current().verifyDownloadChecksum(rs, response.checksum);
            if (S.notBlank(response.downloadFilename)) {
                TestSession.current().verifyDownloadFilename(rs, response.downloadFilename);
            }
        } else if (null != response && S.notBlank(response.downloadFilename)) {
            TestSession.current().verifyDownloadFilename(rs, response.downloadFilename);
        } else {
            String bodyString = S.string(rs.body().string()).trim();
            TestSession.current().verifyBody(bodyString, response);
        }
    }

    private H.Status expectedStatus() {
        return null == response ? null : response.status;
    }

    private static Throwable causeOf(Exception e) {
        Throwable cause = e.getCause();
        Throwable t = null == cause ? e : cause;
        if (t instanceof ErrorMessage) {
            t = null;
        }
        return t;
    }
}
