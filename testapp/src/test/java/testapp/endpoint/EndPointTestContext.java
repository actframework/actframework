package testapp.endpoint;

import org.osgl.$;
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
                tester.url(context.url).get(context.params);
                tester.bodyEq(context.expected);
            }
        },
        POST_FORM_DATA() {
            @Override
            public void verify(EndpointTester tester, EndPointTestContext context) throws Exception {
                tester.setup();
                tester.url(context.url).post(context.params);
                tester.bodyEq(context.expected);
            }
        }, POST_JSON () {
            @Override
            public void verify(EndpointTester tester, EndPointTestContext context) throws Exception {
                tester.setup();
                Map<String, Object> params = tester.prepareJsonData(context.params);
                tester.url(context.url).postJSON(params);
                tester.bodyEq(context.expected);
            }
        };

        public abstract void verify(EndpointTester tester, EndPointTestContext context) throws Exception;
    }

    private String url;
    private String expected;
    private List<$.T2<String, Object>> params;
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
