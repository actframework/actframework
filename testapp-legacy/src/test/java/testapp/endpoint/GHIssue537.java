package testapp.endpoint;

import org.junit.Test;
import org.osgl.http.H;

public class GHIssue537 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/537").accept(H.Format.JSON).get();
        eq(404, resp().code());
        eq("application/json", resp().header("Content-Type"));
    }

    @Test
    public void test2() throws Exception {
        url("/gh/537/333").accept(H.Format.JSON).get();
        bodyEq("{\"result\":\"333\"}");
        eq("application/json", resp().header("Content-Type"));
    }
}
