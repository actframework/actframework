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

import static act.e2e.E2EStatus.PENDING;
import static act.e2e.util.ErrorMessage.*;
import static org.osgl.http.H.Header.Names.ACCEPT;
import static org.osgl.http.H.Header.Names.X_REQUESTED_WITH;
import static org.osgl.http.H.Method.POST;

import act.Act;
import act.app.App;
import act.e2e.func.Func;
import act.e2e.req_modifier.RequestModifier;
import act.e2e.util.CookieStore;
import act.e2e.util.JSONTraverser;
import act.e2e.util.RequestTemplateManager;
import act.e2e.util.ScenarioManager;
import act.e2e.verifier.Verifier;
import act.handler.builtin.FileGetter;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Scenario implements ScenarioPart {

    private static final Logger LOGGER = E2E.LOGGER;

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
            String url = S.concat("http://localhost:", port, S.ensure(processStringSubstitution(requestSpec.url)).startWith("/"));
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
                case DELETE:
                    builder.method(requestSpec.method.name(), null);
                    break;
                case POST:
                case PUT:
                case PATCH:
                    RequestBody body = EMPTY_BODY;
                    String jsonBody = verifyJsonBody(requestSpec.json);
                    if (S.notBlank(jsonBody)) {
                        body = RequestBody.create(MediaType.parse("application/json"), jsonBody);
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
                            String path = S.pathConcat("e2e/upload", '/', val);
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
                        entry.setValue(sVal);
                    }
                } else if (val instanceof Map) {
                    processParamSubstitution((Map) val);
                }
            }
        }

        Request build() {
            return builder.build();
        }

        private String verifyJsonBody(Object jsonBody) {
            if (jsonBody instanceof Map) {
                processParamSubstitution((Map) jsonBody);
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

    }


    private int port = 5460;
    private OkHttpClient http;
    private CookieStore cookieStore;
    private App app;
    public String name;
    public String description;
    public List<String> fixtures = new ArrayList<>();
    public List<String> depends = new ArrayList<>();
    public List<Interaction> interactions = new ArrayList<>();
    public Map<String, Object> constants = new HashMap<>();
    public E2EStatus status = PENDING;
    public String errorMessage;
    public Throwable cause;

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
        return title();
    }

    public String title() {
        return S.blank(description) ? name : description;
    }

    public void cache(String name, Object payload) {
        cache.put(name, payload);
    }

    public Object cached(String name) {
        return cache.get(name);
    }

    public E2EStatus statusOf(Interaction interaction) {
        return interaction.status;
    }

    public String errorMessageOf(Interaction interaction) {
        return interaction.errorMessage;
    }

    @Override
    public void validate(Scenario scenario) throws UnexpectedException {
        errorIf(S.blank(name), "Scenario name not defined");
        errorIf(interactions.isEmpty(), "No interactions defined in Scenario[%s]", scenario.name);
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
        return null == o ? E2E.constant(key) : o;
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

    public void start(ScenarioManager scenarioManager, RequestTemplateManager requestTemplateManager) {
        this.scenarioManager = $.requireNotNull(scenarioManager);
        this.requestTemplateManager = $.requireNotNull(requestTemplateManager);
        this.status = PENDING;
        current.set(this);
        validate(this);
        prepareHttp();
        boolean pass = reset() && run();
        this.status = E2EStatus.of(pass);
    }

    public void clearSession() {
        if (depends.isEmpty()) {
            cookieStore().clear();
        }
    }

    public boolean clearFixtures() {
        return verify(RequestSpec.RS_CLEAR_FIXTURE, "clearing fixtures");
    }

    public String causeStackTrace() {
        return null == cause ? null: E.stackTrace(cause);
    }

    void resolveRequest(RequestSpec req) {
        req.resolveParent(requestTemplateManager);
    }

    Response sendRequest(RequestSpec req) throws IOException {
        Request httpRequest = new RequestBuilder(req).build();
        Response resp = http.newCall(httpRequest).execute();
        lastHeaders.set(resp.headers());
        return resp;
    }

    private boolean createFixtures() {
        if (fixtures.isEmpty()) {
            return true;
        }
        RequestSpec req = RequestSpec.loadFixtures(fixtures);
        return verify(req, "creating fixtures");
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
        long timeout = "e2e".equalsIgnoreCase(Act.profile()) ? 10 : 60 * 60;
        http = new OkHttpClient.Builder()
                .cookieJar(cookieStore())
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    private boolean reset() {
        errorMessage = null;
        clearSession();
        if (depends.isEmpty()) {
            return clearFixtures() && createFixtures();
        }
        return true;
    }

    private boolean run() {
        if (status.finished()) {
            return status.pass();
        }
        return runDependents() && runInteractions();
    }

    private boolean runDependents() {
        for (String dependent : depends) {
            Scenario scenario = scenarioManager.get(dependent);
            if (!scenario.run()) {
                errorMessage = "dependency failure: " + dependent;
                return false;
            }
            inheritFrom(scenario);
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

    private boolean runInteractions() {
        for (Interaction interaction : interactions) {
            boolean pass = run(interaction);
            if (!pass) {
                //errorMessage = S.fmt("interaction[%s] failure", interaction.description);
                return false;
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
                cache.put(entry.getKey(), value);
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

    void verifyDownload(Response response, String checksum) {
        Object o = cached(checksum);
        if (null == o) {
            o = cached("checksum-last");
        }
        if (null == o) {
            o = checksum;
        }
        String downloadChecksum = IO.checksum(response.body().byteStream());
        errorIfNot($.eq(downloadChecksum, o), "Download checksum not match");
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
            if ("size".equals(key) || "len".equals(key) || "length".equals(key)) {
                value = array.size();
            } else if ("toString".equals(key) || "string".equals(key) || "str".equals(key)) {
                value = JSON.toJSONString(array);
            } else if ("?".equals(key) || "<any>".equalsIgnoreCase(sKey)) {
                for (Object arrayElement : array) {
                    try {
                        verifyValue(name, arrayElement, test);
                        return;
                    } catch (Exception e) {
                        // try next one
                    }
                }
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
            Object value = $.getProperty(obj, key);
            verifyValue(key, value, entry.getValue());
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
                errorIfNot(test instanceof Map, "Cannot verify %s value[%s] with test [%s]", name, value, test);
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
                        error("Cannot verify %s value[%s] against test [%s]", name, value, test);
                    }
                }
                double delta = Math.abs(expected.doubleValue() - found.doubleValue());
                if ((delta / found.doubleValue()) > 0.001) {
                    error("Cannot verify %s value[%s] against test [%s]", name, value, test);
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
                errorIfNot(verified, "Cannot verify %s value[%s] with test [%s]", name, value, test);
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
            errorIfNot(test instanceof Map, "Cannot verify %s value[%s] against test[%s]", name, value, test);
            Map<?, ?> map = (Map) test;
            errorIfNot(map.size() == 1, "Cannot verify %s value[%s] against test[%s]", name, value, test);
            Map.Entry entry = map.entrySet().iterator().next();
            Object entryValue = entry.getValue();
            if (entryValue instanceof String) {
                String s = (String) entryValue;
                String processed = processStringSubstitution(s);
                if (S.neq(processed, s)) {
                    entry.setValue(processed);
                }
            }
            Verifier v = $.convert(map).to(Verifier.class);
            errorIf(null == v, "Cannot verify %s value[%s] against test[%s]", name, value, test);
            errorIf(!verify(v, value), "Cannot verify %s value[%s] against test[%s]", name, value, v);
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
        Class<?> c = tryLoadClass(name);
        if (null != c) {
            if (Verifier.class.isAssignableFrom(c)) {
                return (Verifier) (null != app ? app.getInstance(c) : $.newInstance(c));
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
        key = S.underscore(key);
        o = constants.get(key);
        if (null != o) {
            return o;
        }
        o = E2E.constant(key);
        if (null != o) {
            return o;
        }
        try {
            return evalFunc(key);
        } catch (Exception e) {
            return null;
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
            E.illegalArgumentIf(n < -1, "Invalid string: " + s);
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
