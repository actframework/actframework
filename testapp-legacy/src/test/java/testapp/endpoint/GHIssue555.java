package testapp.endpoint;

import org.junit.Test;

public class GHIssue555 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/555");
        bodyEq("alert('Hello World!')");
    }

}

