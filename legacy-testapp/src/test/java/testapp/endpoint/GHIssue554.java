package testapp.endpoint;

import org.junit.Test;

public class GHIssue554 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/554?id=abc");
        bodyEq("abc");
    }

}

