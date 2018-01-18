package testapp.endpoint;

import org.junit.Test;

public class GHIssue473 extends EndpointTester {

    @Test
    public void testFoo() throws Exception {
        url("/gh/473?suffix=abc").get();
        bodyEq("BARabcBARabc");
    }

    @Test
    public void testFoo2() throws Exception {
        url("/gh/473/?level=123").get();
        bodyEq("FOO123FOO123");
    }

    @Test
    public void testString() throws Exception {
        url("/gh/473/str?suffix=abc").get();
        bodyEq("abcabc");
    }

    @Test
    public void testString2() throws Exception {
        url("/gh/473/str?level=123").get();
        bodyEq("123123");
    }

}
