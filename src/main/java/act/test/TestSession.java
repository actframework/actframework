package act.test;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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
import act.metric.Metric;
import act.metric.MetricInfo;
import act.metric.Timer;
import act.test.func.Func;
import act.test.util.*;
import act.test.verifier.Verifier;
import act.util.LogSupport;
import act.util.ProgressGauge;
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
import org.osgl.util.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static act.test.Test.PG_PAYLOAD_FAILED;
import static act.test.util.ErrorMessage.errorIf;
import static act.test.util.ErrorMessage.errorIfNot;

/**
 * A single test session encapsulate a full cycle of test run including
 *
 * * clear fixture
 * * run dependency scenarios
 * * run target scenario
 *
 * For each scenario run it contains:
 *
 * * load fixtures
 * * run scenario
 *
 */
public class TestSession extends LogSupport {

    private final static ThreadLocal<TestSession> current = new ThreadLocal<>();

    static TestSession current() {
        return current.get();
    }

    private boolean proceed;
    private Scenario running;
    private Scenario target;
    private List<Scenario> dependencies = new ArrayList<>();
    private App app;
    private int port = 5460;
    private OkHttpClient http;
    private CookieStore cookieStore;
    private transient Metric metric = Act.metricPlugin().metric(MetricInfo.ACT_TEST_SCENARIO);

    $.Var<Object> lastData = $.var();
    $.Var<Headers> lastHeaders = $.var();
    public Map<String, Object> constants = new HashMap<>();

    Map<String, Object> cache = new HashMap<>();

    public RequestTemplateManager requestTemplateManager;

    public TestSession(Scenario scenario, RequestTemplateManager requestTemplateManager) {
        this.requestTemplateManager = requestTemplateManager;
        dependencies.addAll(scenario.allDepends);
        target = scenario;
        app = Act.app();
        if (null != app) {
            port = app.config().httpPort();
        }
    }

    public void cache(String name, Object payload) {
        cache.put(name, payload);
    }

    public Object cached(String name) {
        return cache.get(name);
    }

    public int port() {
        return port;
    }

    public void run(ProgressGauge gauge) {
        current.set(this);
        prepareHttp();
        gauge.incrMaxHintBy(dependencies.size() + 2);
        reset();
        gauge.step();
        proceed = runAll(gauge, dependencies);
        if (proceed) {
            proceed = runOne(target, gauge);
        }
        gauge.step();
    }

    private boolean reset() {
        Collections.sort(dependencies, new ScenarioComparator(target.partition));
        for (Scenario scenario : dependencies) {
            scenario.reset();
        }
        target.reset();
        clearSession();
        return clearFixtures();
    }

    public void clearSession() {
        cookieStore().clear();
    }

    public boolean clearFixtures() {
        if (isTraceEnabled()) {
            trace("clear fixture for " + target.name);
        }
        Timer timer = metric.startTimer("clear-fixtures");
        try {
            return verify(RequestSpec.RS_CLEAR_FIXTURE, "clearing fixtures");
        } finally {
            timer.stop();
        }
    }

    private boolean runAll(ProgressGauge gauge, List<Scenario> scenarios) {
        boolean proceed = true;
        for (Scenario scenario : scenarios) {
            proceed = proceed && runOne(scenario, gauge);
            gauge.step();
            if (!proceed) {
                break;
            }
        }
        return proceed;
    }

    private boolean runOne(Scenario scenario, ProgressGauge gauge) {
        gauge.clearPayload();
        gauge.setPayload(Test.PG_PAYLOAD_SCENARIO, scenario.title());
        boolean okay;
        try {
            running = $.requireNotNull(scenario);
            scenario.validate(this);
            constants.putAll(running.constants);
            okay = createFixtures() && scenario.run(this, gauge);
            scenario.status = TestStatus.of(okay);
        } catch (RuntimeException e) {
            okay = false;
            String message = e.getMessage();
            scenario.errorMessage = S.blank(message) ? e.getClass().getName() : message;
            scenario.cause = e.getCause();
            scenario.status = TestStatus.FAIL;
        }
        gauge.setPayload(PG_PAYLOAD_FAILED, !scenario.status.pass());
        return okay;
    }

    Response sendRequest(RequestSpec req) throws IOException {
        Timer timer = metric.startTimer("build-request");
        Request httpRequest = new RequestBuilder(req, this, port).build();
        timer.stop();
        timer = metric.startTimer("send-request");
        Response resp = http.newCall(httpRequest).execute();
        timer.stop();
        lastHeaders.set(resp.headers());
        return resp;
    }

    private boolean createFixtures() {
        if (running.fixtures.isEmpty()) {
            return true;
        }
        Timer timer = metric.startTimer("create-fixtures");
        try {
            RequestSpec req = RequestSpec.loadFixtures(running.fixtures);
            return verify(req, "creating fixtures");
        } finally {
            timer.stop();
        }
    }


