package testapp.endpoint;

import org.joda.time.DateTime;
import org.junit.Test;
import org.osgl.util.S;

public class GHIssue657 extends EndpointTester {

    private String year;

    public GHIssue657() {
        year = S.string(DateTime.now().getYear());
    }

    @Test
    public void testList1() throws Exception {
        url("/gh/657/1").getJSON();
        bodyContains(year);
    }

    @Test
    public void testList2() throws Exception {
        url("/gh/657/2").get();
        bodyContains(year);
    }

}
