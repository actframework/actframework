package testapp.endpoint;

import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.List;
import java.util.Map;

public class EndPointTestContext {

    public enum RequestMethod {
        GET() {
            @Override
            public void verify(EndpointTester tester, EndPointTestContext context) throws Exception {
                tester.setup();
                tester.url(context.url).get(context.params).accept(context.accept);
                tester.bodyEq(context.expected);
            }
        },
        POST_FORM_DATA() {
            @Override
            public void verify(EndpointTester tester, EndPointTestContext context) throws Exception {
                tester.setup();
                tester.url(context.url).accept(context.accept).post(context.params);
                tester.bodyEq(context.expected);
            }
        }, POST_JSON () {
            @Override
            public void verify(EndpointTester tester, EndPointTestContext context) throws Exception {
                tester.setup();
                Object payload = context.jsonBody;
                if (null != payload) {
                    tester.url(context.url).postJSON(payload);
                } else {
                    Map<String, Object> params = tester.prepareJsonData(context.params);
                    tester.url(context.url).postJSON(params);
                }
                if (null != context.accept) {
                    tester.reqBuilder.accept(context.accept);
                }
                tester.bodyEq(context.expected, context.expected2, context.expected3);
            }
        };

        public abstract void verify(EndpointTester tester, EndPointTestContext context) throws Exception;
    }

    private String url;
    private String expected;
    // Our JSON deserializer will convert double into BigDecimal, thus
    // the output string will be different, which should be acceptable
    // E.g. double value: "1.7976931348623157E308" should equals to
    //      big decimal value: "1.7976931348623157E+308"
    private String expected2;
    // plus the Set of big decimals
    private String expected3;
    private List<$.T2<String, Object>> params;
    private Object jsonBody;
    private H.Format accept;
    private RequestMethod requestMethod;

    public EndPointTestContext() {}

    public EndPointTestContext url(String url) {
        this.url = $.notNull(url);
        return this;
    }

    public EndPointTestContext expected(String expected) {
        this.expected = expected;
        return this;
    }

    public EndPointTestContext expected(String expected, String expected2, String expected3) {
        this.expected = expected;
        this.expected2 = expected2;
        this.expected3 = expected3;
        return this;
    }

    public EndPointTestContext jsonBody(Object body) {
        this.jsonBody = body;
        return this;
    }

    public EndPointTestContext accept(H.Format format){
        this.accept = format;
        return this;
    }

    public EndPointTestContext params(List<$.T2<String, Object>> params) {
        this.params = params;
        return this;
    }

    public EndPointTestContext params(String key, Object val, Object ... otherPairs) {
        E.illegalArgumentIf(otherPairs.length % 2 != 0);
        this.params = C.newSizedList(1 + otherPairs.length / 2);
        this.params.add($.T2(key, val));
        for (int i = 0; i < otherPairs.length - 1; ++i) {
            this.params.add($.T2(S.string(otherPairs[i]), otherPairs[i+1]));
        }
        return this;
    }

    public EndPointTestContext method(RequestMethod requestMethod) {
        this.requestMethod = requestMethod;
        return this;
    }

    public void applyTo(EndpointTester tester) throws Exception {
        requestMethod.verify(tester, this);
    }
}
