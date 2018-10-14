package testapp.endpoint;

import org.junit.Test;
import org.osgl.http.H;

public class GHIssue349 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/349").accept(H.Format.JSON).post();
        bodyEq("{\"message\": \"Created\"}");
    }

}
