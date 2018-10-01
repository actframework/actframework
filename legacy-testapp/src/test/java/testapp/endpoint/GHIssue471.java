package testapp.endpoint;

import org.junit.Test;

public class GHIssue471 extends EndpointTester {

    @Test
    public void testFoo() throws Exception {
        url("/gh/471/foo/abc").get();
        bodyEq("abc");
    }

    @Test
    public void testFoo2() throws Exception {
        url("/gh/471/foo/abc/123").get();
        bodyEq("abc123");
    }

}
