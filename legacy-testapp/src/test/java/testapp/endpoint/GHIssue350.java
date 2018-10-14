package testapp.endpoint;

import org.junit.Test;
import org.osgl.mvc.result.BadRequest;

/**
 * Test bed: {@link testapp.endpoint.ghissues.GH350}
 */
public class GHIssue350 extends EndpointTester {

    @Test
    public void testExceptionCatched() throws Exception {
        url("/gh/350/_xml").get();
        bodyEq("xml");
    }

    @Test
    public void testExceptionCatched2() throws Exception {
        url("/gh/350/_json").get();
        bodyEq("json");
    }

    @Test(expected = BadRequest.class)
    public void testExceptionNotCatched() throws Exception {
        url("/gh/350/_ex").get();
        checkRespCode();
    }

}