    void verifyDownloadChecksum(Response response, String checksumExpected) {
        if (checksumExpected.startsWith("${")) {
            checksumExpected = S.string(evalStr(checksumExpected));
        }
        Object o = cached(checksumExpected);
        if (null == o) {
            o = cached("checksum-last");
        }
        if (null == o) {
            o = checksumExpected;
        }
        ResponseBody body = response.body();
        errorIf(null == body, "No download found");
        String downloadChecksum = IO.checksum(body.byteStream());
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

    Scenario scenario() {
        return null == running ? target : running;
    }

    boolean verify(RequestSpec req, String operation) {
        boolean pass = true;
        Response resp = null;
        try {
            resp = sendRequest(req);
            if (!resp.isSuccessful()) {
                pass = false;
                scenario().errorMessage = "Fixtures loading failure";
            }
            return pass;
        } catch (IOException e) {
            scenario().errorMessage = "Error " + operation;
            ErrorMessage.error(e, scenario().errorMessage);
            return false;
        } finally {
            IO.close(resp);
        }
    }

    private void prepareHttp() {
        if (isTraceEnabled()) {
            trace("prepareHTTP for scenario: " + target.name);
        }
        long timeout = app.config().testTimeout();
        http = new OkHttpClient.Builder()
                .cookieJar(cookieStore())
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    private synchronized CookieStore cookieStore() {
        if (null == cookieStore) {
            App app = Act.app();
            cookieStore = null == app ? new CookieStore() : app.getInstance(CookieStore.class);
        }
        return cookieStore;
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

    Object evalFunc(String funcExpr) {
        String funcName = funcExpr;
        List<String> vals = C.list();
        if (funcExpr.contains("(")) {
            funcName = S.cut(funcExpr).beforeFirst("(");
            String paramStr = S.cut(funcExpr).afterFirst("(");
            paramStr = S.cut(paramStr).beforeLast(")");
            if (S.notBlank(paramStr)) {
                if (S.is(paramStr).wrappedWith(S.DOUBLE_QUOTES) || S.is(paramStr).wrappedWith(S.SINGLE_QUOTES)) {
                    paramStr = paramStr.substring(1, paramStr.length() - 1);
                    vals = C.list(paramStr);
                } else {
                    vals = C.newList(S.fastSplit(paramStr, ","));
                    for (int i = 0; i < vals.size(); ++i) {
                        String val = S.ensure(vals.get(i).trim()).strippedOff(S.DOUBLE_QUOTES);
                        val = processStringSubstitution(val);
                        vals.set(i, val);
                    }
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

    Object eval(String expr) {
        if (expr.startsWith("func:")) {
            return evalFunc(expr.substring(5));
        } else if (expr.contains("(")) {
            return evalFunc(expr);
        }
        String key = S.underscore(expr);
        Object o = constants.get(key);
        o = null == o ? cache.get(key) : o;
        o = null == o ? Test.constant(key) : o;
        try {
            // final try - treat it as a function
            return null == o ? evalFunc(expr) : o;
        } catch (IllegalArgumentException e) {
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
        String underscoredKey = S.underscore(key);
        o = constants.get(underscoredKey);
        if (null != o) {
            return o;
        }
        o = Test.constant(underscoredKey);
        if (null != o) {
            return o;
        }
        try {
            return evalFunc(key);
        } catch (Exception e) {
            try {
                return getLastVal(key);
            } catch (Exception e1) {
                throw E.unexpected("Unable to get value by key: %s", key);
            }
        }
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

    Object getLastVal(String ref) {
        return getVal("last", ref);
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
                ErrorMessage.error("Unknown JSON string: \n%s", bodyString);
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
                throw ErrorMessage.error("No element matches requirement: %s", test);
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
                    throw ErrorMessage.error("Unknown attribute of array verification: %s", key);
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
                if (val instanceof CharSequence) {
                    val = S.wrap(val).with(S.DOUBLE_QUOTES);
                }
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
            } else if (value instanceof Long) {
                Long lng = (Long) value;
                Long expected = null;
                if (test instanceof Long) {
                    expected = (Long) test;
                } else {
                    String s = S.string(test);
                    s = S.isIntOrLong(s) ? s : processStringSubstitution(s);
                    ErrorMessage.errorIfNot(S.isIntOrLong(s), "Cannot verify %s value [%s] against test", name, value, test);
                    expected = $.convert(s).toLong();
                }
                ErrorMessage.errorIfNot(lng.equals(expected), "Cannot verify %s value [%s] against test [%s]", name, value, test);
            } else if (value instanceof Integer) {
                Integer integer = (Integer) value;
                Integer expected = null;
                if (test instanceof Integer) {
                    expected = (Integer) test;
                } else {
                    String s = S.string(test);
                    s = S.isInt(s) ? s : processStringSubstitution(s);
                    ErrorMessage.errorIfNot(S.isInt(s), "Cannot verify %s value [%s] against test", name, value, test);
                    expected = $.convert(s).toInteger();
                }
                ErrorMessage.errorIfNot(integer.equals(expected), "Cannot verify %s value [%s] against test [%s]", name, value, test);
            } else if (value instanceof Number) {
                Number found = (Number) value;
                Number expected = null;
                if (test instanceof Number) {
                    expected = (Number) test;
                } else {
                    String s = S.string(test);
                    s = S.isNumeric(s) ? s : processStringSubstitution(s);
                    ErrorMessage.errorIfNot(S.isNumeric(S.string(s)), "Cannot verify %s value [%s] against test [%s]", name, value, test);
                    expected = $.convert(s).to(Double.class);
                }
                double delta = Math.abs(expected.doubleValue() - found.doubleValue());
                if ((delta / found.doubleValue()) > 0.001) {
                    ErrorMessage.error("Cannot verify %s value [%s] against test [%s]", name, value, test);
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
        } else if (name.startsWith("not:") || name.startsWith("not ")) {
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

}
