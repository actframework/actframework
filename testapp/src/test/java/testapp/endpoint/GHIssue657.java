package testapp.endpoint;

import org.junit.Test;

public class GHIssue657 extends EndpointTester {

    @Test
    public void testList1() throws Exception {
        url("/gh/657/1").get();
        String s = resp().body().string();
        System.out.println(s);
        bodyContains("2018");
    }

    @Test
    public void testList2() throws Exception {
        url("/gh/657/2").get();
        bodyContains("2018");
    }

}
