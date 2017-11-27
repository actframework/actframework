package testapp.endpoint;

import org.junit.Test;

public class GHIssue421 extends EndpointTester {

    @Test
    public void testCn() throws Exception {
        url("/gh/421/greeting/cn").get("who","World");
        bodyEq("World你好");
    }

    @Test
    public void testEn() throws Exception {
        url("/gh/421/greeting/en").get("who","World");
        bodyEq("Hi World");
    }

    @Test
    public void testFooBar() throws Exception {
        url("/gh/421/foo/bar").get();
        bodyEq("567");
    }

    @Test
    public void testCliPort() throws Exception {
        url("/gh/421/cliPort").get();
        bodyEq("6222");
    }

}
