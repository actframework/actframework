package testapp.endpoint;

import org.junit.Test;

public class GHIssue692 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/692").get();
        bodyEq("1");
    }

}
