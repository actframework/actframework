package testapp.endpoint;

import org.junit.Test;

public class GHIssue636 extends EndpointTester {

    @Test
    public void test1() throws Exception {
        url("/gh/636").get();
        bodyEq("bar");
    }


}
