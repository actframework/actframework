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
import static act.util.ProgressGauge.PAYLOAD_MESSAGE;
import static org.osgl.http.H.Header.Names.ACCEPT;
import static org.osgl.http.H.Header.Names.X_REQUESTED_WITH;
import static org.osgl.http.H.Method.POST;

import act.Act;
import act.app.App;
import act.handler.builtin.FileGetter;
import act.metric.MeasureTime;
import act.metric.Metric;
import act.metric.MetricInfo;
import act.metric.Timer;
import act.test.func.Func;
import act.test.req_modifier.RequestModifier;
import act.test.util.*;
import act.test.verifier.Verifier;
import act.util.ProgressGauge;
import com.alibaba.fastjson.*;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Scenario implements ScenarioPart {

    private static final Logger LOGGER = LogManager.get(Test.class);

    private static final ThreadLocal<Scenario> current = new ThreadLocal<>();

    private static final RequestBody EMPTY_BODY = RequestBody.create(null, "");

    private class RequestBuilder {

        private Request.Builder builder;

        RequestBuilder(RequestSpec requestSpec) {
            builder = new Request.Builder();
            String accept = requestSpec.accept;
            if (null != accept) {
                if (S.eq("json", accept, S.IGNORECASE)) {
                    accept = H.Format.JSON.contentType();
                } else {
                    H.Format format = H.Format.of(accept);
                    if (null == format) {
                        format = H.Format.resolve(accept);
                    }
                    if (H.Format.UNKNOWN == format) {
                        throw new UnexpectedException("Invalid accept in request spec: " + accept);
                    }
                    accept = format.contentType();
                }
                builder.addHeader(ACCEPT, accept);
            }
            if ($.bool(requestSpec.ajax)) {
                builder.addHeader(X_REQUESTED_WITH, "XMLHttpRequest");
            }
            for (RequestModifier modifier : requestSpec.modifiers) {
                modifier.modifyRequest(builder);
            }
            for (Map.Entry<String, Object> entry : requestSpec.headers.entrySet()) {
                String headerName = entry.getKey();
                String headerVal = S.string(entry.getValue());
                if (headerVal.startsWith("last:") || headerVal.startsWith("last|")) {
                    String payload = headerVal.substring(5);
                    if (S.blank(payload)) {
                        payload = headerName;
                    }
                    Headers headers = lastHeaders.get();
                    headerVal = null == headers ? null : S.string(lastHeaders.get().get(payload));
                }
                if (null != headerVal) {
                    builder.addHeader(headerName, headerVal);
                }
            }
            String reqUrl = requestSpec.url;
            if (S.notBlank(urlContext) && !reqUrl.startsWith("/")) {
                reqUrl = S.pathConcat(urlContext, '/', reqUrl);
            }
            String url;
            if (!reqUrl.startsWith("http")) {
                int portx = 0 != requestSpec.port ? requestSpec.port : port;
                url = S.concat("http://localhost:", portx, S.ensure(processStringSubstitution(reqUrl)).startWith("/"));
            } else {
                url = processStringSubstitution(reqUrl);
            }
            boolean hasParams = !requestSpec.params.isEmpty();
            if (hasParams) {
                processParamSubstitution(requestSpec.params);
            }
            boolean hasParts = !hasParams && POST == requestSpec.method && !requestSpec.parts.isEmpty();
            if (hasParts) {
                processParamSubstitution(requestSpec.parts);
            }
            switch (requestSpec.method) {
                case GET:
                case HEAD:
                    if (hasParams) {
                        S.Buffer buf = S.buffer(url);
                        if (!url.contains("?")) {
                            buf.a("?__nil__=nil");
                        }
                        for (Map.Entry<String, Object> entry : requestSpec.params.entrySet()) {
                            String paramName = Codec.encodeUrl(entry.getKey());
                            String paramVal = Codec.encodeUrl(S.string(entry.getValue()));
                            buf.a("&").a(paramName).a("=").a(paramVal);
                        }
                        url = buf.toString();
                    }
                    builder.method(requestSpec.method.name(), null);
                    break;
                case DELETE:
                case POST:
                case PUT:
                case PATCH:
                    RequestBody body = EMPTY_BODY;
                    String jsonBody = verifyJsonBody(requestSpec.json);
                    String xmlBody = verifyXmlBody(requestSpec.xml);
                    if (S.notBlank(jsonBody)) {
                        body = RequestBody.create(MediaType.parse("application/json"), jsonBody);
                    } else if (S.notBlank(xmlBody)) {
                        body = RequestBody.create(MediaType.parse("text/xml"), xmlBody);
                    } else if (hasParams) {
                        FormBody.Builder formBuilder = new FormBody.Builder();
                        for (Map.Entry<String, Object> entry : requestSpec.params.entrySet()) {
                            formBuilder.add(entry.getKey(), S.string(entry.getValue()));
                        }
                        body = formBuilder.build();
                    } else if (hasParts) {
                        MultipartBody.Builder formBuilder = new MultipartBody.Builder();
                        for (Map.Entry<String, Object> entry : requestSpec.parts.entrySet()) {
                            String key = entry.getKey();
                            String val = S.string(entry.getValue());
                            String path = S.pathConcat("test/upload", '/', val);
                            URL fileUrl = Act.getResource(path);
                            if (null != fileUrl) {
                                String filePath = fileUrl.getFile();
                                H.Format fileFormat = FileGetter.contentType(filePath);
                                byte[] content = $.convert(fileUrl).to(byte[].class);
                                String checksum = IO.checksum(content);
                                RequestBody fileBody = RequestBody.create(MediaType.parse(fileFormat.contentType()), content);
                                formBuilder.addFormDataPart(key, S.cut(filePath).afterLast("/"), fileBody);
                                cache("checksum-last", checksum);
                                cache("checksum-" + val, checksum);
                            } else {
                                formBuilder.addFormDataPart(key, val);
                            }
                        }
                        body = formBuilder.build();
                    }
                    builder.method((requestSpec.method.name()), body);
                    break;
                default:
                    throw E.unexpected("HTTP method not supported: " + requestSpec.method);
            }
            builder.url(url);
        }

        private void processParamSubstitution(Map<String, Object> params) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof String) {
                    String sVal = (String) val;
                    if (sVal.startsWith("last:") || sVal.startsWith("last|")) {
                        String ref = sVal.substring(5);
                        entry.setValue(getLastVal(ref));
                    } else if (sVal.contains("${")) {
                        sVal = processStringSubstitution(sVal);
                        entry.setValue(S.isInt(sVal) ? Integer.parseInt(sVal) : sVal);
                    }
                } else if (val instanceof Map) {
                    processParamSubstitution((Map) val);
                } else if (val instanceof Collection) {
                    val = processParamSubstitution((Collection) val);
                    entry.setValue(val);
                }
            }
        }

        private Collection processParamSubstitution(Collection params) {
            Collection ret = Act.getInstance(params.getClass());
            for (Object val : params) {
                if (val instanceof String) {
                    String sVal = (String) val;
                    if (sVal.startsWith("last:") || sVal.startsWith("last|")) {
                        String ref = sVal.substring(5);
                        ret.add(getLastVal(ref));
                    } else if (sVal.contains("${")) {
                        sVal = processStringSubstitution(sVal);
                        ret.add(S.isInt(sVal) ? Integer.parseInt(sVal) : sVal);
                    } else {
                        ret.add(sVal);
                    }
                } else if (val instanceof Map) {
                    processParamSubstitution((Map) val);
                    ret.add(val);
                } else if (val instanceof Collection) {
                    ret.add(processParamSubstitution((Collection) val));
                } else {
                    ret.add(val);
                }
            }
            return ret;
        }

        Request build() {
            return builder.build();
        }

        private String verifyJsonBody(Object jsonBody) {
            if (jsonBody instanceof Map) {
                processParamSubstitution((Map) jsonBody);
            } else if (jsonBody instanceof Collection) {
                jsonBody = processParamSubstitution((Collection) jsonBody);
            }
            String s = null == jsonBody ? "" : (jsonBody instanceof String) ? (String) jsonBody : JSON.toJSONString(jsonBody);
            if (S.blank(s)) {
                return "";
            }
            final String origin = s;
            if (s.startsWith("resource:")) {
                s = S.ensure(s.substring(9).trim()).startWith("/");
                URL url = Act.getResource(s);
                E.unexpectedIf(null == url, "Cannot find JSON body: " + origin);
                s = IO.read(url).toString();
            }
            try {
                JSON.parse(s);
            } catch (Exception e) {
                E.unexpected(e, "Invalid JSON body: " + origin);
            }
            return s;
        }

        private String verifyXmlBody(Object xmlBody) {
            JSON json = null;
            if (xmlBody instanceof Map) {
                processParamSubstitution((Map) xmlBody);
                json = JSON.parseObject(JSON.toJSONString(xmlBody));
            } else if (xmlBody instanceof Collection) {
                xmlBody = processParamSubstitution((Collection) xmlBody);
                json = JSON.parseArray(JSON.toJSONString(xmlBody));
            }
            String s = null == xmlBody ? "" : (xmlBody instanceof String) ? (String) xmlBody : XML.toString($.convert(json).to(org.w3c.dom.Document.class));
            if (S.blank(s)) {
                return "";
            }
            final String origin = s;
            if (s.startsWith("resource:")) {
                s = S.ensure(s.substring(9).trim()).startWith("/");
                URL url = Act.getResource(s);
                E.unexpectedIf(null == url, "Cannot find JSON body: " + origin);
                s = IO.read(url).toString();
            }
            try {
                XML.read(s);
            } catch (Exception e) {
                E.unexpected(e, "Invalid JSON body: " + origin);
            }
            return s;
        }

    }


    private int port = 5460;
    private OkHttpClient http;
    private CookieStore cookieStore;
    private App app;
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
    public boolean ignore = false;
    public List<String> fixtures = new ArrayList<>();
    public Object generateTestData;
    public List<String> depends = new ArrayList<>();
    public List<Interaction> interactions = new ArrayList<>();
    public Map<String, Object> constants = new HashMap<>();
    public TestStatus status = PENDING;
    public String errorMessage;
    public Throwable cause;
    public boolean clearFixtures = true;
    public String urlContext;
    public String partition = "__DEFAULT";
    public String source;
    private transient Metric metric = Act.metricPlugin().metric(MetricInfo.ACT_TEST_SCENARIO);

    $.Var<Object> lastData = $.var();
    $.Var<Headers> lastHeaders = $.var();

    ScenarioManager scenarioManager;
    RequestTemplateManager requestTemplateManager;

    private Map<String, Object> cache = new HashMap<>();

    public Scenario() {
        app = Act.app();
        if (null != app) {
            port = app.config().httpPort();
        }
    }

    @Override
    public String toString() {
        S.Buffer buf = S.buffer("[").a(status).a("]").a(title());
        if (partition != "DEFAULT") {
            buf.append('@').append(partition);
        }
        return buf.toString();
    }

    public int port() {
        return port;
    }

    public String title() {
        if (null == issueKey) {
            return S.blank(description) ? name : description;
        }
        S.Buffer buf = S.buffer("[").a(issueKey).a("]");
        if (S.notBlank(description)) {
            buf.a(" ").a(description);
        }
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

    public void cache(String name, Object payload) {
        cache.put(name, payload);
    }

    public Object cached(String name) {
        return cache.get(name);
    }

    public TestStatus statusOf(Interaction interaction) {
        return interaction.status;
    }

    public String errorMessageOf(Interaction interaction) {
        return interaction.errorMessage;
    }

    @Override
    public void validate(Scenario scenario) throws UnexpectedException {
        errorIf(S.blank(name), "Scenario name not defined");
        for (Interaction interaction : interactions) {
            interaction.validate(scenario);
        }
        processConstants();
    }

    private void processConstants() {
        Map<String, Object> copy = new HashMap<>(constants);
        for (Map.Entry<String, Object> entry : copy.entrySet()) {
            Object value = entry.getValue();
            String sVal = S.string(value);
            if (sVal.startsWith("${")) {
                String expr = S.strip(sVal).of("${", "}");
                value = eval(expr);
            } else if (sVal.contains("${")) {
                value = processStringSubstitution(sVal);
            }
            String key = entry.getKey();
            constants.remove(key);
            constants.put(S.underscore(key), value);
        }
    }

    private Object eval(String expr) {
        if (expr.startsWith("func:")) {
            return evalFunc(expr.substring(5));
        } else if (expr.contains("(")) {
            return evalFunc(expr);
        }
        String key = S.underscore(expr);
        Object o = constants.get(key);
        o = null == o ? cache.get(key) : o;
        return null == o ? Test.constant(key) : o;
    }

    private Object evalFunc(String funcExpr) {
        String funcName = funcExpr;
        List<String> vals = C.list();
        if (funcExpr.contains("(")) {
            funcName = S.cut(funcExpr).beforeFirst("(");
            String paramStr = S.cut(funcExpr).afterFirst("(");
            paramStr = S.cut(paramStr).beforeLast(")");
            if (S.notBlank(paramStr)) {
                vals = C.newList(S.fastSplit(paramStr, ","));
                for (int i = 0; i < vals.size(); ++i) {
                    String val = S.ensure(vals.get(i).trim()).strippedOff(S.DOUBLE_QUOTES);
                    val = processStringSubstitution(val);
                    vals.set(i, val);
                }
            }
        }
        Func func = $.convert(funcName).to(Func.class);
        switch (vals.size()) {
            case 0:
                break;
            case 1:
                func.init(vals.get(0));
                break;
            default:
                func.init(vals);
        }
        return func.apply();
    }

    public void start(ScenarioManager scenarioManager, RequestTemplateManager requestTemplateManager, ProgressGauge gauge, boolean forceClearFixtures) {
        start(scenarioManager, requestTemplateManager, true, gauge, forceClearFixtures);
    }

    private void start(ScenarioManager scenarioManager, RequestTemplateManager requestTemplateManager, boolean reset, ProgressGauge gauge, boolean forceClearFixtures) {
        this.scenarioManager = $.requireNotNull(scenarioManager);
        this.requestTemplateManager = $.requireNotNull(requestTemplateManager);
        this.status = PENDING;
        current.set(this);
        gauge.setPayload(Test.PG_PAYLOAD_SCENARIO, title());
        gauge.incrMaxHint();
        try {
            validate(this);
        } finally {
            gauge.step();
        }
        if (null == http) {
            gauge.incrMaxHint();
            try {
                prepareHttp();
            } finally {
                gauge.step();
            }
        }
        boolean pass = (!reset || reset(gauge)) && run(gauge, forceClearFixtures);
        this.status = TestStatus.of(pass);
        if (TestStatus.FAIL == this.status) {
            for (Interaction interaction : this.interactions) {
                if (interaction.status == TestStatus.FAIL) {
                    this.errorMessage = "Interaction[" + interaction.description + "] fail: " + interaction.errorMessage;
                    if (interaction.cause != null && !(interaction.cause instanceof ErrorMessage)) {
                        this.cause = interaction.cause;
                    }
                }
            }
        }
    }

    public void clearSession() {
        if (depends.isEmpty()) {
            cookieStore().clear();
        }
    }

    public boolean clearFixtures() {
        if (!clearFixtures) {
            return true;
        }
        Timer timer = metric.startTimer("clear-fixtures");
        try {
            return verify(RequestSpec.RS_CLEAR_FIXTURE, "clearing fixtures");
        } finally {
            timer.stop();
        }
    }

    public String causeStackTrace() {
        return null == cause ? null: E.stackTrace(cause);
    }

    public String getStackTrace() {
        return causeStackTrace();
    }

    void resolveRequest(RequestSpec req) {
        req.resolveParent(requestTemplateManager);
    }

    Response sendRequest(RequestSpec req) throws IOException {
        Timer timer = metric.startTimer("build-request");
        Request httpRequest = new RequestBuilder(req).build();
        timer.stop();
        timer = metric.startTimer("send-request");
        Response resp = http.newCall(httpRequest).execute();
        timer.stop();;
        lastHeaders.set(resp.headers());
        return resp;
    }

    private boolean createFixtures() {
        if (fixtures.isEmpty()) {
            return true;
        }
        Timer timer = metric.startTimer("create-fixtures");
        try {
            RequestSpec req = RequestSpec.loadFixtures(fixtures);
            return verify(req, "creating fixtures");
        } finally {
            timer.stop();
        }
    }

    private boolean generateTestData() {
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
                    ok = verify(req, "generate test data for " + entry.getKey());
                    if (!ok) {
                        return false;
                    }
                }
            } else if (generateTestData instanceof List) {
                List<String> list = $.cast(generateTestData);
                for (String modelType : list) {
                    RequestSpec req = RequestSpec.generateTestData(modelType, 100);
                    ok = verify(req, "generate test data for " + modelType);
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

    private boolean verify(RequestSpec req, String operation) {
        boolean pass = true;
        Response resp = null;
        try {
            resp = sendRequest(req);
            if (!resp.isSuccessful()) {
                pass = false;
                errorMessage = "Fixtures loading failure";
            }
            return pass;
        } catch (IOException e) {
            errorMessage = "Error " + operation;
            LOGGER.error(e, errorMessage);
            return false;
        } finally {
            IO.close(resp);
        }
    }

    private void prepareHttp() {
        long timeout = app.config().testTimeout();
        http = new OkHttpClient.Builder()
                .cookieJar(cookieStore())
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    private boolean reset(ProgressGauge gauge) {
        Timer timer = metric.startTimer("reset");
        try {
            gauge.incrMaxHint();
            errorMessage = null;
            clearSession();
            if (depends.isEmpty()) {
                return clearFixtures() && createFixtures() && generateTestData();
            }
            return createFixtures() && generateTestData();
        } finally {
            gauge.step();
            timer.stop();
        }
    }

    private boolean run(ScenarioManager scenarioManager, RequestTemplateManager requestTemplateManager, ProgressGauge gauge, boolean forceClearFixtures) {
        if (null == this.scenarioManager) {
            this.start(scenarioManager, requestTemplateManager, forceClearFixtures, gauge, forceClearFixtures);
            return this.status.pass();
        } else {
            return run(gauge, forceClearFixtures);
        }
    }

    private boolean run(ProgressGauge gauge, boolean forceClearFixtures) {
        if (status.finished()) {
            return status.pass();
        }
        Timer timer = metric.startTimer("run");
        try {
            return runDependents(gauge, forceClearFixtures) && runInteractions(gauge);
        } finally {
            timer.stop();
        }
    }

    private boolean runDependents(ProgressGauge gauge, boolean forceClearFixtures) {
        List<Scenario> partitionSetups = scenarioManager.getPartitionSetups(partition);
        List<Scenario> allDeps = new ArrayList<>();
        for (Scenario scenario : partitionSetups) {
            if (scenario == this) {
                break;
            }
            allDeps.add(scenario);
        }
        for (String dependent : depends) {
            Scenario scenario = scenarioManager.get(dependent);
            errorIf(null == scenario, "Dependent not found: " + dependent);
            allDeps.add(scenario);
        }
        Collections.sort(allDeps, new ScenarioComparator(scenarioManager, partition));
        gauge.incrMaxHintBy(allDeps.size());
        for (Scenario scenario : allDeps) {
            try {
                Scenario old = current.get();
                try {
                    if (!scenario.run(scenarioManager, requestTemplateManager, gauge, forceClearFixtures)) {
                        errorMessage = "dependency failure: " + scenario.name;
                        return false;
                    }
                    inheritFrom(scenario);
                } finally {
                    current.set(old);
                }
            } finally {
                gauge.step();
            }
        }
        return true;
    }

    private void inheritFrom(Scenario dependent) {
        lastHeaders.set(dependent.lastHeaders.get());
        lastData.set(dependent.lastData.get());
        cache.putAll(dependent.cache);
        http = dependent.http;
        Map<String, Object> map = C.newMap(dependent.constants);
        map.putAll(constants);
        constants.putAll(map);
    }

    private boolean runInteractions(ProgressGauge gauge) {
        gauge.incrMaxHintBy(interactions.size());
        for (Interaction interaction : interactions) {
            try {
                gauge.setPayload(Test.PG_PAYLOAD_INTERACTION, interaction.description);
                boolean pass = run(interaction);
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

    private boolean run(Interaction interaction) {
        boolean okay = interaction.run();
        if (!okay) {
            return false;
        }

        for (Map.Entry<String, String> entry : interaction.cache.entrySet()) {
            String ref = entry.getValue();
            Object value = ref.contains("${") ? processStringSubstitution(ref) : getLastVal(ref);
            if (null != value) {
                cache(entry.getKey(), value);
            }
        }
        return true;
    }

    private synchronized CookieStore cookieStore() {
        if (null == cookieStore) {
            App app = Act.app();
            cookieStore = null == app ? new CookieStore() : app.getInstance(CookieStore.class);
        }
        return cookieStore;
    }

    void verifyDownloadChecksum(Response response, String checksumExpected) {
        Object o = cached(checksumExpected);
        if (null == o) {
            o = cached("checksum-last");
        }
        if (null == o) {
            o = checksumExpected;
        }
        String downloadChecksum = IO.checksum(response.body().byteStream());
        errorIfNot($.eq(downloadChecksum, o), "Download checksum not match");
    }

    void verifyDownloadFilename(Response response, String filenameExpected) {
        String header = response.header("Content-Disposition");
        // header should be something like: attachment; filename="postcodes.xls"
        errorIf(S.isBlank(header), "No Content-Disposition header found");
        int pos = header.indexOf('"');
        errorIf(pos < 0, "Content-Disposition header does not have filename specified: " + header);
        String filename = header.substring(pos + 1);
        filename = filename.substring(0, filename.length() - 1); // strip off last `"`
        errorIfNot(S.eq(filenameExpected, filename), "Download filename[%s] not match expected value[%s]", filename, filenameExpected);
    }

    void verifyBody(String bodyString, ResponseSpec spec) {
        lastData.set(bodyString);
        if (null == spec) {
            if (bodyString.startsWith("[")) {
                JSONArray array = JSON.parseArray(bodyString);
                lastData.set(array);
            } else if (bodyString.startsWith("{")) {
                JSONObject obj = JSON.parseObject(bodyString);
                lastData.set(obj);
            }
            return;
        }
        if (null != spec.text) {
            verifyValue("body text", bodyString, spec.text);
        } else if (null != spec.json && !spec.json.isEmpty()) {
            if (bodyString.startsWith("[")) {
                JSONArray array = JSON.parseArray(bodyString);
                lastData.set(array);
                verifyList("body json array", array, spec.json);
            } else if (bodyString.startsWith("{")) {
                JSONObject obj = JSON.parseObject(bodyString);
                lastData.set(obj);
                verifyJsonObject(obj, spec.json);
            } else {
                error("Unknown JSON string: \n%s", bodyString);
            }
        } else if (null != spec.xml && !spec.xml.isEmpty()) {
            org.w3c.dom.Document doc = XML.read(bodyString);
            JSONObject obj = $.convert(doc).to(JSONObject.class);
            String xmlRoot = app.config().xmlRootTag();
            if (obj.containsKey(xmlRoot)) {
                Object o = obj.get(xmlRoot);
                lastData.set(o);
                if (o instanceof JSONObject) {
                    obj = (JSONObject) o;
                    verifyJsonObject(obj, spec.xml);
                } else if (o instanceof List) {
                    verifyList("body xml array", (List)o, spec.xml);
                } else {
                    throw new UnexpectedException("Unknown root type: " + o.getClass());
                }
            } else {
                lastData.set(obj);
                verifyJsonObject(obj, spec.xml);
            }
        } else if (null != spec.html && !spec.html.isEmpty()) {
            lastData.set(bodyString);
            Document doc = Jsoup.parse(bodyString, S.concat("http://localhost:", port, "/"));
            for (Map.Entry<String, Object> entry : spec.html.entrySet()) {
                String path = entry.getKey();
                Elements elements = doc.select(path);
                verifyValue("body html path", elements, entry.getValue());
            }
        }
    }

    void verifyList(String name, List array, Map spec) {
        for (Object obj : spec.entrySet()) {
            Map.Entry entry = $.cast(obj);
            Object key = entry.getKey();
            String sKey = S.string(key);
            Object test = entry.getValue();
            Object value = null;
            if ("me".equals(key) || "this".equals(key)) {
                value = array;
            } else if ("size".equals(key) || "len".equals(key) || "length".equals(key)) {
                value = array.size();
            } else if ("toString".equals(key) || "string".equals(key) || "str".equals(key)) {
                value = JSON.toJSONString(array);
            } else if ("?".equals(key) || (sKey.toLowerCase().startsWith("<any") && sKey.toLowerCase().endsWith(">"))) {
                for (Object arrayElement : array) {
                    try {
                        verifyValue(name, arrayElement, test);
                        return;
                    } catch (Exception e) {
                        // try next one
                    }
                }
                throw error("No element matches requirement: %s", test);
            } else if (S.isInt(sKey)) {
                int id = Integer.parseInt(sKey);
                value = array.get(id);
            } else {
                if (sKey.contains(".")) {
                    String id = S.cut(key).beforeFirst(".");
                    String prop = S.cut(key).afterFirst(".");
                    if ("?".equals(id) || "<any>".equalsIgnoreCase(id)) {
                        for (Object arrayElement : array) {
                            if (!(arrayElement instanceof JSONObject)) {
                                continue;
                            }
                            try {
                                verifyValue(prop, ((JSONObject) arrayElement).get(prop), test);
                                return;
                            } catch (Exception e) {
                                // try next one
                            }
                        }
                    } else if (S.isInt(id)) {
                        int i = Integer.parseInt(id);
                        Object o = array.get(i);
                        if (o instanceof JSONObject) {
                            JSONObject json = (JSONObject) o;
                            value = json.get(prop);
                        }
                    }
                }
                if (null == value) {
                    throw error("Unknown attribute of array verification: %s", key);
                }
            }
            verifyValue(name, value, test);
        }
    }

    void verifyJsonObject(JSONObject obj, Map<String, Object> jsonSpec) {
        for (Map.Entry<String, Object> entry : jsonSpec.entrySet()) {
            String key = entry.getKey();
            // ${sizeOf(date)}
            if (key.startsWith("${")) {
                // sizeOf(date)}
                String s = key.substring(2);

                // sizeOf(date)
                s = s.substring(0, s.length() - 1);

                // sizeOf
                String funcName = s.substring(0, s.indexOf("("));

                // date)
                s = s.substring(s.indexOf("(") + 1);
                // date
                String varName = s.substring(0, s.length() - 1);

                Object val = $.getProperty(obj, varName);
                val = evalFunc(funcName + "(" + val + ")");
                verifyValue(key, val, entry.getValue());
            } else {
                Object val = $.getProperty(obj, key);
                verifyValue(key, val, entry.getValue());
            }
        }
    }

    void verifyValue(String name, Object value, Object test) {
        if (test instanceof List) {
            verifyValue_(name, value, (List) test);
        } else if (value instanceof List && test instanceof Map) {
            verifyList(name, (List) value, (Map) test);
        } else {
            if (matches(value, test)) {
                return;
            }
            if (value instanceof JSONObject) {
                errorIfNot(test instanceof Map, "Cannot verify %s value [%s] against test [%s]", name, value, test);
                JSONObject json = (JSONObject) value;
                Map<String, ?> testMap = (Map) test;
                for (Map.Entry<?, ?> entry : testMap.entrySet()) {
                    Object testKey = entry.getKey();
                    Object testValue = entry.getValue();
                    Object attr = json.get(testKey);
                    verifyValue(S.concat(name, ".", testKey), attr, testValue);
                }
            } else if (value instanceof Elements) {
                if (test instanceof Map) {
                    verifyList(name, (Elements) value, (Map) test);
                } else {
                    Elements elements = (Elements) value;
                    if (elements.isEmpty()) {
                        value = null;
                    } else {
                        value = elements.first();
                    }
                    verifyValue(name, value, test);
                }
            } else if (value instanceof Number) {
                Number found = (Number) value;
                Number expected = null;
                if (test instanceof Number) {
                    expected = (Number) test;
                } else {
                    String s = S.string(test);
                    s = S.isNumeric(s) ? s : processStringSubstitution(s);
                    if (S.isNumeric(S.string(s))) {
                        expected = $.convert(s).to(Double.class);
                    } else {
                        error("Cannot verify %s value [%s] against test [%s]", name, value, test);
                    }
                }
                double delta = Math.abs(expected.doubleValue() - found.doubleValue());
                if ((delta / found.doubleValue()) > 0.001) {
                    error("Cannot verify %s value [%s] against test [%s]", name, value, test);
                }
            } else {
                // try convert the test into String
                String testString = $.convert(test).toString();
                boolean verified = verifyStringValue_(testString, value, test);
                if (!verified) {
                    String processedString = processStringSubstitution(testString);
                    if (S.neq(processedString, testString)) {
                        verified = verifyStringValue_(processedString, value, test);
                    }
                }
                errorIfNot(verified, "Cannot verify %s value [%s] against test [%s]", name, value, test);
            }
        }
    }

    private boolean verifyStringValue_(String testString, Object value, Object test) {
        if (matches(testString, value)) {
            return true;
        }
        if (null != value && ("*".equals(test) || "...".equals(test) || "<any>".equals(test))) {
            return true;
        }
        try {
            Pattern p = Pattern.compile(testString);
            return p.matcher(S.string(value)).matches();
        } catch (Exception e) {
            // ignore
        }
        Verifier v = tryLoadVerifier(testString);
        if (null != v && v.verify(value)) {
            return true;
        }
        return false;
    }

    private void verifyValue_(String name, Object value, List tests) {
        // try to do the literal match
        if (value instanceof List) {
            List found = (List) value;
            boolean ok = found.size() == tests.size();
            if (ok) {
                for (int i = 0; i < found.size(); ++i) {
                    Object foundElement = found.get(i);
                    Object testElement = tests.get(i);
                    if (!matches(foundElement, testElement)) {
                        ok = false;
                        break;
                    }
                }
            }
            if (ok) {
                return;
            }
        }
        // now try verifiers
        if (value instanceof Elements) {
            Elements elements = (Elements) value;
            if (elements.size() > 0) {
                value = elements.first();
            } else {
                value = null;
            }
        }
        for (Object test : tests) {
            errorIfNot(test instanceof Map, "Cannot verify %s value [%s] against test [%s]", name, value, test);
            Map<?, ?> map = (Map) test;
            errorIfNot(map.size() == 1, "Cannot verify %s value [%s] against test [%s]", name, value, test);
            Map.Entry entry = map.entrySet().iterator().next();
            Object entryValue = entry.getValue();
            if (entryValue instanceof String) {
                String s = (String) entryValue;
                Object processed = evalStr(s);
                if (s != processed) {
                    entry.setValue(processed);
                }
            }
            Verifier v = $.convert(map).to(Verifier.class);
            errorIf(null == v, "Cannot verify %s value [%s] against test [%s]", name, value, test);
            errorIf(!verify(v, value), "Cannot verify %s value [%s] against test [%s]", name, value, v);
        }
    }

    private boolean verify(Verifier test, Object value) {
        if (test.verify(value)) {
            return true;
        }
        if (value instanceof Element) {
            Element e = (Element) value;
            if (test.verify(e.val())) {
                return true;
            }
            if (test.verify(e.text())) {
                return true;
            }
            if (test.verify(e.html())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(Object a, Object b) {
        if ($.eq(a, b)) {
            return true;
        }
        if (!((b instanceof String) && (a instanceof Element))) {
            return false;
        }
        String test = S.string(b);
        Element element = (Element) a;
        // try html
        String html = element.html();
        if (S.eq(html, test, S.IGNORECASE)) {
            return true;
        }
        // try text
        String text = element.text();
        if (S.eq(text, test, S.IGNORECASE)) {
            return true;
        }
        // try val
        String val = element.val();
        if (S.eq(val, test, S.IGNORECASE)) {
            return true;
        }
        return false;
    }

    private Class<?> tryLoadClass(String name) {
        try {
            return null != app ? app.classForName(name) : $.classForName(name);
        } catch (Exception e) {
            return null;
        }
    }

    private Verifier tryLoadVerifier(String name) {
        boolean revert = false;
        if (name.startsWith("!") || name.startsWith("-")) {
            revert = true;
            name = name.substring(1).trim();
        } else if (name.startsWith("not:")) {
            revert = true;
            name = name.substring(4).trim();
        }
        Class<?> c = tryLoadClass(name);
        if (null != c) {
            if (Verifier.class.isAssignableFrom(c)) {
                final Verifier v = (Verifier) (null != app ? app.getInstance(c) : $.newInstance(c));
                return v.meOrReversed(revert);
            } else {
                throw new UnexpectedException("Class not supported: " + name);
            }
        }
        return null;
    }

    private Object getVal(String key, String ref) {
        Object stuff = getVal(key);
        if (S.blank(ref) || null == stuff) {
            return stuff;
        }
        if (stuff instanceof String) {
            String str = $.cast(stuff);
            if (str.contains("<") && str.contains(">")) {
                // try get html element
                try {
                    Document doc = Jsoup.parse(str, S.concat("http://localhost:", port, "/"));
                    Elements elements = doc.select(ref);
                    if (elements.size() != 0) {
                        return elements.get(0).text();
                    }
                } catch (Exception e) {
                    // just ignore
                }
            }
        }
        return JSONTraverser.traverse(stuff, ref);
    }

    private Object getVal(String key) {
        if ("last".equals(key)) {
            return lastData.get();
        }
        Object o = cache.get(key);
        if (null != o) {
            return o;
        }
        if (key.contains(".")) {
            String firstLevel = S.cut(key).beforeFirst(".");
            Object firstLevelVal = cache.get(firstLevel);
            if (null != firstLevelVal) {
                try {
                    return $.getProperty(firstLevelVal, S.cut(key).afterFirst("."));
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        key = S.underscore(key);
        o = constants.get(key);
        if (null != o) {
            return o;
        }
        o = Test.constant(key);
        if (null != o) {
            return o;
        }
        try {
            return evalFunc(key);
        } catch (Exception e) {
            if (!"last".equals(key)) {
                return getVal("last", key);
            }
            return null;
        }
    }

    Object evalStr(String s) {
        int n = s.indexOf("${");
        if (n < 0) {
            return s;
        }
        if (n > 0) {
            return processStringSubstitution(s);
        }
        int z = s.indexOf("}");
        if (z < s.length() - 1) {
            return processStringSubstitution(s);
        }
        String part = s.substring(2, z);
        if (part.contains("(") && part.endsWith(")")) {
            return evalFunc(part);
        } else {
            String key = part;
            String payload = "";
            if (part.contains(":")) {
                S.Binary binary = S.binarySplit(part, ':');
                key = binary.first();
                payload = binary.second();
            }
            return getVal(key, payload);
        }
    }

    String processStringSubstitution(String s) {
        int n = s.indexOf("${");
        if (n < 0) {
            return s;
        }
        if (n == 0 && s.endsWith(")}")) {
            s = s.substring(2);
            s = s.substring(0, s.length() - 1);
            return S.string(evalFunc(s));
        }
        int a = 0;
        int z = n;
        S.Buffer buf = S.buffer();
        while (true) {
            buf.append(s.substring(a, z));
            n = s.indexOf("}", z);
            a = n;
            String part = s.substring(z + 2, a);
            if (part.contains("(") && part.endsWith(")")) {
                buf.append(evalFunc(part));
            } else {
                String key = part;
                String payload = "";
                if (part.contains(":")) {
                    S.Binary binary = S.binarySplit(part, ':');
                    key = binary.first();
                    payload = binary.second();
                }
                buf.append(getVal(key, payload));
            }
            n = s.indexOf("${", a);
            if (n < 0) {
                buf.append(s.substring(a + 1));
                return buf.toString();
            }
            z = n;
        }
    }

    private Object getLastVal(String ref) {
        return getVal("last", ref);
    }

    static Scenario get() {
        return current.get();
    }
}
