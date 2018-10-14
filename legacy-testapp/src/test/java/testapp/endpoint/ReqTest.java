package testapp.endpoint;

import org.junit.Test;
import org.osgl.mvc.result.Unauthorized;

import java.io.IOException;

public class ReqTest extends EndpointTester {

    @Test
    public void reqPathShallMatch() throws IOException {
        url("/req/path").get();
        bodyEq("/req/path");
    }

    @Test
    public void reqPathShallNotContainsQuery() throws IOException {
        url("/req/path?a=1&b=2").get();
        bodyEq("/req/path");
    }

    @Test
    public void reqQueryShallMatch() throws IOException {
        url("/req/query?a=1&b=2").get();
        bodyEq("a=1&b=2");
    }

    @Test
    public void reqQueryShallNotTriggerErrorIfNoQueryFound() throws IOException {
        url("/req/query").get();
        bodyEq("");
    }

}
