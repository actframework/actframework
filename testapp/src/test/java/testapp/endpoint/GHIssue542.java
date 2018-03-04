package testapp.endpoint;

import org.junit.Test;

public class GHIssue542 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/542").get();
        bodyEq("true");
    }

}
