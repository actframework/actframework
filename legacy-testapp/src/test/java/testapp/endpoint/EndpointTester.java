package testapp.endpoint;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.result.*;
import org.osgl.storage.ISObject;
import org.osgl.util.C;
import org.osgl.util.Codec;
import org.osgl.util.E;
import org.osgl.util.S;
import testapp.TestBase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EndpointTester extends TestBase {

    public static final String END_POINT = "http://localhost:6111";

    private OkHttpClient http;
    private Request.Builder req;
    private Response resp;
    protected ReqBuilder reqBuilder;

    private static Process process;

    @BeforeClass
    public static void bootup() throws Exception {
        if (ping()) {
            return;
        }
        process = new ProcessBuilder(
                "mvn","exec:exec").start();
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;

        while ((line = br.readLine()) != null) {
            System.out.println(line);
            if (line.contains("to start the app")) {
                break;
            }
        }
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (null != process) {
            shutdownApp();
            process.destroy();
        }
    }

    @Before
    public void setup() throws IOException {
        reset();
        _setup();
    }

    protected final void reset() {
        http = new OkHttpClient.Builder()
                .connectTimeout(60 * 10, TimeUnit.SECONDS)
                .readTimeout(60 * 10, TimeUnit.SECONDS)
                .writeTimeout(60 * 10, TimeUnit.SECONDS).build();
        req = null;
        resp = null;
        reqBuilder = null;
    }

    protected void _setup() throws IOException {}


    private void execute() throws IOException {
        resp = http.newCall(req().build()).execute();
    }

    protected Request.Builder req()  {
        if (null == req) {
            E.illegalStateIf(null == reqBuilder, "Please make sure url(String, Object...) method called first");
            req = reqBuilder.build();
        }
        return req;
    }

    protected Response resp() throws IOException {
        if (null == resp) {
            execute();
        }
        return resp;
    }

    protected List<Cookie> cookies() throws IOException {
        return Cookie.parseAll(HttpUrl.parse("http://localhost:6111"), resp().headers());
    }

    protected void verify(EndPointTestContext context) throws Exception {
        context.applyTo(this);
    }

    protected void verifyAllMethods(String expected, String url, String key, Object val, Object ... otherPairs) throws Exception {
        verifyGet(expected, url, key, val, otherPairs);
        verifyPostFormData(expected, url, key, val, otherPairs);
        verifyPostJsonBody(expected, url, key, val, otherPairs);
    }

    protected void verifyGet(String expected, String url, String key, Object val, Object ... otherPairs) throws Exception {
        setup();
        url(url).get(key, val, otherPairs);
        bodyEq(expected);
    }

    protected void verifyPostFormData(String expected, String url, String key, Object val, Object ... otherPairs) throws Exception {
        setup();
        url(url).post(key, val, otherPairs);
        bodyEq(expected);
    }

    protected void verifyPostJsonBody(String expected, String url, String key, Object val, Object ... otherPairs) throws Exception {
        setup();
        Map<String, Object> params = prepareJsonData(key, val, otherPairs);
        url(url).postJSON(params);
        bodyEq(expected);
    }

    public ReqBuilder url(String pathTmpl, Object ... args) {
        reqBuilder = new ReqBuilder(pathTmpl, args).header("Accept", "text/html");
        return reqBuilder;
    }

    protected void assertNoHeader(String header) throws IOException {
        assertNull(resp().header(header));
    }

    protected void checkHeader(String header, String expected) throws IOException {
        checkHeaderStr(expected, resp().header(header));
    }

    private void checkHeaderStr(String s1, String s0) {
        eq(C.setOf(S.string(s1).split("[,\\s+]")), C.setOf(S.string(s0).split("[,\\s+]")));
    }

    protected void bodyContains(String s) throws IOException {
        yes(resp().body().string().contains(s));
    }

    protected JSONArray bodyJSONArray() throws IOException {
        return JSON.parseArray(resp().body().string());
    }

    protected JSONObject bodyJSONObject() throws IOException {
        return JSON.parseObject(resp().body().string());
    }

    protected void bodyEq(String s) throws IOException {
        final Response resp = resp();
        checkResponseCode(resp);
        eq(s, S.string(resp.body().string()));
    }

    protected void bodyEq(String s1, String s2, String s3) throws IOException {
        final Response resp = resp();
        checkResponseCode(resp);
        String found = resp.body().string();
        if (S.neq(s3, found)) {
            if (S.neq(s2, found)) {
                eq(s1, found);
            }
        }
    }

    protected void bodyEqIgnoreSpace(String s) throws IOException {
        final Response resp = resp();
        eq(200, resp.code());
        eq(s.trim(), S.string(resp.body().string()).trim());
    }

    protected void bodyEq(Object obj) throws IOException {
        bodyEq(JSON.toJSONString(obj));
    }

    protected void bodyEqIgnoreSpace(Object obj) throws IOException {
        bodyEqIgnoreSpace(JSON.toJSONString(obj));
    }

    protected void checkRespCode() throws IOException {
        checkResponseCode(resp());
    }

    protected void responseCodeIs(int code) throws IOException {
        eq(code, resp().code());
    }


    protected Map<String, Object> prepareJsonData(String key, Object val, Object ... otherPairs) {
        Map<String, Object> params = C.newMap(key, val);
        Map<String, Object> otherParams = C.Map(otherPairs);
        params.putAll(otherParams);
        return params;
    }

    protected Map<String, Object> prepareJsonData(List<$.T2<String, Object>> params) {
        Map<String, Object> map = C.newMap();
        if (null != params) {
            for ($.T2<String, Object> pair : params) {
                String key = pair._1;
                Object val = pair._2;
                if (map.containsKey(key)) {
                    List list;
                    Object x = map.get(key);
                    if (x instanceof List) {
                        list = $.cast(x);
                    } else {
                        list = C.newList(x);
                        map.put(key, list);
                    }
                    list.add(val);
                } else {
                    map.put(key, val);
                }
            }
        }
        return map;
    }

    private static void shutdownApp() throws Exception {
        try {
            OkHttpClient http = new OkHttpClient();
            Request req = new ReqBuilder("/shutdown").build().build();
            Response resp = http.newCall(req).execute();
            System.out.println(resp.code());
        } catch (Exception e) {
            // ignore
        }
    }

    private static boolean ping() {
        if (true) {
            return true;
        }
        try {
            OkHttpClient http = new OkHttpClient();
            Request req = new ReqBuilder("/ping").build().build();
            Response resp = http.newCall(req).execute();
            return resp.code() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static String fullUrl(String pathTmpl, Object ... args) {
        String tmpl0 = pathTmpl.startsWith("/") ? pathTmpl : "/" + pathTmpl;
        return S.fmt(END_POINT + tmpl0, args);
    }

    protected final void checkResponseCode(Response resp) {
        if (resp.code() < 300 && resp.code() > 199) {
            return;
        }
        switch (resp.code()) {
            case HttpURLConnection.HTTP_NOT_FOUND:
                throw NotFound.INSTANCE;
            case HttpURLConnection.HTTP_BAD_REQUEST:
                throw BadRequest.INSTANCE;
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw Forbidden.INSTANCE;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                throw Unauthorized.INSTANCE;
            case HttpURLConnection.HTTP_NOT_MODIFIED:
                throw NotModified.INSTANCE;
            case HttpURLConnection.HTTP_CONFLICT:
                throw Conflict.INSTANCE;
            default:
                H.Status status = H.Status.of(resp.code());
                if (status.isError()) {
                    throw new ErrorResult(status);
                }
        }
    }

    protected static class ReqBuilder {
        StringBuilder sb;
        boolean paramAttached;
        private H.Method method = H.Method.GET;
        private H.Format format;
        private H.Format accept;
        private RequestBody body;
        private String postStr;
        private String session;
        private String csrf;
        private byte[] postBytes;
        private ISObject postAttachment;
        private List<Cookie> cookies = C.newList();
        private Map<String, String> headers = C.newMap();
        private List<$.T2<String, Object>> postParams = C.newList();

        public ReqBuilder(String pathTmpl, Object ... args) {
            String s = fullUrl(pathTmpl, args);
            sb = S.builder(s);
            paramAttached = s.contains("?");
        }

        public ReqBuilder cookies(List<Cookie> cookies) {
            this.cookies.addAll(cookies);
            return this;
        }

        public ReqBuilder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public ReqBuilder get() {
            return method(H.Method.GET);
        }

        public ReqBuilder getJSON() {
            return method(H.Method.GET).format(H.Format.JSON).accept(H.Format.JSON);
        }

        public ReqBuilder get(String key, Object val, Object ... morePairs) {
            params(key, val, morePairs);
            return get();
        }

        public ReqBuilder get(List<$.T2<String, Object>> pairs) {
            params(pairs);
            return get();
        }

        public ReqBuilder post() {
            return method(H.Method.POST);
        }

        public ReqBuilder post(String key, Object val, Object... morePairs) {
            format(H.Format.FORM_URL_ENCODED);
            post();
            return params(key, val, morePairs);
        }

        public ReqBuilder post(List<$.T2<String, Object>> pairs) {
            format(H.Format.FORM_URL_ENCODED);
            post();
            return params(pairs);
        }

        public ReqBuilder put() {
            return method(H.Method.PUT);
        }

        public ReqBuilder delete() {
            return method(H.Method.DELETE);
        }

        public ReqBuilder options() {
            return method(H.Method.OPTIONS);
        }

        public ReqBuilder method(H.Method method) {
            this.method = method;
            return this;
        }

        public H.Method method() {
            return this.method;
        }

        public ReqBuilder format(H.Format format) {
            this.format = format;
            return this;
        }

        public ReqBuilder accept(H.Format format) {
            this.accept = format;
            return this;
        }

        public ReqBuilder body(RequestBody body) {
            this.body = body;
            return this;
        }

        public ReqBuilder param(String key, Object val) {
            E.illegalArgumentIf(S.blank(key));
            if (method == H.Method.GET) {
                sb.append(paramAttached ? "&" : "?");
                paramAttached = true;
                sb.append(key).append("=").append(Codec.encodeUrl(S.string(val)));
            } else {
                postParams.add($.T2(key, val));
            }
            return this;
        }

        public ReqBuilder params(String key, Object val, Object ... otherPairs) {
            E.illegalArgumentIf(otherPairs.length % 2 != 0);
            param(key, val);
            int len = otherPairs.length;
            for (int i = 0; i < len - 1; i += 2) {
                String key0 = S.string(otherPairs[i]);
                param(key0, otherPairs[i + 1]);
            }
            return this;
        }

        public ReqBuilder params(List<$.T2<String, Object>> pairs) {
            if (null != pairs) {
                for ($.T2<String, Object> pair : pairs) {
                    param(pair._1, pair._2);
                }
            }
            return this;
        }

        public ReqBuilder post(String content) {
            this.post();
            this.postStr = content;
            return this;
        }

        public ReqBuilder postJSON(Object content) {
            this.postParams.clear();
            this.postBytes = null;
            this.format = H.Format.JSON;
            this.post();
            post(JSON.toJSONString(content));
            return this;
        }

        public ReqBuilder post(byte[] content) {
            this.post();
            this.postBytes = content;
            return this;
        }

        public ReqBuilder post(ISObject content) {
            this.post();
            this.postAttachment = content;
            return this;
        }

        public Request.Builder build() {
            Request.Builder builder = new Request.Builder().url(sb.toString());
            if (this.format == H.Format.JSON) {
                builder.addHeader("Content-Type", "application/json");
            }
            if (this.accept == H.Format.JSON) {
                builder.addHeader("Accept", "application/json");
            }
            if (!this.headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            if (!this.cookies.isEmpty()) {
                builder.header("Cookie", cookieHeader());
            }
            switch (method) {
                case GET:
                    return builder.get();
                case POST:
                    return builder.post(body());
                case PUT:
                    return builder.put(body());
                case DELETE:
                    return builder.delete(body());
                case PATCH:
                    return builder.patch(body());
                case OPTIONS:
                    return builder.method("OPTIONS", null);
                default:
                    return builder;
            }
        }

        private String cookieHeader() {
            StringBuilder cookieHeader = new StringBuilder();
            for (int i = 0, size = cookies.size(); i < size; i++) {
                if (i > 0) {
                    cookieHeader.append("; ");
                }
                Cookie cookie = cookies.get(i);
                cookieHeader.append(cookie.name()).append('=').append(cookie.value());
            }
            return cookieHeader.toString();
        }

        private RequestBody body() {
            if (null == body) {
                body = buildBody();
            }
            return body;
        }

        private MediaType mediaType() {
            return MediaType.parse(format.contentType());
        }

        private RequestBody buildBody() {
            if (format == H.Format.JSON) {
                return buildJsonBody();
            } else {
                return buildFormEncoded();
            }
        }

        private RequestBody buildJsonBody() {
            if (S.notBlank(postStr)) {
                return RequestBody.create(mediaType(), postStr);
            } else {
                Map<String, Object> map = new HashMap<String, Object>();
                for ($.T2<String, Object> entry : postParams) {
                    map.put(entry._1, entry._2);
                }
                return RequestBody.create(mediaType(), JSON.toJSONString(map));
            }
        }

        private RequestBody buildFormEncoded() {
            FormBody.Builder builder = new FormBody.Builder();
            for ($.T2<String, Object> entry : postParams) {
                String val = S.string(entry._2);
                if (this.method == H.Method.GET) {
                    val = Codec.encodeUrl(val);
                }
                builder.add(entry._1, val);
            }
            return builder.build();
        }

    }

}
