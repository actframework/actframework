package testapp;

import com.alibaba.fastjson.JSON;
import okhttp3.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.osgl.http.H;
import org.osgl.storage.ISObject;
import org.osgl.util.C;
import org.osgl.util.Codec;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    public void setup() {
        http = new OkHttpClient.Builder()
                .connectTimeout(60 * 10, TimeUnit.SECONDS)
                .readTimeout(60 * 10, TimeUnit.SECONDS)
                .writeTimeout(60 * 10, TimeUnit.SECONDS).build();
        req = null;
        resp = null;
        reqBuilder = null;
    }


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

    protected void notFoundByAllMethods(String url, String key, Object val, Object ... otherPairs) throws Exception {
        notFoundByGet(url, key, val, otherPairs);
        notFoundByPostFormData(url, key, val, otherPairs);
        notFoundByPostJsonBody(url, key, val, otherPairs);
    }

    protected void notFoundByGet(String url, String key, Object val, Object ... otherPairs) throws Exception {
        setup();
        url(url).get(key, val, otherPairs);
        notFound();
    }

    protected void notFoundByPostFormData(String url, String key, Object val, Object ... otherPairs) throws Exception {
        setup();
        url(url).post(key, val, otherPairs);
        notFound();
    }

    protected void notFoundByPostJsonBody(String url, String key, Object val, Object ... otherPairs) throws Exception {
        setup();
        Map<String, Object> params = prepareJsonData(key, val, otherPairs);
        url(url).postJSON(params);
        notFound();
    }

    protected void badRequestByAllMethods(String url, String key, Object val, Object ... otherPairs) throws Exception {
        badRequestByGet(url, key, val, otherPairs);
        badRequestByPostFormData(url, key, val, otherPairs);
        badRequestByPostJsonBody(url, key, val, otherPairs);
    }

    protected void badRequestByGet(String url, String key, Object val, Object ... otherPairs) throws Exception {
        setup();
        url(url).get(key, val, otherPairs);
        badRequest();
    }

    protected void badRequestByPostFormData(String url, String key, Object val, Object ... otherPairs) throws Exception {
        setup();
        url(url).post(key, val, otherPairs);
        badRequest();
    }

    protected void badRequestByPostJsonBody(String url, String key, Object val, Object ... otherPairs) throws Exception {
        setup();
        Map<String, Object> params = prepareJsonData(key, val, otherPairs);
        url(url).postJSON(params);
        badRequest();
    }

    protected ReqBuilder url(String pathTmpl, Object ... args) {
        reqBuilder = new ReqBuilder(pathTmpl, args);
        return reqBuilder;
    }

    protected void bodyContains(String s) throws IOException {
        yes(resp().body().string().contains(s));
    }

    protected void bodyEq(String s) throws IOException {
        final Response resp = resp();
        eq(200, resp.code());
        eq(s, S.string(resp.body().string()));
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

    protected void notFound() throws IOException {
        final Response resp = resp();
        eq(404, resp.code());
    }

    protected void badRequest() throws IOException {
        final Response resp = resp();
        eq(400, resp.code());
    }

    private Map<String, Object> prepareJsonData(String key, Object val, Object ... otherPairs) {
        Map<String, Object> params = C.newMap(key, val);
        Map<String, Object> otherParams = C.map(otherPairs);
        params.putAll(otherParams);
        return params;
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

    protected static class ReqBuilder {
        StringBuilder sb;
        boolean paramAttached;
        private H.Method method = H.Method.GET;
        private H.Format format = H.Format.JSON;
        private RequestBody body;
        private String postStr;
        private byte[] postBytes;
        private ISObject postAttachment;
        private Map<String, Object> postParams = C.newMap();
        public ReqBuilder(String pathTmpl, Object ... args) {
            String s = fullUrl(pathTmpl, args);
            sb = S.builder(s);
            paramAttached = s.contains("?");
        }

        public ReqBuilder get() {
            return method(H.Method.GET);
        }

        public ReqBuilder get(String key, Object val, Object ... morePairs) {
            params(key, val, morePairs);
            return get();
        }

        public ReqBuilder post() {
            return method(H.Method.POST);
        }

        public ReqBuilder post(String key, Object val, Object... morePairs) {
            params(key, val, morePairs);
            return post();
        }

        public ReqBuilder put() {
            return method(H.Method.PUT);
        }

        public ReqBuilder delete() {
            return method(H.Method.DELETE);
        }

        public ReqBuilder method(H.Method method) {
            this.method = method;
            return this;
        }

        public ReqBuilder format(H.Format format) {
            this.format = format;
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
                postParams.put(key, val);
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
                default:
                    return builder;
            }
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
                return RequestBody.create(mediaType(), JSON.toJSONString(postParams));
            }
        }

        private RequestBody buildFormEncoded() {
            FormBody.Builder builder = new FormBody.Builder();
            for (Map.Entry<String, Object> entry : postParams.entrySet()) {
                builder.add(entry.getKey(), S.string(entry.getValue()));
            }
            return builder.build();
        }

    }

}
