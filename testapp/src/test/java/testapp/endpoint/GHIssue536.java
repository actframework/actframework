package testapp.endpoint;

import org.junit.Test;

public class GHIssue536 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/536").get();
        bodyEq("GH536");
    }

    @Test
    public void testConstFinal() throws Exception {
        url("/gh/536/const_final").get();
        bodyEq("GH536");
    }

    @Test
    public void testConst() throws Exception {
        url("/gh/536/const").get();
        bodyEq("GH536");
    }

    @Test
    public void testValFinal() throws Exception {
        url("/gh/536/val_final").get();
        bodyEq("GH536");
    }

    @Test
    public void testVal() throws Exception {
        url("/gh/536/val").get();
        bodyEq("GH536");
    }


}
