package testapp.endpoint;

import org.junit.Test;
import org.osgl.http.H;
import org.osgl.util.C;
import testapp.endpoint.ghissues.GH317;

import java.util.Map;

public class GHIssue317 extends EndpointTester {

    @Test
    public void test() throws Exception {
        String prodId = "Car 007";
        GH317.Product product = new GH317.Product(prodId);
        int quantity = 3;
        Map<String, Object> order = C.Map("prod", prodId, "quantity", 3);
        url("/gh/317").postJSON(order).accept(H.Format.JSON);
        String s = resp().body().string();
        System.out.println(s);
        checkRespCode();
    }

}
