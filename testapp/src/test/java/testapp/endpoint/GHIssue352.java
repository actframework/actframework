package testapp.endpoint;

import org.junit.Test;

public class GHIssue352 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/352").get();
        bodyEq("<p>Hello Act</p>");
    }

    @Test
    public void testInline() throws Exception {
        url("/gh/352/inline").get();
        bodyEq("Hello Act");
    }

    @Test
    public void testRelative() throws Exception {
        url("/gh/352/relative").get();
        bodyEq("<p>Hello Act</p>");
    }

}
