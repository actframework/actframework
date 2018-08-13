package testapp.endpoint;

import org.junit.Test;

public class GHIssue232 extends EndpointTester {

    @Test
    public void testFoo() throws Exception {
        url("/gh/232/foo").get();
        responseCodeIs(202);
    }

    @Test
    public void testSimple() throws Exception {
        url("/gh/232/bar").get();
        responseCodeIs(202);
    }

    @Test
    public void testSimpleWithJson() throws Exception {
        url("/gh/232/bar").getJSON();
        responseCodeIs(202);
    }

    @Test
    public void testStructured() throws Exception {
        url("/gh/232/map/bar").get();
        responseCodeIs(202);
    }

    @Test
    public void testStructuredWithJson() throws Exception {
        url("/gh/232/map/bar").getJSON();
        responseCodeIs(202);
    }

}
